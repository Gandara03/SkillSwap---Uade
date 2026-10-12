package com.uade.skillswap.domain.model.contratacion.estado;

/** CANCELADA: solo consultar historial. */
public class EstadoCancelada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "CANCELADA";
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
