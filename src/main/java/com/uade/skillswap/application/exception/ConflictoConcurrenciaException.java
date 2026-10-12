package com.uade.skillswap.application.exception;

/** Otro usuario modifico el mismo recurso (optimistic locking). El usuario puede reintentar. */
public class ConflictoConcurrenciaException extends AplicacionException {
    public ConflictoConcurrenciaException(String mensaje, Throwable causa) {
        super(ErrorCode.CONFLICTO_CONCURRENCIA, mensaje, causa);
    }
}
