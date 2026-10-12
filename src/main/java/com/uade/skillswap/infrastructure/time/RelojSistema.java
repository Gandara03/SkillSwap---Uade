package com.uade.skillswap.infrastructure.time;

import com.uade.skillswap.application.port.out.RelojPort;

import java.time.Clock;
import java.time.LocalDateTime;

/** Reloj real (zona del sistema por defecto); el Clock se puede inyectar para pruebas de integracion. */
public class RelojSistema implements RelojPort {

    private final Clock clock;

    public RelojSistema() {
        this(Clock.systemDefaultZone());
    }

    public RelojSistema(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
