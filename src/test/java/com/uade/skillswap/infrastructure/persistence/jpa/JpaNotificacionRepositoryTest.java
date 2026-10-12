package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaNotificacionRepositoryTest extends JpaTestBase {

    private Perfil ana;
    private Perfil bruno;

    @BeforeEach
    void crearPerfiles() {
        ana = profesional("u-ana");
        bruno = profesional("u-bruno");
    }

    private Notificacion nueva(Perfil destinatario, CanalNotificacion canal, int minutos) {
        return notificaciones.guardar(Notificacion.crear(destinatario.getId(), TipoEventoContratacion.SOLICITUD_RECIBIDA,
                "Mensaje " + minutos, canal, AHORA.plusMinutes(minutos)));
    }

    @Test
    @DisplayName("guardar y recuperar una notificacion")
    void guardarYRecuperar() {
        Notificacion guardada = nueva(ana, CanalNotificacion.EMAIL, 5);

        assertNotNull(guardada.getId());
        Notificacion leida = notificaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(ana.getId(), leida.getDestinatarioId());
        assertEquals(TipoEventoContratacion.SOLICITUD_RECIBIDA, leida.getTipoEvento());
        assertEquals("Mensaje 5", leida.getMensaje());
        assertEquals(CanalNotificacion.EMAIL, leida.getCanal());
        assertFalse(leida.estaLeida());
        assertEquals(AHORA.plusMinutes(5), leida.getFecha());
        assertTrue(notificaciones.buscarPorId(9999L).isEmpty());
        assertTrue(notificaciones.buscarPorId(null).isEmpty());
    }

    @Test
    @DisplayName("marcar como leida se persiste sobre la misma fila")
    void marcarLeida() {
        Notificacion guardada = nueva(ana, CanalNotificacion.INTERNA, 1);

        Notificacion editable = notificaciones.buscarPorId(guardada.getId()).orElseThrow();
        editable.marcarLeida();
        notificaciones.guardar(editable);

        assertTrue(notificaciones.buscarPorId(guardada.getId()).orElseThrow().estaLeida());
        assertEquals(1, notificaciones.buscarPorDestinatario(ana.getId(), false, PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("un mismo evento puede generar dos notificaciones por canales distintos")
    void dosCanalesParaUnMismoEvento() {
        Notificacion interna = nueva(ana, CanalNotificacion.INTERNA, 1);
        Notificacion email = nueva(ana, CanalNotificacion.EMAIL, 1);

        Pagina<Notificacion> bandeja = notificaciones.buscarPorDestinatario(ana.getId(), false, PaginaRequest.de(0, 10));

        assertEquals(2, bandeja.totalElementos());
        assertIterableEquals(List.of(email.getId(), interna.getId()), bandeja.contenido().stream().map(Notificacion::getId).toList());
        assertEquals(CanalNotificacion.EMAIL, bandeja.contenido().get(0).getCanal());
    }

    @Test
    @DisplayName("la bandeja filtra por destinatario y por no leidas, de la mas reciente a la mas antigua, paginada")
    void bandeja() {
        Notificacion n1 = nueva(ana, CanalNotificacion.INTERNA, 1);
        Notificacion n2 = nueva(ana, CanalNotificacion.INTERNA, 2);
        Notificacion n3 = nueva(ana, CanalNotificacion.INTERNA, 3);
        Notificacion n4 = nueva(ana, CanalNotificacion.INTERNA, 4);
        nueva(bruno, CanalNotificacion.INTERNA, 5);
        for (Notificacion leida : List.of(n2, n4)) {
            Notificacion editable = notificaciones.buscarPorId(leida.getId()).orElseThrow();
            editable.marcarLeida();
            notificaciones.guardar(editable);
        }

        Pagina<Notificacion> todas = notificaciones.buscarPorDestinatario(ana.getId(), false, PaginaRequest.de(0, 10));
        Pagina<Notificacion> noLeidas = notificaciones.buscarPorDestinatario(ana.getId(), true, PaginaRequest.de(0, 10));
        Pagina<Notificacion> primeraDeDos = notificaciones.buscarPorDestinatario(ana.getId(), false, PaginaRequest.de(0, 2));
        Pagina<Notificacion> segundaDeDos = notificaciones.buscarPorDestinatario(ana.getId(), false, PaginaRequest.de(1, 2));

        assertEquals(4, todas.totalElementos());
        assertIterableEquals(List.of(n4.getId(), n3.getId(), n2.getId(), n1.getId()),
                todas.contenido().stream().map(Notificacion::getId).toList());
        assertEquals(2, noLeidas.totalElementos());
        assertIterableEquals(List.of(n3.getId(), n1.getId()), noLeidas.contenido().stream().map(Notificacion::getId).toList());
        assertIterableEquals(List.of(n4.getId(), n3.getId()), primeraDeDos.contenido().stream().map(Notificacion::getId).toList());
        assertIterableEquals(List.of(n2.getId(), n1.getId()), segundaDeDos.contenido().stream().map(Notificacion::getId).toList());
        assertEquals(1, notificaciones.buscarPorDestinatario(bruno.getId(), true, PaginaRequest.de(0, 10)).totalElementos());
    }
}
