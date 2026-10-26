package com.uade.skillswap.domain.model.contratacion;

/**
 * Acciones que se pueden pedir sobre una contratacion y la parte que puede ejecutar cada una.
 * Cada estado (State) declara cuales admite; Contratacion cruza las dos cosas para un actor concreto.
 */
public enum AccionContratacion {
    ACEPTAR(ParteContratacion.PROVEEDOR),
    RECHAZAR(ParteContratacion.PROVEEDOR),
    PAGAR(ParteContratacion.CLIENTE),
    INICIAR(ParteContratacion.PROVEEDOR),
    COMPLETAR(ParteContratacion.PROVEEDOR),
    CANCELAR(ParteContratacion.AMBAS),
    CALIFICAR(ParteContratacion.AMBAS);

    private final ParteContratacion parte;

    AccionContratacion(ParteContratacion parte) {
        this.parte = parte;
    }

    public ParteContratacion getParte() {
        return parte;
    }
}
