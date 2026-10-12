package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 8: enviar dos tipos de notificacion ante un mismo evento mediante adaptadores mock.
 * El evento es SOLICITUD_RECIBIDA (Observer sobre Contratacion). Por cada destinatario se guarda siempre la
 * notificacion INTERNA y, ademas, se envia por el canal preferido a traves del Adapter del SDK mock.
 */
@DisplayName("Caso 8 (memoria): dos tipos de notificacion ante un mismo evento con adaptadores mock")
class Caso08DosNotificacionesPorEventoTest {

    private static Set<CanalNotificacion> canales(List<NotificacionDto> notificaciones) {
        return notificaciones.stream().map(NotificacionDto::canal).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("Caso 8: proveedor con canal EMAIL recibe la INTERNA y un correo por el SDK de email")
    void caso08_internaMasEmailAnteElMismoEvento() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);

        e.solicitar(bruno.id(), mentoria, 0); // evento SOLICITUD_RECIBIDA para Ana

        // dos notificaciones persistidas para el mismo evento, de distinto canal
        List<NotificacionDto> deAna = e.app.notificacionUseCase.listar(ana.id(), false, e.primeraPagina()).contenido();
        assertEquals(2, deAna.size());
        assertEquals(Set.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL), canales(deAna));
        assertTrue(deAna.stream().allMatch(n -> n.tipoEvento() == TipoEventoContratacion.SOLICITUD_RECIBIDA));

        // y el adaptador de correo realmente envio algo al SDK mock
        List<EmailSdkMock.Correo> correos = e.app.emailSdk.enviados();
        assertEquals(1, correos.size());
        assertEquals("u-ana@test.local", correos.get(0).destino());
        assertTrue(correos.get(0).asunto().contains("SOLICITUD_RECIBIDA"), correos.get(0).asunto());
        assertTrue(correos.get(0).cuerpo().contains("nueva solicitud"), correos.get(0).cuerpo());
        assertTrue(e.app.pushSdk.enviados().isEmpty(), "no se uso el canal PUSH");
    }

    @Test
    @DisplayName("Caso 8: proveedor con canal PUSH recibe la INTERNA y un push por el SDK de push")
    void caso08_internaMasPushAnteElMismoEvento() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.PUSH);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);

        e.solicitar(bruno.id(), mentoria, 0);

        List<NotificacionDto> deAna = e.app.notificacionUseCase.listar(ana.id(), false, e.primeraPagina()).contenido();
        assertEquals(Set.of(CanalNotificacion.INTERNA, CanalNotificacion.PUSH), canales(deAna));

        List<PushSdkMock.Push> pushes = e.app.pushSdk.enviados();
        assertEquals(1, pushes.size());
        assertEquals("dispositivo-u-ana", pushes.get(0).dispositivo());
        assertEquals("SkillSwap", pushes.get(0).titulo());
        assertTrue(pushes.get(0).texto().contains("nueva solicitud"), pushes.get(0).texto());
        assertTrue(e.app.emailSdk.enviados().isEmpty(), "no se uso el canal EMAIL");
    }

    @Test
    @DisplayName("Caso 8: con canal preferido INTERNA no se envia nada por los SDK externos")
    void caso08_conCanalInternoSoloQuedaLaBandeja() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.INTERNA);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);

        e.solicitar(bruno.id(), mentoria, 0);

        List<NotificacionDto> deAna = e.app.notificacionUseCase.listar(ana.id(), false, e.primeraPagina()).contenido();
        assertEquals(1, deAna.size());
        assertEquals(CanalNotificacion.INTERNA, deAna.get(0).canal());
        assertTrue(e.app.emailSdk.enviados().isEmpty());
        assertTrue(e.app.pushSdk.enviados().isEmpty());
    }

    @Test
    @DisplayName("Caso 8: cada destinatario usa su propio canal: el cliente (PUSH) recibe la aceptacion por push")
    void caso08_cadaDestinatarioUsaSuPropioCanal() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno", CanalNotificacion.PUSH);
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);

        var solicitud = e.solicitar(bruno.id(), mentoria, 0);   // aviso a Ana por EMAIL
        e.app.contratacionUseCase.aceptar(ana.id(), solicitud.id()); // aviso a Bruno por PUSH

        assertEquals(1, e.app.emailSdk.enviados().size());
        assertEquals(1, e.app.pushSdk.enviados().size());
        assertEquals("dispositivo-u-bruno", e.app.pushSdk.enviados().get(0).dispositivo());
        assertTrue(e.app.pushSdk.enviados().get(0).texto().contains("aceptada"), e.app.pushSdk.enviados().get(0).texto());
    }
}
