package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NotificacionService: bandeja propia, no leidas y marcar como leida")
class NotificacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;   // proveedora, prefiere EMAIL: cada evento le llega dos veces (INTERNA + EMAIL)
    private PerfilDto bruno; // cliente, solo bandeja interna
    private PublicacionDto publicada;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        bruno = e.profesional("u-bruno", "Bruno");
        publicada = e.publicada(ana);
    }

    @Test
    @DisplayName("cada perfil ve solo SU bandeja")
    void bandejaPropia() {
        e.solicitar(bruno, publicada, 0);

        List<NotificacionDto> deAna = e.bandeja(ana);
        assertEquals(2, deAna.size(), "INTERNA + EMAIL");
        assertTrue(deAna.stream().allMatch(n -> n.tipoEvento() == TipoEventoContratacion.SOLICITUD_RECIBIDA));
        assertTrue(e.bandeja(bruno).isEmpty());
    }

    @Test
    @DisplayName("la bandeja viene de la mas reciente a la mas antigua y se pagina")
    void ordenYPaginacion() {
        var solicitud = e.solicitar(bruno, publicada, 0);
        e.avanzarHoras(1);
        app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        e.avanzarHoras(1);
        app.contratacionUseCase.pagar(bruno.id(), solicitud.id());

        Pagina<NotificacionDto> primera = app.notificacionUseCase.listar(ana.id(), false, PaginaRequest.de(0, 3));
        Pagina<NotificacionDto> segunda = app.notificacionUseCase.listar(ana.id(), false, PaginaRequest.de(1, 3));

        assertEquals(4L, primera.totalElementos()); // 2 eventos x (INTERNA + EMAIL)
        assertEquals(3, primera.contenido().size());
        assertEquals(1, segunda.contenido().size());
        assertEquals(TipoEventoContratacion.PAGO_CONFIRMADO, primera.contenido().get(0).tipoEvento());
        assertEquals(TipoEventoContratacion.SOLICITUD_RECIBIDA, segunda.contenido().get(0).tipoEvento());
        assertTrue(primera.contenido().get(0).fecha().isAfter(segunda.contenido().get(0).fecha()));
    }

    @Test
    @DisplayName("marcarLeida deja la notificacion leida y deja de aparecer en 'solo no leidas'")
    void marcarLeida() {
        e.solicitar(bruno, publicada, 0);
        NotificacionDto primera = e.bandeja(ana).get(0);
        assertFalse(primera.leida());

        NotificacionDto leida = app.notificacionUseCase.marcarLeida(ana.id(), primera.id());

        assertTrue(leida.leida());
        assertEquals(primera.id(), leida.id());
        List<NotificacionDto> noLeidas = app.notificacionUseCase.listar(ana.id(), true, PaginaRequest.de(0, 10)).contenido();
        assertEquals(1, noLeidas.size());
        assertTrue(noLeidas.stream().noneMatch(n -> n.id().equals(primera.id())));
        assertEquals(2, e.bandeja(ana).size(), "sigue estando en la bandeja completa");
        assertTrue(app.notificacionUseCase.marcarLeida(ana.id(), primera.id()).leida(), "es idempotente");
    }

    @Test
    @DisplayName("NO se puede marcar como leida la notificacion de otro perfil: NoAutorizado y sigue sin leer")
    void marcarLeidaAjena() {
        e.solicitar(bruno, publicada, 0);
        NotificacionDto deAna = e.bandeja(ana).get(0);

        assertThrows(NoAutorizadoException.class, () -> app.notificacionUseCase.marcarLeida(bruno.id(), deAna.id()));

        assertFalse(e.bandeja(ana).stream().anyMatch(NotificacionDto::leida));
    }

    @Test
    @DisplayName("marcar una notificacion inexistente es RecursoNoEncontrado y los nulos son ValidacionException")
    void casosInvalidos() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.notificacionUseCase.marcarLeida(ana.id(), 999L));
        assertThrows(ValidacionException.class, () -> app.notificacionUseCase.marcarLeida(null, 1L));
        assertThrows(ValidacionException.class, () -> app.notificacionUseCase.marcarLeida(ana.id(), null));
        assertThrows(ValidacionException.class, () -> app.notificacionUseCase.listar(null, false, PaginaRequest.de(0, 10)));
        assertThrows(ValidacionException.class, () -> app.notificacionUseCase.listar(ana.id(), false, null));
    }

    @Test
    @DisplayName("una bandeja sin eventos esta vacia")
    void bandejaVacia() {
        assertTrue(e.bandeja(ana).isEmpty());
        assertEquals(0L, app.notificacionUseCase.listar(bruno.id(), true, PaginaRequest.de(0, 10)).totalElementos());
    }
}
