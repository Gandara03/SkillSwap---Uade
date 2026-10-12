package com.uade.skillswap.domain.model.historial;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContextoTransicion e HistorialEstado")
class HistorialYContextoTest {

    @Test
    @DisplayName("el contexto exige actor y fecha")
    void contextoObligatorios() {
        assertThrows(ReglaDeNegocioException.class, () -> new ContextoTransicion(null, "m", AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> new ContextoTransicion(1L, "m", null));
    }

    @Test
    @DisplayName("exigirMotivo falla con motivo nulo o en blanco y devuelve el mismo contexto si hay motivo")
    void exigirMotivo() {
        assertThrows(ReglaDeNegocioException.class, () -> new ContextoTransicion(1L, null, AHORA).exigirMotivo());
        assertThrows(ReglaDeNegocioException.class, () -> new ContextoTransicion(1L, "   ", AHORA).exigirMotivo());
        ContextoTransicion ok = new ContextoTransicion(1L, "porque si", AHORA);
        assertSame(ok, ok.exigirMotivo());
    }

    @Test
    @DisplayName("registrar copia actor, motivo y fecha del contexto")
    void registrar() {
        HistorialEstado h = HistorialEstado.registrar("A", "B", new ContextoTransicion(4L, "motivo", AHORA));
        assertNull(h.getId());
        assertEquals("A", h.getEstadoAnterior());
        assertEquals("B", h.getEstadoNuevo());
        assertEquals(4L, (long) h.getActorId());
        assertEquals("motivo", h.getMotivo());
        assertEquals(AHORA, h.getFecha());
    }

    @Test
    @DisplayName("reconstruir conserva el id")
    void reconstruir() {
        HistorialEstado h = HistorialEstado.reconstruir(9L, "A", "B", 1L, null, AHORA);
        assertEquals(9L, (long) h.getId());
        assertNull(h.getMotivo());
    }
}
