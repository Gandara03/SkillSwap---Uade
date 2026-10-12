package com.uade.skillswap.domain.exception;

/** La franja pedida no existe, ya paso o esta ocupada. */
public class FranjaNoDisponibleException extends DominioException {

    public FranjaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
