import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.FranjaDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.presentation.Resultado;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Evidencia del area 3 de la revision, sobre el sistema real (Composicion + H2 + JPA) y solo por controllers:
 *   Parte 1 - carreras con dos hilos: misma franja, doble pago, doble cancelacion, doble completar, doble
 *             calificacion, dos pagos con saldo para uno, pagar contra cancelar, dos cobros del mismo proveedor y
 *             doble registro. En cada ronda: cuantas tuvieron exito y que el dinero se conserva.
 *   Parte 2 - permisos: un tercero, la parte equivocada, sin sesion, sin perfil y con el perfil dado de baja.
 *   Parte 3 - entradas hostiles: vacias, enormes, negativas, ids inexistentes. Nunca debe salir ERROR_INTERNO
 *             ni escaparse una excepcion del controller.
 * No es parte del repo. Se compila contra las clases del proyecto y se ejecuta con su classpath.
 */
public class ConcurrenciaYAutorizacion {

    static final String ANA = "u-ana", BRUNO = "u-bruno", CECILIA = "u-cecilia", TECH = "u-techcorp";
    static final PaginaRequest PAG = PaginaRequest.de(0, 20);
    static final String LARGO = "x".repeat(10_000);

    static JpaContexto ctx;
    static IdentidadMockAdapter identidad;
    static Composicion app;
    static final Map<String, Long> perfilId = new LinkedHashMap<>();
    static final Map<Long, String> clienteDe = new LinkedHashMap<>();
    static int franjasCreadas = 0;

