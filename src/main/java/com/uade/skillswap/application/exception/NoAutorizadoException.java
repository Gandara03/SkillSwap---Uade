package com.uade.skillswap.application.exception;

public class NoAutorizadoException extends AplicacionException {
    public NoAutorizadoException(String mensaje) {
        super(ErrorCode.NO_AUTORIZADO, mensaje);
    }
}
