package com.uade.skillswap.bootstrap;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.FranjaDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.presentation.Resultado;
import com.uade.skillswap.presentation.ui.Formato;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Recorrido de punta a punta, solo a traves de los controllers (como lo haria la UI):
 * publicar, solicitar, aceptar, pagar, iniciar, completar y calificar; mas una cancelacion con reintegro
 * parcial y un pago con saldo insuficiente, que se muestra como error controlado.
 * Cada paso imprime el actor, el resultado, el estado y los saldos.
 */
public final class DemoFlujoPrincipal {

    private record Actor(String userId, String nombre) { }

    /** Un paso que debia salir bien fallo (o uno que debia fallar no fallo): la demo no puede seguir. */
    private static final class DemoInterrumpida extends RuntimeException {
        DemoInterrumpida(String mensaje) {
            super(mensaje);
        }
    }

    private static final PaginaRequest PAGINA = PaginaRequest.de(0, 20);

    private final Composicion app;
    private final IdentidadMockAdapter identidad;
    private final DatosDemo.Semilla semilla;
    private final PrintStream out;
    private final LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
    private final Actor ana = new Actor(DatosDemo.ANA, "Ana");
    private final Actor bruno = new Actor(DatosDemo.BRUNO, "Bruno");
    private final Actor cecilia = new Actor(DatosDemo.CECILIA, "Cecilia");
    private Actor actorActual;
    private int numeroPaso;
    // datos del flujo principal que reutiliza la cancelacion
    private Long publicacionPrincipal;
    private Long franjaCancelacion;

    public DemoFlujoPrincipal(Composicion app, IdentidadMockAdapter identidad, DatosDemo.Semilla semilla,
                              PrintStream out) {
        this.app = app;
        this.identidad = identidad;
        this.semilla = semilla;
        this.out = out;
    }

    /** @return true si el recorrido completo salio como se esperaba. */
    public boolean ejecutar() {
        try {
            descubrimiento();
            Long contratacionPrincipal = flujoPrincipal();
            cancelacionConReintegroParcial();
            pagoConSaldoInsuficiente();
            resumenFinal(contratacionPrincipal);
            out.println("\nDemo finalizada correctamente.");
            return true;
        } catch (DemoInterrumpida e) {
            out.println("\nDemo interrumpida: " + e.getMessage());
            return false;
        } finally {
            identidad.cerrarSesion();
        }
    }

    // ---------- bloques de la demo ----------

    private void descubrimiento() {
        seccion("Descubrimiento: búsqueda y recomendaciones de Bruno");
        paso(bruno, "busca publicaciones con el texto 'java', ordenadas por reputación", false,
                () -> app.getBusquedaController().buscar(new FiltroBusqueda("java", null, null, null, null, null,
                        null, null, null, null, CriterioOrden.REPUTACION), PAGINA),
                pagina -> listar(pagina.contenido(), Formato::publicacion, pagina.totalElementos() + " resultado(s)"));
        for (TipoEstrategiaRecomendacion estrategia : TipoEstrategiaRecomendacion.values()) {
            paso(bruno, "pide recomendaciones por " + estrategia, false,
                    () -> app.getRecomendacionController().recomendar(estrategia, PaginaRequest.de(0, 3)),
                    pagina -> listar(pagina.contenido(), Formato::publicacion,
                            pagina.totalElementos() + " recomendación(es), se muestran hasta 3"));
        }
    }

