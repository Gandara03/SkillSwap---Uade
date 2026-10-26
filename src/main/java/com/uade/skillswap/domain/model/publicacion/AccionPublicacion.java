package com.uade.skillswap.domain.model.publicacion;

/**
 * Acciones de cambio de estado que el autor puede pedir sobre una publicacion.
 * Cada estado (State) declara cuales admite.
 */
public enum AccionPublicacion {
    PUBLICAR,
    PAUSAR,
    REANUDAR,
    CERRAR,
    CANCELAR
}
