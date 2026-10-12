package com.uade.skillswap.domain.model.contratacion;

import java.time.LocalDateTime;

/** Propuesta pendiente de mover la reserva a otra franja; la contraparte debe aceptarla. */
public final class PropuestaReprogramacion {

    private final Long nuevaFranjaId;
    private final Long propuestaPorId;
    private final LocalDateTime fecha;

    public PropuestaReprogramacion(Long nuevaFranjaId, Long propuestaPorId, LocalDateTime fecha) {
        this.nuevaFranjaId = nuevaFranjaId;
        this.propuestaPorId = propuestaPorId;
        this.fecha = fecha;
    }

    public Long getNuevaFranjaId() {
        return nuevaFranjaId;
    }

    public Long getPropuestaPorId() {
        return propuestaPorId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
