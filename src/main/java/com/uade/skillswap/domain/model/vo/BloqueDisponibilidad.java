package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Value Object: bloque semanal en el que un perfil declara estar disponible. */
public final class BloqueDisponibilidad {

    private final DayOfWeek dia;
    private final LocalTime desde;
    private final LocalTime hasta;

    public BloqueDisponibilidad(DayOfWeek dia, LocalTime desde, LocalTime hasta) {
        if (dia == null || desde == null || hasta == null) {
            throw new ReglaDeNegocioException("El bloque de disponibilidad requiere dia, desde y hasta");
        }
        if (!desde.isBefore(hasta)) {
            throw new ReglaDeNegocioException("En la disponibilidad, 'desde' debe ser anterior a 'hasta'");
        }
        this.dia = dia;
        this.desde = desde;
        this.hasta = hasta;
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public LocalTime getDesde() {
        return desde;
    }

    public LocalTime getHasta() {
        return hasta;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BloqueDisponibilidad b && b.dia == dia && b.desde.equals(desde) && b.hasta.equals(hasta);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dia, desde, hasta);
    }
}
