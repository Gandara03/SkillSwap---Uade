package com.uade.skillswap.domain.model.contratacion.estado;

/** RECHAZADA: solo consultar historial. */
public class EstadoRechazada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "RECHAZADA";
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
