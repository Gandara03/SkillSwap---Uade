package com.uade.skillswap.domain.exception;

/** La publicacion ya no tiene cupo disponible. */
public class CupoAgotadoException extends DominioException {

    public CupoAgotadoException(String mensaje) {
        super(mensaje);
    }
}
