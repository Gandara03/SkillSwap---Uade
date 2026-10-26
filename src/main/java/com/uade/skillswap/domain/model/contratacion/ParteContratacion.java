package com.uade.skillswap.domain.model.contratacion;

/** Quien de los participantes de una contratacion puede ejecutar una accion. */
public enum ParteContratacion {
    PROVEEDOR("Solo el proveedor puede realizar esta accion"),
    CLIENTE("Solo el cliente puede realizar esta accion"),
    AMBAS("Solo los participantes de la contratacion pueden realizar esta accion");

    private final String mensajeSinPermiso;

    ParteContratacion(String mensajeSinPermiso) {
        this.mensajeSinPermiso = mensajeSinPermiso;
    }

    public String getMensajeSinPermiso() {
        return mensajeSinPermiso;
    }
}