    static final List<String> problemas = new ArrayList<>();
    static final Map<String, Integer> conteo = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("escala")) {
            for (int n = 2; n <= 12; n += 2) {
                consultasPorListado(n, true);
            }
            return;
        }
        carreras();
        carrerasDeReprogramacion();
        dobleRegistro();
        permisos();
        entradasHostiles();
        notificacionConMotivoLargo();
        textosQueSoloDifierenEnMayusculas();
        consultasPorListado(10);
        consultasPorListado(40);
        consultasPorListado(10, true);
        consultasPorListado(40, true);

        System.out.println();
        conteo.forEach((k, v) -> System.out.println("[AREA3]   " + k + " = " + v));
        if (problemas.isEmpty()) {
            System.out.println("[AREA3] RESULTADO: sin problemas");
        } else {
            System.out.println("[AREA3] RESULTADO: " + problemas.size() + " PROBLEMAS");
            problemas.forEach(p -> System.out.println("[AREA3]   - " + p));
        }
    }

    // =====================================================================================================
    // Parte 1: carreras
    // =====================================================================================================

    static void carreras() throws Exception {
        iniciar(ANA, BRUNO, CECILIA);
        int rondas = 25;

        carrera("C1 misma franja, dos clientes, dos aceptaciones", rondas, 1, 1, () -> {
            PublicacionDto p = publicar(ANA, "1.00", 3, 2);
            Long deBruno = solicitar(BRUNO, p, 0);
            Long deCecilia = solicitar(CECILIA, p, 0);
            como(ANA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().aceptar(deBruno),
                    () -> app.getContratacionController().aceptar(deCecilia));
            PublicacionDto tras = app.getPublicacionController().obtener(p.id()).valor();
            if (tras.cupoDisponible() != 2 || !tras.franjas().get(0).ocupada()) {
                problema("C1: cupo disponible " + tras.cupoDisponible() + " y franja ocupada="
                        + tras.franjas().get(0).ocupada());
            }
            return r;
        });

        carrera("C2 doble pago de la misma contratacion", rondas, 1, 1, () -> {
            Long c = aceptada(ANA, BRUNO, "0.50");
            BigDecimal antes = saldo(BRUNO);
            como(BRUNO);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().pagar(c),
                    () -> app.getContratacionController().pagar(c));
            exigirSaldo("C2", BRUNO, antes.subtract(new BigDecimal("0.50")));
            return r;
        });

        carrera("C3 doble cancelacion de una CONFIRMADA", rondas, 1, 1, () -> {
            Long c = pagada(ANA, BRUNO, "0.50");
            BigDecimal antes = saldo(BRUNO);
            como(BRUNO);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().cancelar(c, "Me arrepenti"),
                    () -> app.getContratacionController().cancelar(c, "Me arrepenti"));
            exigirSaldo("C3", BRUNO, antes.add(new BigDecimal("0.50"))); // franja a 5 dias: reintegro total, una vez
            return r;
        });

        carrera("C4 doble completar", rondas, 1, 1, () -> {
            Long c = pagada(ANA, BRUNO, "0.50");
            como(ANA);
            app.getContratacionController().iniciar(c);
            BigDecimal antes = saldo(ANA);
            como(ANA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().completar(c),
                    () -> app.getContratacionController().completar(c));
            exigirSaldo("C4", ANA, antes.add(new BigDecimal("0.50")));
            return r;
        });

        carrera("C5 doble calificacion del mismo autor", rondas, 1, 1, () -> {
            Long c = pagada(ANA, BRUNO, "0.00");
            como(ANA);
            app.getContratacionController().iniciar(c);
            app.getContratacionController().completar(c);
            long antes = calificacionesDe(ANA);
            como(BRUNO);
            List<Resultado<?>> r = enParalelo(
                    () -> app.getCalificacionController().calificar(new CalificarCommand(c, 5, "Excelente")),
                    () -> app.getCalificacionController().calificar(new CalificarCommand(c, 1, "Malo")));
            if (calificacionesDe(ANA) != antes + 1) {
                problema("C5: las calificaciones de Ana pasaron de " + antes + " a " + calificacionesDe(ANA));
            }
            return r;
        });

        carrera("C6 dos pagos del mismo cliente con saldo para uno solo", rondas, 1, 1, () -> {
            BigDecimal saldoCecilia = saldo(CECILIA);
            String precio = saldoCecilia.multiply(new BigDecimal("0.60")).setScale(2, java.math.RoundingMode.DOWN)
                    .toPlainString();
            Long c1 = aceptada(ANA, CECILIA, precio);
            Long c2 = aceptada(BRUNO, CECILIA, precio);
            como(CECILIA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().pagar(c1),
                    () -> app.getContratacionController().pagar(c2));
            if (saldo(CECILIA).signum() < 0) {
                problema("C6: saldo negativo " + saldo(CECILIA));
            }
            // el proveedor cancela la que se pago: Cecilia recupera el 100 % y puede jugar otra ronda
            como(ANA);
            app.getContratacionController().cancelar(c1, "Se devuelve");
            como(BRUNO);
            app.getContratacionController().cancelar(c2, "Se devuelve");
            exigirSaldo("C6", CECILIA, saldoCecilia);
            return r;
        });

        carrera("C7 pagar contra cancelar la misma ACEPTADA", rondas, 1, 2, () -> {
            Long c = aceptada(ANA, BRUNO, "0.50");
            como(BRUNO);
            return enParalelo(() -> app.getContratacionController().pagar(c),
                    () -> app.getContratacionController().cancelar(c, "Me arrepenti"));
        });

        carrera("C8 dos cobros del mismo proveedor (contrataciones distintas)", rondas, 1, 2, () -> {
            Long c1 = pagada(ANA, BRUNO, "0.50");
            Long c2 = pagada(ANA, CECILIA, "0.50");
            como(ANA);
            app.getContratacionController().iniciar(c1);
            app.getContratacionController().iniciar(c2);
            BigDecimal antes = saldo(ANA);
            como(ANA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().completar(c1),
                    () -> app.getContratacionController().completar(c2));
            long exitos = r.stream().filter(Resultado::esOk).count();
            exigirSaldo("C8", ANA, antes.add(new BigDecimal("0.50").multiply(BigDecimal.valueOf(exitos))));
            return r;
        });

        cerrar();
    }

    /** Reprogramar no cambia el cupo: estas carreras solo las frena la version de la publicacion. */
    static void carrerasDeReprogramacion() throws Exception {
        iniciar(ANA, BRUNO, CECILIA);
        carrera("C10 aceptar una reprogramacion contra aceptar otra solicitud, misma franja destino", 25, 1, 1, () -> {
            PublicacionDto p = publicar(ANA, "0.00", 5, 2);
            Long destino = p.franjas().get(1).id();
            Long x = solicitar(BRUNO, p, 0);
            como(ANA);
            app.getContratacionController().aceptar(x);
            como(BRUNO);
            app.getContratacionController().proponerReprogramacion(x, destino).valor();
            Long y = solicitar(CECILIA, p, 1);
            como(ANA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().responderReprogramacion(x, true),
                    () -> app.getContratacionController().aceptar(y));
            reservasSobre("C10", p.id(), destino, x, y);
            return r;
        });
        carrera("C11 dos reprogramaciones aceptadas a la vez hacia la misma franja", 25, 1, 1, () -> {
            PublicacionDto p = publicar(ANA, "0.00", 5, 3);
            Long destino = p.franjas().get(2).id();
            Long x = solicitar(BRUNO, p, 0);
            Long z = solicitar(CECILIA, p, 1);
            como(ANA);
            app.getContratacionController().aceptar(x);
            app.getContratacionController().aceptar(z);
            como(BRUNO);
            app.getContratacionController().proponerReprogramacion(x, destino).valor();
            como(CECILIA);
            app.getContratacionController().proponerReprogramacion(z, destino).valor();
            como(ANA);
            List<Resultado<?>> r = enParalelo(() -> app.getContratacionController().responderReprogramacion(x, true),
                    () -> app.getContratacionController().responderReprogramacion(z, true));
            reservasSobre("C11", p.id(), destino, x, z);
            return r;
        });
        cerrar();
    }

    /** La franja destino debe quedar ocupada y reservada por exactamente una de las dos contrataciones. */
    static void reservasSobre(String donde, Long pubId, Long franjaId, Long c1, Long c2) {
        int reservas = 0;
        for (Long id : new Long[] {c1, c2}) {
            como(clienteDe.get(id));
            ContratacionDto c = app.getContratacionController().obtener(id).valor();
            if (c.franjaId().equals(franjaId) && !c.estado().equals("PENDIENTE")) {
                reservas++;
            }
        }
        como(ANA);
        boolean ocupada = false;
        for (FranjaDto f : app.getPublicacionController().obtener(pubId).valor().franjas()) {
            if (f.id().equals(franjaId)) {
                ocupada = f.ocupada();
            }
        }
        if (reservas != 1 || !ocupada) {
            problema(donde + ": la franja destino quedo con " + reservas + " reservas y ocupada=" + ocupada);
        }
    }

    /** Una ronda devuelve los dos resultados; se exige que la cantidad de exitos este entre minimo y maximo. */
    static void carrera(String nombre, int rondas, int minExitos, int maxExitos,
                        java.util.concurrent.Callable<List<Resultado<?>>> ronda) throws Exception {
        Map<String, Integer> codigos = new TreeMap<>();
        int malas = 0;
        for (int i = 0; i < rondas; i++) {
            List<Resultado<?>> resultados = ronda.call();
            long exitos = resultados.stream().filter(Resultado::esOk).count();
            if (exitos < minExitos || exitos > maxExitos) {
                malas++;
                problema(nombre + ", ronda " + i + ": " + exitos + " exitos -> " + resultados);
            }
            for (Resultado<?> r : resultados) {
                codigos.merge(r.esOk() ? "OK" : r.codigo().name(), 1, Integer::sum);
            }
        }
        dineroConservado(nombre);
        System.out.println("[AREA3] " + (malas == 0 ? "OK    " : "FALLA ") + nombre + ": " + rondas + " rondas -> "
                + codigos);
        if (codigos.containsKey("ERROR_INTERNO")) {
            problema(nombre + ": el perdedor de la carrera recibe ERROR_INTERNO " + codigos.get("ERROR_INTERNO")
                    + " veces");
        }
    }

    static void dobleRegistro() throws Exception {
        Map<String, Integer> codigos = new TreeMap<>();
        for (int i = 0; i < 8; i++) {
            iniciar();
            como(TECH);
            RegistrarPerfilCommand comando = new RegistrarPerfilCommand(TECH, "TechCorp", TipoPerfil.ORGANIZACION,
                    "UTC", null, Set.of(), Set.of(), List.of(), CanalNotificacion.INTERNA);
            List<Resultado<?>> r = enParalelo(() -> app.getPerfilController().registrar(comando),
                    () -> app.getPerfilController().registrar(comando));
            long exitos = r.stream().filter(Resultado::esOk).count();
            if (exitos != 1) {
                problema("C9 doble registro, ronda " + i + ": " + exitos + " exitos -> " + r);
            }
            for (Resultado<?> x : r) {
                codigos.merge(x.esOk() ? "OK" : x.codigo().name(), 1, Integer::sum);
            }
            cerrar();
        }
        System.out.println("[AREA3] C9 doble registro del mismo usuario: 8 rondas -> " + codigos);
        if (codigos.containsKey("ERROR_INTERNO")) {
            problema("C9 doble registro: el perdedor recibe ERROR_INTERNO " + codigos.get("ERROR_INTERNO") + " veces");
        }
    }

    static List<Resultado<?>> enParalelo(Supplier<Resultado<?>> a, Supplier<Resultado<?>> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resultado<?>>> futuros = new ArrayList<>();
            for (Supplier<Resultado<?>> tarea : List.of(a, b)) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await();
                    return tarea.get();
                }));
            }
            listos.await();
            largada.countDown();
            List<Resultado<?>> resultados = new ArrayList<>();
            for (Future<Resultado<?>> f : futuros) {
                resultados.add(f.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    static void dineroConservado(String donde) {
        BigDecimal total = BigDecimal.ZERO;
        for (String u : perfilId.keySet()) {
            BigDecimal s = saldo(u);
            if (s.signum() < 0) {
                problema(donde + ": saldo negativo de " + u);
            }
            total = total.add(s);
        }
        for (Map.Entry<Long, String> e : clienteDe.entrySet()) {
            como(e.getValue());
            ContratacionDto c = app.getContratacionController().obtener(e.getKey()).valor();
            if (c.estado().equals("CONFIRMADA") || c.estado().equals("EN_CURSO")) {
                total = total.add(c.precioFinal());
            }
        }
        if (total.compareTo(BigDecimal.valueOf(100L * perfilId.size())) != 0) {
            problema(donde + ": el dinero no se conserva, total " + total);
        }
    }

    // =====================================================================================================
    // Parte 2: permisos
    // =====================================================================================================

    static void permisos() {
        iniciar(ANA, BRUNO, CECILIA);
        PublicacionDto p = publicar(ANA, "10.00", 5, 4);
        Long borrador = borrador(ANA, "10.00", 2, 1).id();
        Long pendiente = solicitar(BRUNO, p, 0);
        Long aceptada = solicitar(BRUNO, p, 1);
        como(ANA);
        app.getContratacionController().aceptar(aceptada);
        Long confirmada = solicitar(BRUNO, p, 2);
        como(ANA);
        app.getContratacionController().aceptar(confirmada);
        como(BRUNO);
        app.getContratacionController().pagar(confirmada);
        Long completada = solicitar(BRUNO, p, 3);
        como(ANA);
        app.getContratacionController().aceptar(completada);
        como(BRUNO);
        app.getContratacionController().pagar(completada);
        como(ANA);
        app.getContratacionController().iniciar(completada);
        app.getContratacionController().completar(completada);
        Long notificacionDeAna = app.getNotificacionController().listar(false, PAG).valor().contenido().get(0).id();
        Long franjaLibre = app.getPublicacionController().agregarFranja(p.id(), enDias(9), enDias(9).plusHours(1))
                .valor().franjas().get(4).id();
        String fotoAntes = foto(p.id(), borrador, pendiente, aceptada, confirmada, completada);

        Set<ErrorCode> NO = Set.of(ErrorCode.NO_AUTORIZADO);
        // --- un tercero (Cecilia) sobre contrataciones ajenas ---
        como(CECILIA);
        permiso("tercero: obtener contratacion", NO, () -> app.getContratacionController().obtener(confirmada));
        permiso("tercero: historial", NO, () -> app.getContratacionController().historial(confirmada, PAG));
        permiso("tercero: comprobante", NO, () -> app.getContratacionController().comprobante(completada));
        permiso("tercero: aceptar", NO, () -> app.getContratacionController().aceptar(pendiente));
        permiso("tercero: rechazar", NO, () -> app.getContratacionController().rechazar(pendiente, "motivo"));
        permiso("tercero: pagar", NO, () -> app.getContratacionController().pagar(aceptada));
        permiso("tercero: iniciar", NO, () -> app.getContratacionController().iniciar(confirmada));
        permiso("tercero: completar", NO, () -> app.getContratacionController().completar(confirmada));
        permiso("tercero: cancelar", NO, () -> app.getContratacionController().cancelar(confirmada, "motivo"));
        permiso("tercero: proponer reprogramacion", NO,
                () -> app.getContratacionController().proponerReprogramacion(confirmada, franjaLibre));
        permiso("tercero: responder reprogramacion", NO,
                () -> app.getContratacionController().responderReprogramacion(confirmada, true));
        permiso("tercero: calificar", NO,
                () -> app.getCalificacionController().calificar(new CalificarCommand(completada, 1, "troll")));
        // --- un tercero sobre publicaciones ajenas ---
        permiso("no autor: editar borrador", NO, () -> app.getPublicacionController().editar(borrador, edicion()));
        permiso("no autor: agregar franja", NO,
                () -> app.getPublicacionController().agregarFranja(p.id(), enDias(8), enDias(8).plusHours(1)));
        permiso("no autor: publicar borrador", NO, () -> app.getPublicacionController().publicar(borrador));
        permiso("no autor: pausar", NO, () -> app.getPublicacionController().pausar(p.id()));
        permiso("no autor: reanudar", NO, () -> app.getPublicacionController().reanudar(p.id()));
        permiso("no autor: cerrar", NO, () -> app.getPublicacionController().cerrar(p.id()));
        permiso("no autor: cancelar", NO, () -> app.getPublicacionController().cancelar(p.id(), "motivo"));
        permiso("no autor: cambiarEstado", NO,
                () -> app.getPublicacionController().cambiarEstado(p.id(), AccionPublicacion.PAUSAR, null));
        permiso("no autor: agregar promocion", NO, () -> app.getPublicacionController().agregarPromocion(
                new CrearPromocionCommand(p.id(), "Regalo", TipoPromocion.PORCENTAJE, new BigDecimal("100"), null,
                        null)));
        permiso("no autor: ver borrador ajeno", Set.of(ErrorCode.NO_AUTORIZADO, ErrorCode.NO_ENCONTRADO),
                () -> app.getPublicacionController().obtener(borrador));
        permiso("otro usuario: marcar leida una notificacion ajena", Set.of(ErrorCode.NO_AUTORIZADO,
                ErrorCode.NO_ENCONTRADO), () -> app.getNotificacionController().marcarLeida(notificacionDeAna));
        permiso("registrar un perfil con el userId de otro", NO, () -> app.getPerfilController().registrar(
                new RegistrarPerfilCommand(ANA, "Impostor", TipoPerfil.PROFESIONAL, "UTC", null, Set.of(), Set.of(),
                        List.of(), CanalNotificacion.INTERNA)));
        // --- la parte equivocada ---
        como(BRUNO);
        permiso("cliente: aceptar su propia solicitud", NO, () -> app.getContratacionController().aceptar(pendiente));
        permiso("cliente: rechazar", NO, () -> app.getContratacionController().rechazar(pendiente, "motivo"));
        permiso("cliente: iniciar", NO, () -> app.getContratacionController().iniciar(confirmada));
        permiso("cliente: completar", NO, () -> app.getContratacionController().completar(confirmada));
        como(ANA);
        permiso("proveedor: pagar por el cliente", NO, () -> app.getContratacionController().pagar(aceptada));
        // --- sin sesion ---
        identidad.cerrarSesion();
        permiso("sin sesion: saldo", NO, () -> app.getWalletController().consultar());
        permiso("sin sesion: movimientos", NO, () -> app.getWalletController().movimientos(PAG));
        permiso("sin sesion: mis contrataciones", NO, () -> app.getContratacionController().misContrataciones(PAG));
        permiso("sin sesion: obtener contratacion", NO, () -> app.getContratacionController().obtener(confirmada));
        permiso("sin sesion: solicitar", NO, () -> app.getContratacionController().solicitar(
                new SolicitarContratacionCommand(p.id(), franjaLibre, "hola", "objetivo")));
        permiso("sin sesion: pagar", NO, () -> app.getContratacionController().pagar(aceptada));
        permiso("sin sesion: crear borrador", NO, () -> app.getPublicacionController().crearBorrador(comando("1")));
        permiso("sin sesion: pausar", NO, () -> app.getPublicacionController().pausar(p.id()));
        permiso("sin sesion: notificaciones", NO, () -> app.getNotificacionController().listar(false, PAG));
        permiso("sin sesion: mi perfil", NO, () -> app.getPerfilController().miPerfil());
        permiso("sin sesion: dar de baja", NO, () -> app.getPerfilController().darDeBaja());
        permiso("sin sesion: recomendar", NO,
                () -> app.getRecomendacionController().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, PAG));
        permiso("sin sesion: calificar", NO,
                () -> app.getCalificacionController().calificar(new CalificarCommand(completada, 5, "ok")));
        observar("sin sesion: buscar", app.getBusquedaController().buscar(FiltroBusqueda.vacio(), PAG));
        observar("sin sesion: ver perfil publico", app.getPerfilController().verPerfilPublico(perfilId.get(ANA)));
        observar("sin sesion: calificaciones recibidas de otro",
                app.getCalificacionController().recibidasDe(perfilId.get(ANA), PAG));
        observar("sin sesion: ver publicacion publicada", app.getPublicacionController().obtener(p.id()));
        // --- usuario autenticado pero sin perfil ---
        como(TECH);
        Set<ErrorCode> sinPerfil = Set.of(ErrorCode.NO_AUTORIZADO, ErrorCode.NO_ENCONTRADO);
        permiso("sin perfil: saldo", sinPerfil, () -> app.getWalletController().consultar());
        permiso("sin perfil: solicitar", sinPerfil, () -> app.getContratacionController().solicitar(
                new SolicitarContratacionCommand(p.id(), franjaLibre, "hola", "objetivo")));
        permiso("sin perfil: crear borrador", sinPerfil,
                () -> app.getPublicacionController().crearBorrador(comando("1")));

        if (!foto(p.id(), borrador, pendiente, aceptada, confirmada, completada).equals(fotoAntes)) {
            problema("permisos: algun intento sin permiso cambio el estado del sistema");
        }

        // --- perfil dado de baja (Cecilia): que puede seguir haciendo ---
        como(CECILIA);
        app.getPerfilController().darDeBaja();
        observar("perfil dado de baja: solicitar", app.getContratacionController().solicitar(
                new SolicitarContratacionCommand(p.id(), franjaLibre, "hola", "objetivo")));
        observar("perfil dado de baja: crear borrador", app.getPublicacionController().crearBorrador(comando("1")));
        observar("perfil dado de baja: actualizar perfil", app.getPerfilController().actualizar(
                new ActualizarPerfilCommand("Otra", null, "UTC", Set.of(), Set.of(), List.of(),
                        CanalNotificacion.INTERNA)));
        observar("perfil dado de baja: consultar saldo", app.getWalletController().consultar());
        observar("perfil dado de baja: dar de baja otra vez", app.getPerfilController().darDeBaja());
        como(BRUNO);
        observar("solicitarle a un perfil dado de baja (publicacion de Ana, Ana activa)",
                app.getContratacionController().solicitar(new SolicitarContratacionCommand(p.id(), franjaLibre,
                        "hola", "objetivo")));
        cerrar();
    }

    static void permiso(String nombre, Set<ErrorCode> esperados, Supplier<Resultado<?>> llamada) {
        Resultado<?> r;
        try {
            r = llamada.get();
        } catch (RuntimeException e) {
            problema("PERMISO " + nombre + ": el controller lanzo " + e);
            return;
        }
        if (r.esOk()) {
            problema("PERMISO " + nombre + ": se permitio");
        } else if (!esperados.contains(r.codigo())) {
            problema("PERMISO " + nombre + ": se esperaba " + esperados + " y fue " + r.codigo() + " ("
                    + r.mensaje() + ")");
        } else {
            contar("permiso denegado con " + r.codigo());
        }
    }

    static void observar(String nombre, Resultado<?> r) {
        System.out.println("[AREA3] observacion: " + nombre + " -> " + (r.esOk() ? "PERMITIDO" : r.codigo() + " ("
                + r.mensaje() + ")"));
    }

    // =====================================================================================================
    // Parte 3: entradas hostiles
    // =====================================================================================================

    static void entradasHostiles() {
        iniciar(ANA, BRUNO, CECILIA);
        PublicacionDto p = publicar(ANA, "10.00", 5, 3);
        Long pub = p.id();
        Long franja = p.franjas().get(0).id();
        Long borrador = borrador(ANA, "10.00", 2, 1).id();
        Long otraPub = publicar(CECILIA, "10.00", 2, 1).franjas().get(0).id(); // franja de otra publicacion
        Long pendiente = solicitar(BRUNO, p, 1);
        Long completada = solicitar(BRUNO, p, 2);
        como(ANA);
        app.getContratacionController().aceptar(completada);
        como(BRUNO);
        app.getContratacionController().pagar(completada);
        como(ANA);
        app.getContratacionController().iniciar(completada);
        app.getContratacionController().completar(completada);
        String fotoAntes = foto(pub, borrador, pendiente, completada);

        // --- ids ---
        for (Long id : Arrays.asList(null, 0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 999_999L)) {
            como(BRUNO);
            hostil("obtener contratacion id=" + id, () -> app.getContratacionController().obtener(id));
            hostil("pagar id=" + id, () -> app.getContratacionController().pagar(id));
            hostil("cancelar id=" + id, () -> app.getContratacionController().cancelar(id, "motivo"));
            hostil("historial id=" + id, () -> app.getContratacionController().historial(id, PAG));
            hostil("comprobante id=" + id, () -> app.getContratacionController().comprobante(id));
            hostil("proponer reprogramacion a franja id=" + id,
                    () -> app.getContratacionController().proponerReprogramacion(pendiente, id));
            hostil("calificar contratacion id=" + id,
                    () -> app.getCalificacionController().calificar(new CalificarCommand(id, 5, "ok")));
            hostil("marcar leida id=" + id, () -> app.getNotificacionController().marcarLeida(id));
            hostil("perfil publico id=" + id, () -> app.getPerfilController().verPerfilPublico(id));
            hostil("calificaciones de id=" + id, () -> app.getCalificacionController().recibidasDe(id, PAG));
            hostil("solicitar publicacion id=" + id, () -> app.getContratacionController().solicitar(
                    new SolicitarContratacionCommand(id, franja, "hola", "objetivo")));
            hostil("solicitar franja id=" + id, () -> app.getContratacionController().solicitar(
                    new SolicitarContratacionCommand(pub, id, "hola", "objetivo")));
            como(ANA);
            hostil("obtener publicacion id=" + id, () -> app.getPublicacionController().obtener(id));
            hostil("publicar id=" + id, () -> app.getPublicacionController().publicar(id));
            hostil("aceptar id=" + id, () -> app.getContratacionController().aceptar(id));
            hostil("promocion publicacion id=" + id, () -> app.getPublicacionController().agregarPromocion(
                    new CrearPromocionCommand(id, "Promo", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null)));
        }
        como(BRUNO);
        hostil("solicitar con una franja de OTRA publicacion", () -> app.getContratacionController().solicitar(
                new SolicitarContratacionCommand(pub, otraPub, "hola", "objetivo")));
        hostil("reprogramar a una franja de OTRA publicacion",
                () -> app.getContratacionController().proponerReprogramacion(pendiente, otraPub));

        // --- textos ---
        for (String texto : Arrays.asList(null, "", "   ", LARGO, "'; DROP TABLE perfil; --", "\u0000😀%_\\")) {
            String t = texto == null ? "null" : texto.length() > 30 ? texto.length() + " caracteres" : "\"" + texto + "\"";
            como(BRUNO);
            hostil("solicitar mensaje=" + t, () -> app.getContratacionController().solicitar(
                    new SolicitarContratacionCommand(pub, franja, texto, "objetivo")), "solicitar");
            hostil("solicitar objetivo=" + t, () -> app.getContratacionController().solicitar(
                    new SolicitarContratacionCommand(pub, franja, "hola", texto)), "solicitar");
            hostil("cancelar motivo=" + t, () -> app.getContratacionController().cancelar(pendiente, texto));
            hostil("calificar comentario=" + t, () -> app.getCalificacionController().calificar(
                    new CalificarCommand(pendiente, 5, texto)));
            hostil("buscar texto=" + t, () -> app.getBusquedaController().buscar(new FiltroBusqueda(texto, texto,
                    null, null, null, texto, texto, null, null, null, CriterioOrden.RELEVANCIA), PAG), "buscar");
            hostil("actualizar perfil nombre=" + t, () -> app.getPerfilController().actualizar(
                    new ActualizarPerfilCommand(texto, texto, "UTC", Set.of(), Set.of(), List.of(),
                            CanalNotificacion.INTERNA)), "perfil");
            hostil("actualizar perfil zona=" + t, () -> app.getPerfilController().actualizar(
                    new ActualizarPerfilCommand("Bruno", null, texto, Set.of(), Set.of(), List.of(),
                            CanalNotificacion.INTERNA)));
            como(ANA);
            hostil("rechazar motivo=" + t, () -> app.getContratacionController().rechazar(pendiente, texto));
            hostil("crear borrador titulo/categoria=" + t, () -> app.getPublicacionController().crearBorrador(
                    new CrearPublicacionCommand(TipoPublicacion.MENTORIA, texto, texto, texto, Modalidad.REMOTA, 60,
                            1, BigDecimal.ONE, Set.of(), Set.of(), null, List.of())), "borrador");
            hostil("cancelar publicacion motivo=" + t, () -> app.getPublicacionController().cancelar(borrador, texto));
            hostil("promocion descripcion=" + t, () -> app.getPublicacionController().agregarPromocion(
                    new CrearPromocionCommand(borrador, texto, TipoPromocion.MONTO_FIJO, BigDecimal.ONE, null, null)),
                    "promocion");
            Set<String> conjunto = new HashSet<>();
            conjunto.add(texto);
            hostil("crear borrador etiqueta/habilidad=" + t, () -> app.getPublicacionController().crearBorrador(
                    new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA,
                            60, 1, BigDecimal.ONE, conjunto, conjunto, null, List.of())), "borrador");
        }

        // --- numeros ---
        como(ANA);
        for (String monto : List.of("-1", "-0.01", "0.001", "99999999999999999.99", "1e18", "1e30", "1e400")) {
            BigDecimal valor = new BigDecimal(monto);
            hostil("crear borrador precio=" + monto, () -> app.getPublicacionController().crearBorrador(
                    comando(valor)), "borrador");
            hostil("promocion monto fijo=" + monto, () -> app.getPublicacionController().agregarPromocion(
                    new CrearPromocionCommand(borrador, "Promo", TipoPromocion.MONTO_FIJO, valor, null, null)),
                    "promocion");
            hostil("promocion porcentaje=" + monto, () -> app.getPublicacionController().agregarPromocion(
                    new CrearPromocionCommand(borrador, "Promo", TipoPromocion.PORCENTAJE, valor, null, null)),
                    "promocion");
        }
        hostil("crear borrador precio=null y luego publicar", () -> {
            Resultado<PublicacionDto> b = app.getPublicacionController().crearBorrador(comando((BigDecimal) null));
            return b.esError() ? b : app.getPublicacionController().publicar(b.valor().id());
        });
        hostil("promocion porcentaje=101", () -> app.getPublicacionController().agregarPromocion(
                new CrearPromocionCommand(borrador, "Promo", TipoPromocion.PORCENTAJE, new BigDecimal("101"), null,
                        null)));
        hostil("promocion valor=0", () -> app.getPublicacionController().agregarPromocion(
                new CrearPromocionCommand(borrador, "Promo", TipoPromocion.PORCENTAJE, BigDecimal.ZERO, null, null)));
        hostil("promocion tipo=null", () -> app.getPublicacionController().agregarPromocion(
                new CrearPromocionCommand(borrador, "Promo", null, BigDecimal.ONE, null, null)));
        hostil("promocion vigencia invertida", () -> app.getPublicacionController().agregarPromocion(
                new CrearPromocionCommand(borrador, "Promo", TipoPromocion.PORCENTAJE, BigDecimal.TEN, enDias(5),
                        enDias(2))));
        hostil("promocion comando=null", () -> app.getPublicacionController().agregarPromocion(null));
        for (int n : new int[] {-1, 0, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            hostil("crear borrador cupo=" + n, () -> app.getPublicacionController().crearBorrador(
                    new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA,
                            60, n, BigDecimal.ONE, Set.of(), Set.of(), null, List.of())), "borrador");
            hostil("crear borrador duracion=" + n, () -> app.getPublicacionController().crearBorrador(
                    new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA,
                            n, 1, BigDecimal.ONE, Set.of(), Set.of(), null, List.of())), "borrador");
            como(BRUNO);
            hostil("calificar puntaje=" + n, () -> app.getCalificacionController().calificar(
                    new CalificarCommand(completada, n, "ok")));
            como(ANA);
        }
        hostil("publicar un borrador con cupo 0", () -> {
            Resultado<PublicacionDto> b = app.getPublicacionController().crearBorrador(new CrearPublicacionCommand(
                    TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA, 60, 0, BigDecimal.ONE,
                    Set.of(), Set.of(), null, List.of(new FranjaCommand(enDias(3), enDias(3).plusHours(1)))));
            return b.esError() ? b : app.getPublicacionController().publicar(b.valor().id());
        });
        hostil("crear borrador comando=null", () -> app.getPublicacionController().crearBorrador(null));
        hostil("crear borrador tipo=null", () -> app.getPublicacionController().crearBorrador(
                new CrearPublicacionCommand(null, "Titulo", "Desc", "cat", null, 60, 1, BigDecimal.ONE, null, null,
                        null, null)));
        hostil("crear un DESAFIO siendo profesional", () -> app.getPublicacionController().crearBorrador(
                new CrearPublicacionCommand(TipoPublicacion.DESAFIO, "Titulo", "Desc", "cat", Modalidad.REMOTA, 60, 1,
                        BigDecimal.ONE, Set.of(), Set.of(), null, List.of())));
        Set<String> milEtiquetas = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            milEtiquetas.add("etiqueta" + i);
        }
        hostil("crear borrador con 2000 etiquetas", () -> app.getPublicacionController().crearBorrador(
                new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA, 60,
                        1, BigDecimal.ONE, milEtiquetas, Set.of(), null, List.of())), "borrador");
        hostil("editar comando=null", () -> app.getPublicacionController().editar(borrador, null));
        hostil("cambiarEstado accion=null", () -> app.getPublicacionController().cambiarEstado(borrador, null, null));

        // --- fechas ---
        hostil("franja inicio=null", () -> app.getPublicacionController().agregarFranja(pub, null, enDias(3)));
        hostil("franja fin=null", () -> app.getPublicacionController().agregarFranja(pub, enDias(3), null));
        hostil("franja invertida", () -> app.getPublicacionController().agregarFranja(pub, enDias(4), enDias(3)));
        hostil("franja de duracion cero", () -> app.getPublicacionController().agregarFranja(pub, enDias(4), enDias(4)));
        hostil("franja en el pasado", () -> app.getPublicacionController().agregarFranja(pub,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(1)));
        hostil("franja en el anio 9999", () -> app.getPublicacionController().agregarFranja(pub,
                LocalDateTime.of(9999, 12, 31, 22, 0), LocalDateTime.of(9999, 12, 31, 23, 0)), "franja");
        hostil("franja LocalDateTime.MAX", () -> app.getPublicacionController().agregarFranja(pub,
                LocalDateTime.MAX.minusHours(1), LocalDateTime.MAX), "franja");
        hostil("franja de 50 anios", () -> app.getPublicacionController().agregarFranja(pub, enDias(3),
                enDias(3).plusYears(50)), "franja");
        hostil("crear borrador con franja null en la lista", () -> app.getPublicacionController().crearBorrador(
                new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA, 60,
                        1, BigDecimal.ONE, Set.of(), Set.of(), null, Arrays.asList((FranjaCommand) null))));

        // --- busqueda, recomendacion y paginas ---
        hostil("buscar filtro=null", () -> app.getBusquedaController().buscar(null, PAG), "buscar");
        hostil("buscar pagina=null", () -> app.getBusquedaController().buscar(FiltroBusqueda.vacio(), null));
        for (Double rep : Arrays.asList(-1.0, 5.01, Double.NaN, Double.POSITIVE_INFINITY)) {
            hostil("buscar reputacion minima=" + rep, () -> app.getBusquedaController().buscar(new FiltroBusqueda(
                    null, null, null, null, null, null, null, rep, null, null, null), PAG));
        }
        hostil("buscar disponibilidad invertida", () -> app.getBusquedaController().buscar(new FiltroBusqueda(null,
                null, null, null, null, null, null, null, enDias(9), enDias(1), null), PAG));
        PaginaRequest lejos = PaginaRequest.de(Integer.MAX_VALUE, 100); // offset = pagina * tamano desborda el int
        hostil("buscar pagina " + Integer.MAX_VALUE, () -> app.getBusquedaController().buscar(FiltroBusqueda.vacio(),
                lejos), "pagina");
        hostil("mis publicaciones pagina " + Integer.MAX_VALUE,
                () -> app.getPublicacionController().misPublicaciones(lejos), "pagina");
        hostil("mis contrataciones pagina " + Integer.MAX_VALUE,
                () -> app.getContratacionController().misContrataciones(lejos), "pagina");
        hostil("movimientos pagina " + Integer.MAX_VALUE, () -> app.getWalletController().movimientos(lejos),
                "pagina");
        hostil("notificaciones pagina " + Integer.MAX_VALUE,
                () -> app.getNotificacionController().listar(false, lejos), "pagina");
        hostil("calificaciones pagina " + Integer.MAX_VALUE,
                () -> app.getCalificacionController().misRecibidas(lejos), "pagina");
        hostil("historial pagina " + Integer.MAX_VALUE,
                () -> app.getContratacionController().historial(completada, lejos), "pagina");
        hostil("recomendar pagina " + Integer.MAX_VALUE, () -> app.getRecomendacionController().recomendar(
                TipoEstrategiaRecomendacion.AFINIDAD, lejos), "pagina");
        hostil("recomendar estrategia=null",
                () -> app.getRecomendacionController().recomendar(null, PAG), "recomendar");
        hostil("recomendar pagina=null",
                () -> app.getRecomendacionController().recomendar(TipoEstrategiaRecomendacion.POPULARIDAD, null));
        hostil("movimientos pagina=null", () -> app.getWalletController().movimientos(null));

        // --- perfil ---
        como(BRUNO);
        hostil("actualizar perfil comando=null", () -> app.getPerfilController().actualizar(null));
        hostil("actualizar perfil zona inexistente", () -> app.getPerfilController().actualizar(
                new ActualizarPerfilCommand("Bruno", null, "Marte/Fobos", Set.of(), Set.of(), List.of(),
                        CanalNotificacion.INTERNA)));
        hostil("actualizar perfil bloque invertido", () -> app.getPerfilController().actualizar(
                new ActualizarPerfilCommand("Bruno", null, "UTC", Set.of(), Set.of(), List.of(
                        new BloqueDisponibilidadDto(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(9, 0))),
                        CanalNotificacion.INTERNA)));
        hostil("actualizar perfil bloque con nulls", () -> app.getPerfilController().actualizar(
                new ActualizarPerfilCommand("Bruno", null, "UTC", Set.of(), Set.of(), List.of(
                        new BloqueDisponibilidadDto(null, null, null)), CanalNotificacion.INTERNA)));
        hostil("actualizar perfil colecciones null y canal null", () -> app.getPerfilController().actualizar(
                new ActualizarPerfilCommand("Bruno", null, "UTC", null, null, null, null)), "perfil");
        hostil("registrar comando=null", () -> app.getPerfilController().registrar(null));
        hostil("registrar por segunda vez", () -> app.getPerfilController().registrar(new RegistrarPerfilCommand(
                BRUNO, "Bruno", TipoPerfil.PROFESIONAL, "UTC", null, Set.of(), Set.of(), List.of(),
                CanalNotificacion.INTERNA)));
        como(TECH);
        hostil("registrar tipo=null", () -> app.getPerfilController().registrar(new RegistrarPerfilCommand(TECH,
                "Tech", null, "UTC", null, Set.of(), Set.of(), List.of(), CanalNotificacion.INTERNA)));
        hostil("solicitar comando=null", () -> app.getContratacionController().solicitar(null));
        hostil("calificar comando=null", () -> app.getCalificacionController().calificar(null));

        if (!foto(pub, borrador, pendiente, completada).equals(fotoAntes)) {
            System.out.println("[AREA3] nota: el estado cambio durante las entradas hostiles (alguna fue aceptada)");
        }
        cerrar();
    }

    /** Entrada hostil: lo unico inadmisible es ERROR_INTERNO o una excepcion; lo aceptado se lista para revisarlo. */
    static void hostil(String nombre, Supplier<Resultado<?>> llamada) {
        hostil(nombre, llamada, null);
    }

    /** {@code grupo}: si no es null, un resultado OK es esperable para ese grupo y solo se cuenta. */
    static void hostil(String nombre, Supplier<Resultado<?>> llamada, String grupo) {
        Resultado<?> r;
        try {
            r = llamada.get();
        } catch (RuntimeException | Error e) {
            problema("HOSTIL " + nombre + ": el controller lanzo " + e.getClass().getSimpleName() + ": "
                    + String.valueOf(e.getMessage()).lines().findFirst().orElse(""));
            return;
        }
        if (r.esOk()) {
            System.out.println("[AREA3] aceptada: " + nombre);
            contar("entrada hostil ACEPTADA");
        } else if (r.codigo() == ErrorCode.ERROR_INTERNO) {
            problema("HOSTIL " + nombre + ": ERROR_INTERNO");
        } else {
            contar("entrada hostil rechazada con " + r.codigo());
        }
    }

    // =====================================================================================================
    // Parte 4: verificaciones sueltas que salieron de leer el esquema
    // =====================================================================================================

    /** El motivo admite 1000 caracteres y el mensaje de la notificacion (tambien VARCHAR(1000)) lo incluye. */
    static void notificacionConMotivoLargo() {
        iniciar(ANA, BRUNO, CECILIA);
        for (int largo : new int[] {100, 960, 1000}) {
            Long c = pagada(ANA, BRUNO, "1.00");
            como(ANA);
            long antes = app.getNotificacionController().listar(false, PAG).valor().totalElementos();
            como(BRUNO);
            Resultado<?> r = app.getContratacionController().cancelar(c, "m".repeat(largo));
            como(ANA);
            long despues = app.getNotificacionController().listar(false, PAG).valor().totalElementos();
            System.out.println("[AREA3] cancelar con motivo de " + largo + " caracteres -> " + (r.esOk() ? "OK"
                    : r.codigo()) + "; notificaciones nuevas del proveedor: " + (despues - antes));
            if (r.esOk() && despues == antes) {
                problema("cancelacion con motivo de " + largo + " caracteres: se cancelo pero el proveedor NO fue"
                        + " notificado");
            }
        }
        cerrar();
    }

    /** En MySQL la comparacion de textos no distingue mayusculas; en H2 si. Aca solo se ve que hace H2. */
    static void textosQueSoloDifierenEnMayusculas() {
        iniciar(ANA, BRUNO, CECILIA);
        como(ANA);
        Resultado<?> etiquetas = app.getPublicacionController().crearBorrador(new CrearPublicacionCommand(
                TipoPublicacion.MENTORIA, "Titulo", "Desc", "cat", Modalidad.REMOTA, 60, 1, BigDecimal.ONE,
                Set.of("Java", "java"), Set.of(), null, List.of()));
        Resultado<?> intereses = app.getPerfilController().actualizar(new ActualizarPerfilCommand("Ana", null, "UTC",
                Set.of(), Set.of("Java", "java"), List.of(), CanalNotificacion.INTERNA));
        System.out.println("[AREA3] etiquetas {Java, java} en H2 -> " + (etiquetas.esOk() ? "aceptadas"
                : etiquetas.codigo()) + "; intereses {Java, java} en H2 -> " + (intereses.esOk() ? "aceptados"
                : intereses.codigo()));
        cerrar();
    }

    /** Cuenta las sentencias SQL de cada listado con n publicaciones y n contrataciones: si crece con n, hay N+1. */
    static void consultasPorListado(int n) {
        consultasPorListado(n, false);
    }

    /**
     * Con conCompletadas, ademas, n/2 contrataciones de Ana pagadas y completadas: asi la consulta de pagadas
     * recientes (popularidad) y la pagina de completadas del perfil publico tienen filas que cargar.
     */
    static void consultasPorListado(int n, boolean conCompletadas) {
        iniciar(ANA, BRUNO, CECILIA);
        for (int i = 0; i < n; i++) {
            PublicacionDto p = publicar(i % 2 == 0 ? ANA : CECILIA, "1.00", 3, 2);
            solicitar(BRUNO, p, 0);
            if (conCompletadas && i % 2 == 0) {
                Long c = pagada(ANA, BRUNO, "0.50");
                como(ANA);
                if (!app.getContratacionController().iniciar(c).esOk()
                        || !app.getContratacionController().completar(c).esOk()) {
                    problema("consultas: no se pudo completar la contratacion " + c);
                }
            }
        }
        if (conCompletadas) {
            como(BRUNO);
            int completadas = app.getPerfilController().verPerfilPublico(perfilId.get(ANA)).valor()
                    .contratacionesCompletadas();
            if (completadas != n / 2) {
                problema("consultas: Ana tiene " + completadas + " completadas, se esperaban " + n / 2);
            }
        }
        org.hibernate.stat.Statistics st = ctx.emf().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        st.setStatisticsEnabled(true);
        PaginaRequest cien = PaginaRequest.de(0, 100);
        com.uade.skillswap.presentation.ui.swing.contrato.UiControllers ui = app.crearControllersUi(identidad);
        Map<String, Runnable> listados = new LinkedHashMap<>();
        listados.put("buscar", () -> app.getBusquedaController().buscar(FiltroBusqueda.vacio(), cien));
        listados.put("recomendar", () -> app.getRecomendacionController().recomendar(
                TipoEstrategiaRecomendacion.AFINIDAD, cien));
        listados.put("recomendar (popularidad)", () -> app.getRecomendacionController().recomendar(
                TipoEstrategiaRecomendacion.POPULARIDAD, cien));
        listados.put("mis contrataciones", () -> app.getContratacionController().misContrataciones(cien));
        listados.put("mis publicaciones (Ana)", () -> {
            como(ANA);
            app.getPublicacionController().misPublicaciones(cien);
        });
        listados.put("notificaciones (Ana)", () -> {
            como(ANA);
            app.getNotificacionController().listar(false, cien);
        });
        listados.put("movimientos", () -> app.getWalletController().movimientos(cien));
        listados.put("perfil publico de Ana", () -> app.getPerfilController().verPerfilPublico(perfilId.get(ANA)));
        listados.put("pantalla: listarContrataciones", () -> ui.contrataciones().listarContrataciones(BRUNO, 0));
        listados.put("pantalla: listarPropias (Ana)", () -> ui.publicaciones().listarPropias(ANA, 0));
        listados.put("pantalla: verPerfil (Ana)", () -> ui.perfiles().verPerfil(ANA));
        listados.put("pantalla: verPerfilPublico (Ana, visto por Bruno)", () -> ui.perfiles().verPerfilPublico(BRUNO,
                String.valueOf(perfilId.get(ANA))));
        listados.put("pantalla: listarNotificaciones (Ana)", () -> ui.notificaciones().listarNotificaciones(ANA, 0));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Runnable> e : listados.entrySet()) {
            como(BRUNO);
            long antes = st.getPrepareStatementCount();
            e.getValue().run();
            sb.append(e.getKey()).append('=').append(st.getPrepareStatementCount() - antes).append("; ");
        }
        System.out.println("[AREA3] sentencias SQL con n=" + n + (conCompletadas ? " y n/2 completadas" : "")
                + ": " + sb);
        cerrar();
    }

    // =====================================================================================================
    // soporte
    // =====================================================================================================

    static void iniciar(String... usuarios) {
        ctx = BaseDeDatos.iniciarH2();
        identidad = IdentidadMockAdapter.conUsuariosDemo();
        app = Composicion.crear(ctx, identidad, SkillSwapConfig.porDefecto());
        perfilId.clear();
        clienteDe.clear();
        for (String usuario : usuarios) {
            como(usuario);
            perfilId.put(usuario, app.getPerfilController().registrar(new RegistrarPerfilCommand(usuario,
                    "Perfil " + usuario, TipoPerfil.PROFESIONAL, "UTC", null, Set.of(), Set.of(), List.of(),
                    CanalNotificacion.INTERNA)).valor().id());
        }
    }

    static void cerrar() {
        identidad.cerrarSesion();
        app.cerrar();
    }

    static void como(String usuario) {
        identidad.iniciarSesion(usuario);
    }

    static LocalDateTime enDias(int dias) {
        return LocalDateTime.now().plusDays(dias).plusHours(franjasCreadas++ % 20).withNano(0);
    }

    static CrearPublicacionCommand comando(String precio) {
        return comando(new BigDecimal(precio));
    }

    static CrearPublicacionCommand comando(BigDecimal precio) {
        return new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Mentoria", "Descripcion", "programacion",
                Modalidad.REMOTA, 60, 1, precio, Set.of("java"), Set.of("java"), TipoPoliticaCancelacion.FLEXIBLE,
                List.of(new FranjaCommand(enDias(5), enDias(5).plusHours(1))));
    }

    static EditarPublicacionCommand edicion() {
        return new EditarPublicacionCommand("Hackeada", "Desc", "cat", Modalidad.REMOTA, 60, 1, BigDecimal.ZERO,
                Set.of(), Set.of(), null);
    }

    static PublicacionDto borrador(String autor, String precio, int cupo, int franjas) {
        List<FranjaCommand> lista = new ArrayList<>();
        for (int i = 0; i < franjas; i++) {
            LocalDateTime inicio = LocalDateTime.now().plusDays(5).plusHours(franjasCreadas++).withNano(0);
            lista.add(new FranjaCommand(inicio, inicio.plusHours(1)));
        }
        como(autor);
        return app.getPublicacionController().crearBorrador(new CrearPublicacionCommand(TipoPublicacion.MENTORIA,
                "Mentoria", "Descripcion", "programacion", Modalidad.REMOTA, 60, cupo, new BigDecimal(precio),
                Set.of("java"), Set.of("java"), TipoPoliticaCancelacion.FLEXIBLE, lista)).valor();
    }

    static PublicacionDto publicar(String autor, String precio, int cupo, int franjas) {
        PublicacionDto b = borrador(autor, precio, cupo, franjas);
        return app.getPublicacionController().publicar(b.id()).valor();
    }

    static Long solicitar(String cliente, PublicacionDto p, int franja) {
        como(cliente);
        Long id = app.getContratacionController().solicitar(new SolicitarContratacionCommand(p.id(),
                p.franjas().get(franja).id(), "Quiero contratar", "Aprender")).valor().id();
        clienteDe.put(id, cliente);
        return id;
    }

    static Long aceptada(String proveedor, String cliente, String precio) {
        Long id = solicitar(cliente, publicar(proveedor, precio, 1, 1), 0);
        como(proveedor);
        app.getContratacionController().aceptar(id).valor();
        return id;
    }

    static Long pagada(String proveedor, String cliente, String precio) {
        Long id = aceptada(proveedor, cliente, precio);
        como(cliente);
        app.getContratacionController().pagar(id).valor();
        return id;
    }

    static BigDecimal saldo(String usuario) {
        como(usuario);
        return app.getWalletController().consultar().valor().saldo();
    }

    static void exigirSaldo(String donde, String usuario, BigDecimal esperado) {
        BigDecimal real = saldo(usuario);
        if (real.compareTo(esperado) != 0) {
            problema(donde + ": saldo de " + usuario + " = " + real + ", se esperaba " + esperado);
        }
    }

    static long calificacionesDe(String usuario) {
        como(usuario);
        return app.getCalificacionController().misRecibidas(PAG).valor().totalElementos();
    }

    /** Estado visible de una publicacion (vista por Ana) y de las contrataciones (vistas por Bruno), mas saldos. */
    static String foto(Long pubId, Long borradorId, Long... contrataciones) {
        StringBuilder sb = new StringBuilder();
        como(ANA);
        for (Long id : new Long[] {pubId, borradorId}) {
            PublicacionDto p = app.getPublicacionController().obtener(id).valor();
            sb.append(p.estado()).append(p.titulo()).append(p.cupoDisponible()).append(p.precioFinal())
                    .append(p.historial().size()).append(p.franjas().size());
            for (FranjaDto f : p.franjas()) {
                sb.append(f.ocupada());
            }
        }
        como(BRUNO);
        for (Long id : contrataciones) {
            ContratacionDto c = app.getContratacionController().obtener(id).valor();
            sb.append(c.estado()).append(c.franjaId()).append(c.historial().size());
        }
        for (String u : List.of(ANA, BRUNO, CECILIA)) {
            sb.append(saldo(u));
        }
        return sb.toString();
    }

    static void contar(String clave) {
        conteo.merge(clave, 1, Integer::sum);
    }

    static void problema(String detalle) {
        problemas.add(detalle);
    }
}
