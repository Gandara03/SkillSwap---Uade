package com.uade.skillswap.application.exception;

/** Base de los errores de la capa de aplicacion. */
public class AplicacionException extends RuntimeException {

    private final ErrorCode codigo;

    public AplicacionException(ErrorCode codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public AplicacionException(ErrorCode codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    public ErrorCode getCodigo() {
        return codigo;
    }
}
