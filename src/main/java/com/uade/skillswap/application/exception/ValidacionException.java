package com.uade.skillswap.application.exception;

public class ValidacionException extends AplicacionException {
    public ValidacionException(String mensaje) {
        super(ErrorCode.VALIDACION, mensaje);
    }
}
