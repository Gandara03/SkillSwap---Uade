package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Las columnas son DATETIME(6) (microsegundos). Si el dominio trae nanosegundos, la base los redondea o trunca
 * segun el motor y Hibernate veria el valor como "modificado" en cada guardado (UPDATE y version de mas).
 * Se trunca a microsegundos antes de escribir para que el valor leido sea siempre igual al escrito.
 */
final class Fechas {

    private Fechas() {
    }

    static LocalDateTime enMicros(LocalDateTime fecha) {
        return fecha == null ? null : fecha.truncatedTo(ChronoUnit.MICROS);
    }
}
