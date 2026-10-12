package com.uade.skillswap.domain.model.contratacion.estado;

/** COMPLETADA: calificar y consultar comprobante. No admite mas transiciones. */
public class EstadoCompletada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "COMPLETADA";
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
