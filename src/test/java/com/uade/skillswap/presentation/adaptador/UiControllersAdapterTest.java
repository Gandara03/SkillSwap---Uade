package com.uade.skillswap.presentation.adaptador;

import com.uade.skillswap.acceptance.jpa.AceptacionJpaBase;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models;
import com.uade.skillswap.presentation.ui.swing.contrato.OperacionFallidaException;
import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion.ACEPTAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion.CALIFICAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion.CANCELAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion.PAGAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion.RECHAZAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionPublicacion.CERRAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionPublicacion.PAUSAR;
import static com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionPublicacion.PUBLICAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El contrato de la pantalla contra la aplicacion real (Composicion + JPA + H2), sin abrir ninguna ventana.
 * Las acciones se hacen solo a traves de {@link UiControllers}, con el userId que mandaria la pantalla.
 */
@DisplayName("UiControllersAdapter: contrato de la pantalla sobre los controllers reales")
class UiControllersAdapterTest extends AceptacionJpaBase {

    private static final String ANA = "u-ana";
    private static final String BRUNO = "u-bruno";
    private static final LocalDateTime INICIO_FRANJA = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.MINUTES);

    private UiControllers ui;

    @BeforeEach
    void prepararUi() {
        registrar(ANA, "Ana Gomez", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of("java"), Set.of("java"));
        registrar(BRUNO, "Bruno Diaz", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of("java"));
        identidad.cerrarSesion();
        ui = app.crearControllersUi(identidad);
    }

    // ---------- recorrido principal ----------

    @Test
    @DisplayName("publicar, buscar, solicitar, aceptar, pagar, iniciar, completar y calificar, con los botones de cada parte")
    void recorridoCompleto() {
        Models.Publicacion borrador = ui.publicaciones().crearBorrador(ANA, mentoria("Mentoria de Java", 40));
        assertEquals(Models.EstadoPublicacion.BORRADOR, borrador.estado());
        assertEquals(List.of(PUBLICAR, Models.AccionPublicacion.CANCELAR), borrador.accionesDelAutor());
        assertEquals("Ana Gomez", borrador.proveedor());

        Models.Publicacion publicada = ui.publicaciones().cambiarEstado(ANA, borrador.id(), PUBLICAR, null);
        assertEquals(Models.EstadoPublicacion.PUBLICADA, publicada.estado());
        assertEquals(List.of(PAUSAR, CERRAR, Models.AccionPublicacion.CANCELAR), publicada.accionesDelAutor());

        Models.Publicacion encontrada = ui.busqueda().buscar(new Models.FiltrosBusqueda("java",
                UiControllers.BusquedaController.TODAS_LAS_CATEGORIAS, null, Models.OrdenBusqueda.RELEVANCIA)).get(0);
        assertEquals(publicada.id(), encontrada.id());
        assertEquals("Sesion practica de Mentoria de Java", encontrada.descripcion());
        assertEquals(60, encontrada.duracionMinutos());
        assertEquals(Set.of("java", "patrones"), Set.copyOf(encontrada.etiquetas()));
        assertMonto(40, encontrada.precio());

        Models.Contratacion solicitud = solicitar(BRUNO, encontrada);
        assertEquals(Models.EstadoContratacion.PENDIENTE, solicitud.estado());
        assertEquals(List.of(), solicitud.acciones(), "el cliente no puede retirar la solicitud");
        assertEquals("Proveedor: Ana Gomez", solicitud.contraparte());
        assertEquals(INICIO_FRANJA, solicitud.fecha());

        Models.Contratacion vistaDeAna = contratacionDe(ANA, solicitud.id());
        assertEquals(List.of(ACEPTAR, RECHAZAR), vistaDeAna.acciones());
        assertEquals("Cliente: Bruno Diaz", vistaDeAna.contraparte());

        assertEquals(List.of(CANCELAR), ui.contrataciones().aceptar(ANA, solicitud.id()).acciones());
        assertEquals(List.of(PAGAR, CANCELAR), contratacionDe(BRUNO, solicitud.id()).acciones());

        ui.contrataciones().pagar(BRUNO, solicitud.id());
        assertMonto(60, ui.wallet().consultarSaldo(BRUNO));
        ui.contrataciones().iniciar(ANA, solicitud.id());
        Models.Contratacion completada = ui.contrataciones().completar(ANA, solicitud.id());
        assertEquals(Models.EstadoContratacion.COMPLETADA, completada.estado());
        assertEquals(List.of(CALIFICAR), completada.acciones());
        assertEquals(List.of(CALIFICAR), contratacionDe(BRUNO, solicitud.id()).acciones());
        assertMonto(140, ui.wallet().consultarSaldo(ANA));

        ui.calificaciones().calificar(BRUNO, solicitud.id(), 5, "Muy clara");

        Models.Perfil perfilDeAna = ui.perfiles().verPerfil(ANA);
        assertEquals("u-ana", perfilDeAna.id());
        assertEquals("Profesional", perfilDeAna.rol());
        assertEquals(5.0, perfilDeAna.reputacion());
        assertEquals(1, perfilDeAna.resenas());
        assertEquals(1, perfilDeAna.proyectosCompletados());
        assertEquals(5, perfilDeAna.ultimasResenas().get(0).puntaje());
        assertEquals("Muy clara", perfilDeAna.ultimasResenas().get(0).comentario());
        assertEquals(0, ui.perfiles().verPerfil(BRUNO).proyectosCompletados(), "Bruno fue el cliente");
    }

    @Test
    @DisplayName("los movimientos llegan del mas nuevo al mas viejo, con signo y con el saldo que quedo")
    void movimientosConSigno() {
        Models.Contratacion pagada = contratadaYPagada();

        List<Models.Movimiento> movimientos = ui.wallet().listarMovimientos(BRUNO);

        Models.Movimiento pago = movimientos.get(0);
        assertTrue(pago.concepto().startsWith("Pago"), pago.concepto());
        assertTrue(pago.concepto().endsWith("#" + pagada.id()), pago.concepto());
        assertMonto(-40, pago.importe());
        assertMonto(60, pago.saldo());
        Models.Movimiento bono = movimientos.get(movimientos.size() - 1);
        assertEquals("Bono de bienvenida", bono.concepto());
        assertMonto(100, bono.importe());
    }

    @Test
    @DisplayName("cancelar devuelve la contratacion sin acciones y el reparto de lo retenido segun la politica")
    void cancelarInformaElReparto() {
        Models.Contratacion pagada = contratadaYPagada();

        Models.ResultadoCancelacion resultado = ui.contrataciones().cancelar(BRUNO, pagada.id(), "Me surgio algo");

        assertEquals(Models.EstadoContratacion.CANCELADA, resultado.contratacion().estado());
        assertEquals(List.of(), resultado.contratacion().acciones());
        assertEquals("FLEXIBLE", resultado.politica());
        assertMonto(40, resultado.reintegroCliente().add(resultado.compensacionProveedor()));
    }

    @Test
    @DisplayName("notificaciones: titulo legible, marcar una y marcar todas")
    void notificaciones() {
        Models.Publicacion publicada = publicar("Mentoria de Java", 40);
        solicitar(BRUNO, publicada);
        solicitar(BRUNO, publicada);

        List<Models.Notificacion> deAna = ui.notificaciones().listarNotificaciones(ANA);
        assertEquals(2, deAna.size());
        assertEquals("Nueva solicitud", deAna.get(0).titulo());
        assertFalse(deAna.get(0).leida());

        ui.notificaciones().marcarLeida(ANA, deAna.get(0).id());
        assertEquals(1, noLeidas(ANA));

        ui.notificaciones().marcarTodasLeidas(ANA);
        assertEquals(0, noLeidas(ANA));
    }

    // ---------- busqueda ----------

    @Test
    @DisplayName("categorias: \"Todas\" primero y despues las de las publicaciones publicadas, sin repetir")
    void categorias() {
        publicar("Mentoria de Java", 40);
        publicar("Mentoria de patrones", 50);
        Models.Publicacion diseno = ui.publicaciones().crearBorrador(ANA, oferta("Taller de UX", "Diseno", 30));
        ui.publicaciones().cambiarEstado(ANA, diseno.id(), PUBLICAR, null);

        assertEquals(List.of("Todas", "Diseno", "Programacion"), ui.busqueda().categorias());
    }

    @Test
    @DisplayName("buscar: \"Todas\" no filtra, una categoria si, y el orden por menor precio es ascendente")
    void filtrosYOrden() {
        publicar("Mentoria de Java", 40);
        Models.Publicacion diseno = ui.publicaciones().crearBorrador(ANA, oferta("Taller de UX", "Diseno", 30));
        ui.publicaciones().cambiarEstado(ANA, diseno.id(), PUBLICAR, null);

        List<Models.Publicacion> todas = ui.busqueda().buscar(new Models.FiltrosBusqueda(" ",
                UiControllers.BusquedaController.TODAS_LAS_CATEGORIAS, null, Models.OrdenBusqueda.MENOR_PRECIO));
        assertEquals(List.of("Taller de UX", "Mentoria de Java"), todas.stream().map(Models.Publicacion::titulo).toList());

        List<Models.Publicacion> soloDiseno = ui.busqueda().buscar(new Models.FiltrosBusqueda(null, "Diseno",
                Models.Modalidad.REMOTA, Models.OrdenBusqueda.MAS_RECIENTES));
        assertEquals(List.of("Taller de UX"), soloDiseno.stream().map(Models.Publicacion::titulo).toList());

        assertEquals(2, ui.busqueda().buscar(null).size(), "sin filtros = todas las publicadas");
    }

    @Test
    @DisplayName("con una promocion vigente la publicacion sale destacada y con el precio final")
    void destacadaConPromocion() {
        Models.Publicacion publicada = publicar("Mentoria de Java", 40);
        promocionar(ANA, Long.valueOf(publicada.id()), TipoPromocion.PORCENTAJE, 20);

        Models.Publicacion encontrada = ui.busqueda().buscar(null).get(0);

        assertTrue(encontrada.destacada());
        assertMonto(32, encontrada.precio());
    }

    @Test
    @DisplayName("las recomendaciones usan la sesion del usuario: a Bruno, interesado en java, le aparece la mentoria")
    void recomendaciones() {
        Models.Publicacion publicada = publicar("Mentoria de Java", 40);

        assertEquals(List.of(publicada.id()), ui.busqueda().recomendar(BRUNO).stream().map(Models.Publicacion::id).toList());
        assertEquals(List.of(), ui.busqueda().recomendar(ANA), "no se recomiendan las publicaciones propias");
    }

    // ---------- errores que llegan a la pantalla ----------

    @Test
    @DisplayName("solicitar la propia publicacion es REGLA_DE_NEGOCIO")
    void solicitarLaPropia() {
        Models.Publicacion publicada = publicar("Mentoria de Java", 40);

        falla("REGLA_DE_NEGOCIO", () -> solicitar(ANA, publicada));
    }

    @Test
    @DisplayName("pagar sin saldo es SALDO_INSUFICIENTE y no cambia ni el saldo ni el estado")
    void pagarSinSaldo() {
        Models.Publicacion cara = publicar("Mentoria intensiva", 150);
        Models.Contratacion solicitud = solicitar(BRUNO, cara);
        ui.contrataciones().aceptar(ANA, solicitud.id());

        falla("SALDO_INSUFICIENTE", () -> ui.contrataciones().pagar(BRUNO, solicitud.id()));

        assertMonto(100, ui.wallet().consultarSaldo(BRUNO));
        assertEquals(Models.EstadoContratacion.ACEPTADA, contratacionDe(BRUNO, solicitud.id()).estado());
    }

    @Test
    @DisplayName("calificar dos veces la misma contratacion es REGLA_DE_NEGOCIO")
    void calificarDosVeces() {
        Models.Contratacion pagada = contratadaYPagada();
        ui.contrataciones().iniciar(ANA, pagada.id());
        ui.contrataciones().completar(ANA, pagada.id());
        ui.calificaciones().calificar(BRUNO, pagada.id(), 4, "Bien");

        falla("REGLA_DE_NEGOCIO", () -> ui.calificaciones().calificar(BRUNO, pagada.id(), 5, "Otra vez"));
    }

    @Test
    @DisplayName("cada parte solo puede lo suyo y en el estado que corresponde")
    void permisosYTransiciones() {
        Models.Contratacion solicitud = solicitar(BRUNO, publicar("Mentoria de Java", 40));

        falla("NO_AUTORIZADO", () -> ui.contrataciones().aceptar(BRUNO, solicitud.id()));
        falla("TRANSICION_INVALIDA", () -> ui.contrataciones().pagar(BRUNO, solicitud.id()));
        assertEquals(Models.EstadoContratacion.PENDIENTE, contratacionDe(ANA, solicitud.id()).estado());
    }

    @Test
    @DisplayName("rechazar y cancelar sin motivo es VALIDACION y la contratacion no cambia")
    void motivoObligatorio() {
        Models.Contratacion solicitud = solicitar(BRUNO, publicar("Mentoria de Java", 40));

        falla("VALIDACION", () -> ui.contrataciones().rechazar(ANA, solicitud.id(), "  "));
        ui.contrataciones().aceptar(ANA, solicitud.id());
        falla("VALIDACION", () -> ui.contrataciones().cancelar(BRUNO, solicitud.id(), null));

        assertEquals(Models.EstadoContratacion.ACEPTADA, contratacionDe(BRUNO, solicitud.id()).estado());
    }

    @Test
    @DisplayName("publicar sin franja falla con el detalle de lo que falta, y sin accion es VALIDACION")
    void publicarIncompleta() {
        Models.NuevaPublicacion sinFranja = new Models.NuevaPublicacion(Models.TipoPublicacion.MENTORIA, "Sin franja",
                "Descripcion", "Programacion", Models.Modalidad.REMOTA, 60, 1, BigDecimal.valueOf(40),
                Models.PoliticaCancelacion.FLEXIBLE, List.of(), List.of(), null, 0);
        Models.Publicacion borrador = ui.publicaciones().crearBorrador(ANA, sinFranja);

        String mensaje = falla("REGLA_DE_NEGOCIO",
                () -> ui.publicaciones().cambiarEstado(ANA, borrador.id(), PUBLICAR, null));
        assertTrue(mensaje.contains("franja"), mensaje);
        falla("VALIDACION", () -> ui.publicaciones().cambiarEstado(ANA, borrador.id(), null, null));
        falla("VALIDACION", () -> ui.publicaciones().crearBorrador(ANA, null));
    }

    @Test
    @DisplayName("ids que no son numeros, o no positivos, son VALIDACION")
    void idsInvalidos() {
        falla("VALIDACION", () -> ui.contrataciones().aceptar(ANA, "abc"));
        falla("VALIDACION", () -> ui.notificaciones().marcarLeida(ANA, "uno"));
        falla("VALIDACION", () -> ui.publicaciones().cambiarEstado(ANA, "-1", PUBLICAR, null));
        falla("VALIDACION", () -> ui.contrataciones().pagar(BRUNO, null));
    }

    @Test
    @DisplayName("un usuario que la identidad no conoce, o que no tiene perfil, es NO_AUTORIZADO")
    void usuarioSinAcceso() {
        falla("NO_AUTORIZADO", () -> ui.wallet().consultarSaldo("u-nadie"));
        falla("NO_AUTORIZADO", () -> ui.perfiles().verPerfil("u-cecilia")); // existe en la identidad, sin perfil
    }

    // ---------- soporte ----------

    private Models.Publicacion publicar(String titulo, long precio) {
        Models.Publicacion borrador = ui.publicaciones().crearBorrador(ANA, mentoria(titulo, precio));
        return ui.publicaciones().cambiarEstado(ANA, borrador.id(), PUBLICAR, null);
    }

    private static Models.NuevaPublicacion mentoria(String titulo, long precio) {
        return oferta(titulo, "Programacion", precio);
    }

    /** Cupo 2 y una franja de una hora dentro de 3 dias. */
    private static Models.NuevaPublicacion oferta(String titulo, String categoria, long precio) {
        return new Models.NuevaPublicacion(Models.TipoPublicacion.MENTORIA, titulo, "Sesion practica de " + titulo,
                categoria, Models.Modalidad.REMOTA, 60, 2, BigDecimal.valueOf(precio),
                Models.PoliticaCancelacion.FLEXIBLE, List.of("java", "patrones"), List.of("java"), INICIO_FRANJA, 60);
    }

    private Models.Contratacion solicitar(String userId, Models.Publicacion publicacion) {
        Models.Franja franja = ui.publicaciones().franjasDisponibles(userId, publicacion.id()).get(0);
        return ui.contrataciones().solicitar(userId, publicacion.id(), franja.id(), "Hola, quiero la mentoria",
                "Ordenar mi proyecto");
    }

    /** Mentoria de 40 SC de Ana, solicitada por Bruno, aceptada y pagada. */
    private Models.Contratacion contratadaYPagada() {
        Models.Contratacion solicitud = solicitar(BRUNO, publicar("Mentoria de Java", 40));
        ui.contrataciones().aceptar(ANA, solicitud.id());
        return ui.contrataciones().pagar(BRUNO, solicitud.id());
    }

    private Models.Contratacion contratacionDe(String userId, String contratacionId) {
        return ui.contrataciones().listarContrataciones(userId).stream()
                .filter(c -> c.id().equals(contratacionId))
                .findFirst()
                .orElseThrow();
    }

    private long noLeidas(String userId) {
        return ui.notificaciones().listarNotificaciones(userId).stream().filter(n -> !n.leida()).count();
    }

    /** Exige que la operacion falle con ese codigo y devuelve el mensaje que veria el usuario. */
    private static String falla(String codigo, Executable operacion) {
        OperacionFallidaException error = assertThrows(OperacionFallidaException.class, operacion);
        assertEquals(codigo, error.getCodigo(), error.getMessage());
        return error.getMessage();
    }

    private static void assertMonto(long esperado, BigDecimal real) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(real), () -> "se esperaba " + esperado + " pero fue " + real);
    }
}
