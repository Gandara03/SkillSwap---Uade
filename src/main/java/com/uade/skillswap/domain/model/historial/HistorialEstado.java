package com.uade.skillswap.domain.model.historial;

import java.time.LocalDateTime;

/** Registro inmutable de una transicion de estado (fecha, actor y motivo). */
public final class HistorialEstado {

    private final Long id;
    private final String estadoAnterior;
    private final String estadoNuevo;
    private final Long actorId;
    private final String motivo;
    private final LocalDateTime fecha;

    private HistorialEstado(Long id, String estadoAnterior, String estadoNuevo, Long actorId,
                            String motivo, LocalDateTime fecha) {
        this.id = id;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actorId = actorId;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public static HistorialEstado registrar(String estadoAnterior, String estadoNuevo, ContextoTransicion t) {
        return new HistorialEstado(null, estadoAnterior, estadoNuevo, t.getActorId(), t.getMotivo(), t.getFecha());
    }

    /** Solo para rehidratar desde persistencia. */
    public static HistorialEstado reconstruir(Long id, String estadoAnterior, String estadoNuevo, Long actorId,
                                              String motivo, LocalDateTime fecha) {
        return new HistorialEstado(id, estadoAnterior, estadoNuevo, actorId, motivo, fecha);
    }

    public Long getId() {
        return id;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
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
