package com.uade.skillswap.domain.exception;

/** Se intento una accion que el estado actual no permite. */
public class TransicionInvalidaException extends DominioException {

    private final String estadoActual;
    private final String accion;

    public TransicionInvalidaException(String entidad, String estadoActual, String accion) {
        super("No se puede " + accion + " una " + entidad + " en estado " + estadoActual);
        this.estadoActual = estadoActual;
        this.accion = accion;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public String getAccion() {
        return accion;
    }
}
