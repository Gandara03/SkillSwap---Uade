package com.uade.skillswap.application.notificacion;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.port.out.CanalNotificacionFactory;
import com.uade.skillswap.application.service.ContratacionService;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.infrastructure.notificacion.CanalNotificacionFactoryImpl;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NotificadorObserver: destinatario por evento, INTERNA + canal preferido y tolerancia a fallas")
class NotificadorObserverTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;      // proveedora, canal EMAIL
    private PerfilDto bruno;    // cliente, canal PUSH
    private PerfilDto cecilia;  // sin canal externo (INTERNA)
    private NotificadorObserver observer;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        bruno = e.profesional("u-bruno", "Bruno", CanalNotificacion.PUSH);
        cecilia = e.profesional("u-cecilia", "Cecilia", CanalNotificacion.INTERNA);
        observer = observerCon(canalesReales());
    }

    private CanalNotificacionFactory canalesReales() {
        return new CanalNotificacionFactoryImpl(app.emailSdk, app.pushSdk, app.identidad);
    }

    private NotificadorObserver observerCon(CanalNotificacionFactory factory) {
        return new NotificadorObserver(app.notificaciones, app.perfiles, factory, app.transaccion, app.reloj);
    }

    /** Evento entre Bruno (cliente) y Ana (proveedora) originado por {@code actor}. */
    private ContratacionEvent evento(TipoEventoContratacion tipo, PerfilDto actor, String detalle) {
        return new ContratacionEvent(tipo, 77L, 5L, bruno.id(), ana.id(), actor.id(), detalle, app.reloj.ahora());
    }

    private List<Notificacion> nuevasTras(Runnable accion) {
        int antes = app.notificaciones.todas().size();
        accion.run();
        List<Notificacion> todas = app.notificaciones.todas();
        return todas.subList(antes, todas.size());
    }

    private static Level silenciar(Class<?> clase) {
        Logger log = Logger.getLogger(clase.getName());
        Level anterior = log.getLevel();
        log.setLevel(Level.OFF);
        return anterior;
    }

    private static void restaurar(Class<?> clase, Level nivel) {
        Logger.getLogger(clase.getName()).setLevel(nivel);
    }

    // ---------- destinatarios ----------

    @Test
    @DisplayName("SOLICITUD_RECIBIDA y PAGO_CONFIRMADO van al PROVEEDOR; SOLICITUD_ACEPTADA y SOLICITUD_RECHAZADA al CLIENTE")
    void destinatariosFijos() {
        assertEquals(ana.id(), unico(evento(TipoEventoContratacion.SOLICITUD_RECIBIDA, bruno, "Nueva solicitud")));
        assertEquals(ana.id(), unico(evento(TipoEventoContratacion.PAGO_CONFIRMADO, bruno, "Pago de 40 SC")));
        assertEquals(bruno.id(), unico(evento(TipoEventoContratacion.SOLICITUD_ACEPTADA, ana, "Aceptada")));
        assertEquals(bruno.id(), unico(evento(TipoEventoContratacion.SOLICITUD_RECHAZADA, ana, "Sin cupo")));
    }

    @Test
    @DisplayName("REPROGRAMACION_*, CANCELACION y SESION_COMPLETADA van a la CONTRAPARTE de quien origino el evento")
    void destinatariosSegunActor() {
        for (TipoEventoContratacion tipo : List.of(TipoEventoContratacion.REPROGRAMACION_PROPUESTA,
                TipoEventoContratacion.REPROGRAMACION_ACEPTADA, TipoEventoContratacion.REPROGRAMACION_RECHAZADA,
                TipoEventoContratacion.CANCELACION, TipoEventoContratacion.SESION_COMPLETADA)) {
            assertEquals(ana.id(), unico(evento(tipo, bruno, "x")), tipo + " originado por el cliente -> proveedor");
            assertEquals(bruno.id(), unico(evento(tipo, ana, "x")), tipo + " originado por el proveedor -> cliente");
        }
    }

    /** Dispara el evento y devuelve el destinatario (todas las notificaciones generadas deben ser del mismo). */
    private Long unico(ContratacionEvent evento) {
        List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(evento));
        assertFalse(nuevas.isEmpty(), "el evento " + evento.getTipo() + " debe generar notificaciones");
        Long destinatario = nuevas.get(0).getDestinatarioId();
        assertTrue(nuevas.stream().allMatch(n -> n.getDestinatarioId().equals(destinatario)
                && n.getTipoEvento() == evento.getTipo()));
        return destinatario;
    }

    // ---------- dos tipos de notificacion por evento ----------

    @Test
    @DisplayName("destinatario con canal EMAIL: recibe la INTERNA y ademas un correo (dos notificaciones por evento)")
    void internaMasEmail() {
        List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECIBIDA, bruno, "Nueva solicitud")));

        assertEquals(List.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL), nuevas.stream().map(Notificacion::getCanal).toList());
        assertEquals(1, app.emailSdk.enviados().size());
        var correo = app.emailSdk.enviados().get(0);
        assertEquals("u-ana@test.local", correo.destino());
        assertTrue(correo.asunto().contains("SOLICITUD_RECIBIDA"));
        assertEquals(nuevas.get(1).getMensaje(), correo.cuerpo());
        assertTrue(app.pushSdk.enviados().isEmpty());
    }

    @Test
    @DisplayName("destinatario con canal PUSH: recibe la INTERNA y ademas un push al dispositivo del usuario")
    void internaMasPush() {
        List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(evento(TipoEventoContratacion.SOLICITUD_ACEPTADA, ana, "Aceptada")));

        assertEquals(List.of(CanalNotificacion.INTERNA, CanalNotificacion.PUSH), nuevas.stream().map(Notificacion::getCanal).toList());
        assertEquals(1, app.pushSdk.enviados().size());
        assertEquals("dispositivo-u-bruno", app.pushSdk.enviados().get(0).dispositivo());
        assertEquals(nuevas.get(1).getMensaje(), app.pushSdk.enviados().get(0).texto());
        assertTrue(app.emailSdk.enviados().isEmpty());
    }

    @Test
    @DisplayName("destinatario con canal INTERNA: una sola notificacion y ninguna llamada a los SDK externos")
    void soloInterna() {
        ContratacionEvent paraCecilia = new ContratacionEvent(TipoEventoContratacion.SOLICITUD_ACEPTADA, 9L, 5L,
                cecilia.id(), ana.id(), ana.id(), "ok", app.reloj.ahora());

        List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(paraCecilia));

        assertEquals(1, nuevas.size());
        assertEquals(CanalNotificacion.INTERNA, nuevas.get(0).getCanal());
        assertEquals(cecilia.id(), nuevas.get(0).getDestinatarioId());
        assertTrue(app.emailSdk.enviados().isEmpty());
        assertTrue(app.pushSdk.enviados().isEmpty());
    }

    @Test
    @DisplayName("las notificaciones nacen sin leer, con la fecha del reloj y un mensaje que referencia la contratacion")
    void contenidoDeLaNotificacion() {
        app.reloj.avanzar(java.time.Duration.ofHours(5));

        List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(evento(TipoEventoContratacion.CANCELACION, bruno, "Cambio de planes")));

        Notificacion interna = nuevas.get(0);
        assertFalse(interna.estaLeida());
        assertEquals(app.reloj.ahora(), interna.getFecha());
        assertTrue(interna.getMensaje().contains("#77"), interna.getMensaje());
        assertTrue(interna.getMensaje().contains("Cambio de planes"), "el motivo viaja en el mensaje");
    }

    @Test
    @DisplayName("los eventos con motivo (rechazo, cancelacion) lo incluyen; los demas no agregan parentesis vacios")
    void mensajesConYSinDetalle() {
        String rechazo = nuevasTras(() -> observer.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECHAZADA, ana, "No tengo lugar"))).get(0).getMensaje();
        String sinDetalle = nuevasTras(() -> observer.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECHAZADA, ana, " "))).get(0).getMensaje();

        assertTrue(rechazo.contains("No tengo lugar"));
        assertFalse(sinDetalle.contains("()"));
        assertFalse(sinDetalle.contains("("));
    }

    @Test
    @DisplayName("las notificaciones se persisten en su propia transaccion")
    void persisteEnSuTransaccion() {
        int antes = app.transaccion.ejecuciones();

        observer.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECIBIDA, bruno, "x"));

        assertEquals(antes + 1, app.transaccion.ejecuciones());
        assertFalse(app.transaccion.enTransaccion());
    }

    // ---------- tolerancia a fallas ----------

    @Test
    @DisplayName("si el destinatario ya no existe se omite la notificacion sin lanzar error")
    void destinatarioInexistente() {
        ContratacionEvent huerfano = new ContratacionEvent(TipoEventoContratacion.SOLICITUD_RECIBIDA, 1L, 5L, bruno.id(),
                9999L, bruno.id(), "x", app.reloj.ahora());
        Level nivel = silenciar(NotificadorObserver.class);
        try {
            List<Notificacion> nuevas = nuevasTras(() -> observer.actualizar(huerfano));
            assertTrue(nuevas.isEmpty());
        } finally {
            restaurar(NotificadorObserver.class, nivel);
        }
    }

    @Test
    @DisplayName("si el canal EMAIL falla, la notificacion INTERNA queda registrada y el error no se propaga")
    void canalExternoCaidoNoImpideLaInterna() {
        CanalNotificacionFactory emailCaido = canal -> {
            if (canal == CanalNotificacion.EMAIL) {
                throw new IllegalStateException("SMTP caido");
            }
            return canalesReales().crear(canal);
        };
        NotificadorObserver conFalla = observerCon(emailCaido);
        Level nivel = silenciar(NotificadorObserver.class);
        try {
            List<Notificacion> nuevas = nuevasTras(() -> conFalla.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECIBIDA, bruno, "x")));

            assertEquals(List.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL), nuevas.stream().map(Notificacion::getCanal).toList(),
                    "ambas quedan registradas aunque el envio externo fallo");
            assertTrue(app.emailSdk.enviados().isEmpty());
        } finally {
            restaurar(NotificadorObserver.class, nivel);
        }
    }

    @Test
    @DisplayName("si falla el canal interno igual se envia el canal preferido (un canal caido no bloquea al otro)")
    void canalInternoCaidoNoImpideElExterno() {
        CanalNotificacionFactory internaCaida = canal -> {
            if (canal == CanalNotificacion.INTERNA) {
                throw new IllegalStateException("bandeja caida");
            }
            return canalesReales().crear(canal);
        };
        NotificadorObserver conFalla = observerCon(internaCaida);
        Level nivel = silenciar(NotificadorObserver.class);
        try {
            conFalla.actualizar(evento(TipoEventoContratacion.SOLICITUD_RECIBIDA, bruno, "x"));

            assertEquals(1, app.emailSdk.enviados().size());
        } finally {
            restaurar(NotificadorObserver.class, nivel);
        }
    }

    @Test
    @DisplayName("un fallo del canal NO deshace la operacion de negocio: la solicitud queda creada y la bandeja interna la muestra")
    void fallaDelCanalNoDeshaceLaOperacion() {
        CanalNotificacionFactory todoCaido = canal -> {
            if (canal != CanalNotificacion.INTERNA) {
                throw new IllegalStateException("canal externo caido: " + canal);
            }
            return canalesReales().crear(canal);
        };
        ContratacionService servicio = new ContratacionService(app.contrataciones, app.publicaciones, app.perfiles,
                app.wallets, app.promociones, observerCon(todoCaido), app.transaccion, app.reloj);
        PublicacionDto publicada = e.publicada(ana);
        Level nivel = silenciar(NotificadorObserver.class);
        try {
            ContratacionDto solicitud = servicio.solicitar(bruno.id(), new SolicitarContratacionCommand(publicada.id(),
                    e.franjaId(publicada, 0), "Hola", "Aprender"));
            ContratacionDto aceptada = servicio.aceptar(ana.id(), solicitud.id());

            assertEquals("ACEPTADA", aceptada.estado());
            assertEquals("ACEPTADA", e.estado(bruno, solicitud).estado());
            assertTrue(e.bandeja(ana).stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.SOLICITUD_RECIBIDA));
            assertTrue(e.bandeja(bruno).stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.SOLICITUD_ACEPTADA));
        } finally {
            restaurar(NotificadorObserver.class, nivel);
        }
    }

    // ---------- de punta a punta con los servicios ----------

    @Test
    @DisplayName("en el flujo completo cada parte recibe solo los eventos que le corresponden, por su bandeja y su canal")
    void flujoCompletoNotificaACadaParte() {
        PublicacionDto publicada = e.publicada(ana);

        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);

        List<TipoEventoContratacion> deAna = e.bandeja(ana).stream().filter(n -> n.canal() == CanalNotificacion.INTERNA)
                .map(NotificacionDto::tipoEvento).toList();
        List<TipoEventoContratacion> deBruno = e.bandeja(bruno).stream().filter(n -> n.canal() == CanalNotificacion.INTERNA)
                .map(NotificacionDto::tipoEvento).toList();
        assertEquals(List.of(TipoEventoContratacion.PAGO_CONFIRMADO, TipoEventoContratacion.SOLICITUD_RECIBIDA), deAna);
        assertEquals(List.of(TipoEventoContratacion.SESION_COMPLETADA, TipoEventoContratacion.SOLICITUD_ACEPTADA), deBruno);
        assertEquals(2, app.emailSdk.enviados().size(), "Ana prefiere EMAIL: un correo por cada evento suyo");
        assertEquals(2, app.pushSdk.enviados().size(), "Bruno prefiere PUSH: un push por cada evento suyo");
        assertEquals("COMPLETADA", completada.estado());
    }

    @Test
    @DisplayName("cancelar y reprogramar notifican a la contraparte, por ambos canales si los tiene")
    void cancelacionYReprogramacion() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), e.franjaId(publicada, 1));
        app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true);
        app.contratacionUseCase.cancelar(ana.id(), aceptada.id(), "Imprevisto");

        List<NotificacionDto> deBruno = e.bandeja(bruno);
        assertTrue(deBruno.stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.REPROGRAMACION_ACEPTADA));
        assertTrue(deBruno.stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.CANCELACION && n.canal() == CanalNotificacion.PUSH));
        assertTrue(deBruno.stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.CANCELACION && n.canal() == CanalNotificacion.INTERNA));
        List<NotificacionDto> deAna = e.bandeja(ana);
        assertTrue(deAna.stream().anyMatch(n -> n.tipoEvento() == TipoEventoContratacion.REPROGRAMACION_PROPUESTA));
        assertTrue(deAna.stream().noneMatch(n -> n.tipoEvento() == TipoEventoContratacion.CANCELACION), "quien cancela no se auto-notifica");
    }
}
