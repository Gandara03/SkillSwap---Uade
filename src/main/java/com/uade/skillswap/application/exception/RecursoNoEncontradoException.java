package com.uade.skillswap.application.exception;

public class RecursoNoEncontradoException extends AplicacionException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(ErrorCode.NO_ENCONTRADO, recurso + " no encontrado: " + id);
    }
}
