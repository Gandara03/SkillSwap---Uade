package com.uade.skillswap.domain.model.publicacion.estado;

/** CANCELADA: retirada con motivo obligatorio. */
public class EstadoCancelada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "CANCELADA";
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