    private Long flujoPrincipal() {
        seccion("Flujo principal: publicar -> solicitar -> aceptar -> pagar -> iniciar -> completar -> calificar");
        var publicaciones = app.getPublicacionController();
        var contrataciones = app.getContratacionController();

        // Franjas: una para la cancelacion (en ~11 h) y otras dos para el flujo principal (en 3 y 4 dias)
        var comando = new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Mentoría exprés de patrones de diseño",
                "Una hora para aplicar Strategy, Adapter y Observer sobre tu propio proyecto.",
                "Programación", Modalidad.REMOTA, 60, 10, BigDecimal.valueOf(40),
                set("patrones", "gof"), set("java", "patrones de diseño"), TipoPoliticaCancelacion.FLEXIBLE,
                List.of(franja(ahora.plusHours(11)), franja(ahora.plusDays(3).plusHours(1)),
                        franja(ahora.plusDays(4).plusHours(1))));
        PublicacionDto borrador = paso(ana, "crea el borrador de su mentoría con 3 franjas",
                () -> publicaciones.crearBorrador(comando),
                p -> "Publicación #%d '%s' | %s | %d franjas".formatted(p.id(), p.titulo(), p.estado(),
                        p.franjas().size()));
        paso(ana, "publica la mentoría", () -> publicaciones.publicar(borrador.id()),
                p -> "Publicación #%d | %s".formatted(p.id(), p.estado()));
        PublicacionDto publicada = paso(ana, "agrega una promoción del 25 % por tiempo limitado",
                () -> publicaciones.agregarPromocion(new CrearPromocionCommand(borrador.id(), "Promo 25 %",
                        TipoPromocion.PORCENTAJE, BigDecimal.valueOf(25), ahora.minusDays(1), ahora.plusDays(30))),
                p -> "Precio base %s -> precio final %s | %s".formatted(Formato.monto(p.precioBase()),
                        Formato.monto(p.precioFinal()), p.detallePrecio()));
        List<FranjaDto> franjas = franjasOrdenadas(publicada);
        franjaCancelacion = franjas.get(0).id();
        Long franjaFlujo = franjas.get(1).id();
        publicacionPrincipal = borrador.id();

        paso(bruno, "busca 'patrones' y encuentra la mentoría nueva", false,
                () -> app.getBusquedaController().buscar(new FiltroBusqueda("patrones", null, null, null, null, null,
                        null, null, null, null, CriterioOrden.RECIENTES), PAGINA),
                pagina -> listar(pagina.contenido(), Formato::publicacion, pagina.totalElementos() + " resultado(s)"));

        ContratacionDto solicitada = paso(bruno, "solicita la mentoría en la franja de dentro de 3 días",
                () -> contrataciones.solicitar(new SolicitarContratacionCommand(borrador.id(), franjaFlujo,
                        "Hola Ana, quiero revisar la arquitectura de mi TP.", "Entender cómo separar las capas")),
                this::detalleContratacion);
        Long id = solicitada.id();
        paso(ana, "(proveedora) acepta la solicitud", () -> contrataciones.aceptar(id), this::detalleContratacion);
        paso(bruno, "(cliente) paga: el monto queda retenido en la contratación", () -> contrataciones.pagar(id),
                this::detalleContratacion);
        paso(ana, "inicia la sesión", () -> contrataciones.iniciar(id), this::detalleContratacion);
        paso(ana, "completa la sesión: se acredita lo retenido a Ana", () -> contrataciones.completar(id),
                this::detalleContratacion);
        paso(bruno, "consulta el comprobante", false, () -> contrataciones.comprobante(id),
                c -> "%s | monto %s | pagado %s | completado %s".formatted(c.numero(), Formato.monto(c.monto()),
                        Formato.fecha(c.pagadoEn()), Formato.fecha(c.completadoEn())));
        paso(bruno, "califica a Ana con 5 estrellas", false,
                () -> app.getCalificacionController().calificar(new CalificarCommand(id, 5, "Excelente mentoría, muy clara.")),
                c -> "Puntaje %d: '%s'".formatted(c.puntaje(), c.comentario()));
        paso(ana, "califica a Bruno con 4 estrellas", false,
                () -> app.getCalificacionController().calificar(new CalificarCommand(id, 4, "Llegó con las dudas bien preparadas.")),
                c -> "Puntaje %d: '%s'".formatted(c.puntaje(), c.comentario()));
        paso(bruno, "mira el perfil público de Ana (reputación recalculada)", false,
                () -> app.getPerfilController().verPerfilPublico(semilla.anaId()),
                p -> "%s | reputación %.1f sobre %d calificación(es)".formatted(p.nombreVisible(), p.reputacion(),
                        p.cantidadCalificaciones()));

