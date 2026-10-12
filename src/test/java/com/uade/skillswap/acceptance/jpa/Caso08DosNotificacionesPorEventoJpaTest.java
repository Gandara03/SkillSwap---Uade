package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 8 (JPA/H2): dos tipos de notificacion ante un mismo evento mediante adaptadores mock.
 * Evento: SOLICITUD_RECIBIDA. El Observer persiste (en su propia transaccion) la notificacion INTERNA y la del
 * canal preferido, y el Adapter correspondiente envia al SDK mock, que queda inspeccionable.
 */
@DisplayName("Caso 8 (JPA/H2): dos tipos de notificacion ante un mismo evento con adaptadores mock")
class Caso08DosNotificacionesPorEventoJpaTest extends AceptacionJpaBase {

    private List<NotificacionDto> bandejaDe(String userId) {
        como(userId);
        return ok(app.getNotificacionController().listar(false, primeraPagina())).contenido();
    }

    private static Set<CanalNotificacion> canales(List<NotificacionDto> notificaciones) {
        return notificaciones.stream().map(NotificacionDto::canal).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Caso 8: proveedor con canal EMAIL recibe la INTERNA y un correo por el SDK de email")
    void caso08_internaMasEmailAnteElMismoEvento() {
        profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);

        solicitar("u-bruno", mentoria, 0); // evento SOLICITUD_RECIBIDA para Ana

        List<NotificacionDto> deAna = bandejaDe("u-ana");
        assertEquals(2, deAna.size());
        assertEquals(Set.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL), canales(deAna));
        assertTrue(deAna.stream().allMatch(n -> n.tipoEvento() == TipoEventoContratacion.SOLICITUD_RECIBIDA));

        List<EmailSdkMock.Correo> correos = app.getEmailSdk().enviados();
        assertEquals(1, correos.size());
        assertEquals("ana@skillswap.test", correos.get(0).destino()); // email del proveedor de identidad demo
        assertTrue(correos.get(0).asunto().contains("SOLICITUD_RECIBIDA"), correos.get(0).asunto());
        assertTrue(correos.get(0).cuerpo().contains("nueva solicitud"), correos.get(0).cuerpo());
        assertTrue(app.getPushSdk().enviados().isEmpty(), "no se uso el canal PUSH");
    }

    @Test
    @DisplayName("Caso 8: proveedor con canal PUSH recibe la INTERNA y un push por el SDK de push")
    void caso08_internaMasPushAnteElMismoEvento() {
        profesional("u-ana", "Ana", CanalNotificacion.PUSH);
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);

        solicitar("u-bruno", mentoria, 0);

        assertEquals(Set.of(CanalNotificacion.INTERNA, CanalNotificacion.PUSH), canales(bandejaDe("u-ana")));
        List<PushSdkMock.Push> pushes = app.getPushSdk().enviados();
        assertEquals(1, pushes.size());
        assertEquals("dispositivo-u-ana", pushes.get(0).dispositivo());
        assertTrue(pushes.get(0).texto().contains("nueva solicitud"), pushes.get(0).texto());
        assertTrue(app.getEmailSdk().enviados().isEmpty(), "no se uso el canal EMAIL");
    }

    @Test
    @DisplayName("Caso 8: con canal preferido INTERNA solo queda la bandeja; los SDK externos no se usan")
    void caso08_conCanalInternoSoloQuedaLaBandeja() {
        profesional("u-ana", "Ana", CanalNotificacion.INTERNA);
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);

        solicitar("u-bruno", mentoria, 0);

        List<NotificacionDto> deAna = bandejaDe("u-ana");
        assertEquals(1, deAna.size());
        assertEquals(CanalNotificacion.INTERNA, deAna.get(0).canal());
        assertTrue(app.getEmailSdk().enviados().isEmpty());
        assertTrue(app.getPushSdk().enviados().isEmpty());
    }
}
