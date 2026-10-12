package com.uade.skillswap.support;

import com.uade.skillswap.application.port.out.RelojPort;

import java.time.Duration;
import java.time.LocalDateTime;

/** Reloj controlable para tests deterministas. */
public class RelojFijo implements RelojPort {

    public static final LocalDateTime INICIO_POR_DEFECTO = LocalDateTime.of(2026, 3, 2, 10, 0);

    private LocalDateTime ahora;

    public RelojFijo() {
        this(INICIO_POR_DEFECTO);
    }

    public RelojFijo(LocalDateTime ahora) {
        this.ahora = ahora;
    }

    @Override
    public LocalDateTime ahora() {
        return ahora;
    }

    public void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    public void fijar(LocalDateTime nuevo) {
        ahora = nuevo;
    }
}