        return id;
    }

    private void cancelacionConReintegroParcial() {
        seccion("Cancelación con reintegro parcial (política FLEXIBLE: menos de 24 h y más de 2 h -> 50 %)");
        var contrataciones = app.getContratacionController();
        ContratacionDto nueva = paso(bruno, "solicita otra sesión, en la franja de dentro de ~11 horas",
                () -> contrataciones.solicitar(new SolicitarContratacionCommand(publicacionPrincipal, franjaCancelacion,
                        "Necesito una segunda sesión urgente.", "Repasar el patrón Observer")),
                this::detalleContratacion);
        Long id = nueva.id();
        paso(ana, "(proveedora) acepta", () -> contrataciones.aceptar(id), this::detalleContratacion);
        paso(bruno, "(cliente) paga", () -> contrataciones.pagar(id), this::detalleContratacion);
        paso(bruno, "cancela: surgió un imprevisto", () -> contrataciones.cancelar(id, "Surgió un imprevisto laboral"),
                r -> "%s | política %s | reintegro al cliente %s | compensación a la proveedora %s".formatted(
                        Formato.contratacion(r.contratacion()), r.politica(), Formato.monto(r.reintegroCliente()),
                        Formato.monto(r.compensacionProveedor())));
    }

    private void pagoConSaldoInsuficiente() {
        seccion("Pago con saldo insuficiente (error controlado, sin excepciones hacia la UI)");
        var contrataciones = app.getContratacionController();
        PublicacionDto auditoria = paso(bruno, "mira la 'Auditoría de UX completa' de Cecilia", false,
                () -> app.getPublicacionController().obtener(semilla.auditoriaCeciliaId()),
                p -> "#%d '%s' | precio %s | política %s".formatted(p.id(), p.titulo(), Formato.monto(p.precioFinal()),
                        p.politicaCancelacion()));
        Long franjaLibre = franjasOrdenadas(auditoria).stream()
                .filter(f -> !f.ocupada() && f.inicio().isAfter(LocalDateTime.now())).findFirst()
                .orElseThrow(() -> new DemoInterrumpida("La auditoría de Cecilia no tiene franjas libres futuras"))
                .id();
        ContratacionDto solicitada = paso(bruno, "solicita la auditoría",
                () -> contrataciones.solicitar(new SolicitarContratacionCommand(auditoria.id(), franjaLibre,
                        "Quiero auditar la UX de mi app.", "Detectar problemas de usabilidad")),
                this::detalleContratacion);
        Long id = solicitada.id();
        paso(cecilia, "(proveedora) acepta", () -> contrataciones.aceptar(id), this::detalleContratacion);
        pasoConErrorEsperado(bruno, "intenta pagar la auditoría sin saldo suficiente",
                () -> contrataciones.pagar(id), ErrorCode.SALDO_INSUFICIENTE);
        paso(bruno, "consulta la contratación: sigue ACEPTADA y sin cobrar nada", false,
                () -> contrataciones.obtener(id), this::detalleContratacion);
        paso(bruno, "la cancela (no había pago: no hay reintegro)", () -> contrataciones.cancelar(id, "Sin saldo suficiente"),
                r -> "%s | reintegro %s | compensación %s".formatted(Formato.contratacion(r.contratacion()),
                        Formato.monto(r.reintegroCliente()), Formato.monto(r.compensacionProveedor())));
    }

    private void resumenFinal(Long contratacionPrincipal) {
        seccion("Estado final");
        mostrarSaldos();
        paso(bruno, "revisa los movimientos de su wallet", false,
                () -> app.getWalletController().movimientos(PAGINA),
                m -> listar(m.contenido(), Formato::movimiento, m.totalElementos() + " movimiento(s)"));
        paso(ana, "revisa los movimientos de su wallet", false,
                () -> app.getWalletController().movimientos(PAGINA),
                m -> listar(m.contenido(), Formato::movimiento, m.totalElementos() + " movimiento(s)"));
        paso(ana, "consulta su reputación", false, () -> app.getPerfilController().miPerfil(),
                p -> "reputación %.1f (%d calificación(es))".formatted(p.reputacion(), p.cantidadCalificaciones()));
        paso(bruno, "consulta su reputación", false, () -> app.getPerfilController().miPerfil(),
                p -> "reputación %.1f (%d calificación(es))".formatted(p.reputacion(), p.cantidadCalificaciones()));
        paso(bruno, "revisa el historial de estados de la contratación #" + contratacionPrincipal, false,
                () -> app.getContratacionController().historial(contratacionPrincipal, PAGINA),
                h -> listar(h.contenido(), Formato::historial, h.totalElementos() + " transición(es)"));
        for (Actor actor : List.of(ana, bruno)) {
            paso(actor, "revisa su bandeja de notificaciones", false,
                    () -> app.getNotificacionController().listar(false, PaginaRequest.de(0, 50)),
                    n -> resumenNotificaciones(n.contenido(), n.totalElementos()));
        }
        out.println("\n  Canales externos simulados: %d correo(s) enviados por Email, %d push enviados."
                .formatted(app.getEmailSdk().enviados().size(), app.getPushSdk().enviados().size()));
    }

    // ---------- infraestructura de impresion ----------

    private void seccion(String titulo) {
        out.println("\n==== " + titulo + " ====");
    }

    private <T> T paso(Actor actor, String descripcion, Supplier<Resultado<T>> operacion, Function<T, String> detalle) {
        return paso(actor, descripcion, true, operacion, detalle);
    }

    private <T> T paso(Actor actor, String descripcion, boolean conSaldos, Supplier<Resultado<T>> operacion,
                       Function<T, String> detalle) {
        encabezado(actor, descripcion);
        Resultado<T> r = operacion.get();
        if (r.esError()) {
            out.println("    -> ERROR [%s] %s".formatted(r.codigo(), r.mensaje()));
            throw new DemoInterrumpida("falló un paso que debía salir bien: " + actor.nombre() + " " + descripcion);
        }
        out.println("    -> OK    " + detalle.apply(r.valor()));
        if (conSaldos) {
            mostrarSaldos();
        }
        return r.valor();
    }

    /** La operacion DEBE fallar con el codigo indicado; se muestra el mensaje que vería el usuario. */
    private <T> void pasoConErrorEsperado(Actor actor, String descripcion, Supplier<Resultado<T>> operacion,
                                          ErrorCode esperado) {
        encabezado(actor, descripcion);
        Resultado<T> r = operacion.get();
        if (r.esOk() || r.codigo() != esperado) {
            throw new DemoInterrumpida("se esperaba el error " + esperado + " pero se obtuvo " + r);
        }
        out.println("    -> ERROR CONTROLADO [%s] %s".formatted(r.codigo(), r.mensaje()));
        out.println("       (la operación no se aplicó: no cambió el estado ni se movió ningún saldo)");
        mostrarSaldos();
    }

    private void encabezado(Actor actor, String descripcion) {
        numeroPaso++;
        actuarComo(actor);
        out.println("\n[%02d] %s %s".formatted(numeroPaso, actor.nombre(), descripcion));
    }

    private void actuarComo(Actor actor) {
        identidad.iniciarSesion(actor.userId());
        actorActual = actor;
    }

    /** Lee cada wallet con la sesion de su duenio y despues vuelve a la sesion del paso en curso. */
    private void mostrarSaldos() {
        Actor volverA = actorActual;
        StringBuilder linea = new StringBuilder("    saldos:");
        String separador = " ";
        for (Actor actor : List.of(ana, bruno, cecilia)) {
            actuarComo(actor);
            Resultado<WalletDto> w = app.getWalletController().consultar();
            linea.append(separador).append(actor.nombre()).append(' ')
                    .append(w.esOk() ? Formato.monto(w.valor().saldo()) : "(sin dato: " + w.mensaje() + ")");
            separador = " | ";
        }
        out.println(linea);
        if (volverA != null) {
            actuarComo(volverA);
        }
    }

    private String detalleContratacion(ContratacionDto c) {
        return "Contratación " + Formato.contratacion(c);
    }

    private <T> String listar(List<T> items, Function<T, String> formato, String titulo) {
        if (items.isEmpty()) {
            return titulo + " (lista vacía)";
        }
        StringBuilder sb = new StringBuilder(titulo);
        items.forEach(i -> sb.append("\n         - ").append(formato.apply(i)));
        return sb.toString();
    }

    private String resumenNotificaciones(List<NotificacionDto> notificaciones, long total) {
        Map<String, Integer> porCanal = new TreeMap<>();
        notificaciones.forEach(n -> porCanal.merge(n.canal().name(), 1, Integer::sum));
        String ultimas = listar(notificaciones.stream().limit(3).toList(), Formato::notificacion, "últimas 3");
        return "%d notificación(es) por canal %s | %s".formatted(total, porCanal, ultimas);
    }

    // ---------- helpers de datos ----------

    private static List<FranjaDto> franjasOrdenadas(PublicacionDto publicacion) {
        return publicacion.franjas().stream().sorted(Comparator.comparing(FranjaDto::inicio)).toList();
    }

    private static FranjaCommand franja(LocalDateTime inicio) {
        return new FranjaCommand(inicio, inicio.plusHours(1));
    }

    private static Set<String> set(String... valores) {
        return new LinkedHashSet<>(List.of(valores));
    }
}
