package com.uade.skillswap.domain.model.publicacion.estado;

/** Simple Factory: reconstruye el objeto State a partir del nombre persistido. */
public final class EstadoPublicacionFactory {

    private EstadoPublicacionFactory() {
    }

    public static EstadoPublicacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoBorrador();
        }
        return switch (nombre) {
            case "BORRADOR" -> new EstadoBorrador();
            case "PUBLICADA" -> new EstadoPublicada();
            case "PAUSADA" -> new EstadoPausada();
            case "CERRADA" -> new EstadoCerrada();
            case "CANCELADA" -> new EstadoCancelada();
            default -> throw new IllegalArgumentException("Estado de publicacion desconocido: " + nombre);
        };
    }
}
