package com.uade.skillswap.domain.model.contratacion.observer;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;

import java.time.LocalDateTime;

/** Evento de dominio que se publica a los observadores cuando ocurre algo relevante en una contratacion. */
public final class ContratacionEvent {

    private final TipoEventoContratacion tipo;
    private final Long contratacionId;
    private final Long publicacionId;
    private final Long clienteId;
    private final Long proveedorId;
    private final Long actorId;
    private final String detalle;
    private final LocalDateTime fecha;

    public ContratacionEvent(TipoEventoContratacion tipo, Long contratacionId, Long publicacionId, Long clienteId,
                             Long proveedorId, Long actorId, String detalle, LocalDateTime fecha) {
        this.tipo = tipo;
        this.contratacionId = contratacionId;
        this.publicacionId = publicacionId;
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.actorId = actorId;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public TipoEventoContratacion getTipo() {
        return tipo;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
