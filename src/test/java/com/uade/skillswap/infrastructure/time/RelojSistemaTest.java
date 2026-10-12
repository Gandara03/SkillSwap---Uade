package com.uade.skillswap.infrastructure.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RelojSistema")
class RelojSistemaTest {

    @Test
    @DisplayName("devuelve la hora actual del sistema")
    void horaActual() {
        LocalDateTime antes = LocalDateTime.now();
        LocalDateTime medida = new RelojSistema().ahora();
        LocalDateTime despues = LocalDateTime.now();
        assertFalse(medida.isBefore(antes));
        assertFalse(medida.isAfter(despues));
    }

    @Test
    @DisplayName("respeta el Clock inyectado")
    void clockInyectado() {
        Clock fijo = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
        assertEquals(LocalDateTime.of(2026, 1, 1, 12, 0), new RelojSistema(fijo).ahora());
    }
}
