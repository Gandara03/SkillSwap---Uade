package com.uade.skillswap.domain.model.contratacion.estado;

/** Simple Factory: reconstruye el objeto State a partir del nombre persistido. */
public final class EstadoContratacionFactory {

    private EstadoContratacionFactory() {
    }

    public static EstadoContratacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoPendiente();
        }
        return switch (nombre) {
            case "PENDIENTE" -> new EstadoPendiente();
            case "ACEPTADA" -> new EstadoAceptada();
            case "CONFIRMADA" -> new EstadoConfirmada();
            case "EN_CURSO" -> new EstadoEnCurso();
            case "COMPLETADA" -> new EstadoCompletada();
            case "RECHAZADA" -> new EstadoRechazada();
            case "CANCELADA" -> new EstadoCancelada();
            default -> throw new IllegalArgumentException("Estado de contratacion desconocido: " + nombre);
        };
    }
}
