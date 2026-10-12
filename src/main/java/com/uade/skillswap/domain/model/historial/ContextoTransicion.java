package com.uade.skillswap.domain.model.historial;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.time.LocalDateTime;

/** Datos de quien, cuando y por que se ejecuta una transicion de estado. */
public final class ContextoTransicion {

    private final Long actorId;
    private final String motivo;
    private final LocalDateTime fecha;

    public ContextoTransicion(Long actorId, String motivo, LocalDateTime fecha) {
        if (actorId == null) {
            throw new ReglaDeNegocioException("El actor de la transicion es obligatorio");
        }
        if (fecha == null) {
            throw new ReglaDeNegocioException("La fecha de la transicion es obligatoria");
        }
        this.actorId = actorId;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    /** Falla si no hay un motivo informado (cancelaciones y rechazos lo exigen). */
    public ContextoTransicion exigirMotivo() {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("El motivo es obligatorio");
        }
        return this;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
