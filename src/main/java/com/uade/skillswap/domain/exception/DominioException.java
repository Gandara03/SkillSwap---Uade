package com.uade.skillswap.domain.exception;

/** Base de todos los errores de reglas del dominio. */
public class DominioException extends RuntimeException {

    public DominioException(String mensaje) {
        super(mensaje);
    }
}
