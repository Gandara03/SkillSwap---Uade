package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;

import java.util.Set;

/** COMPLETADA: calificar y consultar comprobante. No admite mas transiciones. */
public class EstadoCompletada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "COMPLETADA";
    }

    /** Calificar no cambia el estado: la registra Calificacion, que exige una contratacion COMPLETADA. */
    @Override
    public Set<AccionContratacion> accionesPermitidas() {
        return Set.of(AccionContratacion.CALIFICAR);
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
