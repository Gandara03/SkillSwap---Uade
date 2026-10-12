package com.uade.skillswap.domain.model.notificacion;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notificacion")
class NotificacionTest {

    @Test
    @DisplayName("nace no leida, con el canal y el evento indicados")
    void nace() {
        Notificacion n = Notificacion.crear(7L, TipoEventoContratacion.PAGO_CONFIRMADO, "pagaron", CanalNotificacion.PUSH, AHORA);
        assertNull(n.getId());
        assertEquals(7L, (long) n.getDestinatarioId());
        assertEquals(TipoEventoContratacion.PAGO_CONFIRMADO, n.getTipoEvento());
        assertEquals("pagaron", n.getMensaje());
        assertEquals(CanalNotificacion.PUSH, n.getCanal());
        assertFalse(n.estaLeida());
        assertEquals(AHORA, n.getFecha());
    }

    @Test
    @DisplayName("marcarLeida la deja leida y es idempotente")
    void marcarLeida() {
        Notificacion n = Notificacion.crear(7L, TipoEventoContratacion.CANCELACION, "m", CanalNotificacion.INTERNA, AHORA);
        n.marcarLeida();
        n.marcarLeida();
        assertTrue(n.estaLeida());
    }

    @Test
    @DisplayName("reconstruir conserva el flag leida")
    void reconstruir() {
        Notificacion n = Notificacion.reconstruir(1L, 7L, TipoEventoContratacion.CANCELACION, "m",
                CanalNotificacion.EMAIL, true, AHORA);
        assertTrue(n.estaLeida());
        assertEquals(1L, (long) n.getId());
    }
}
