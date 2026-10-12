package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

/** EN_CURSO: completar. */
public class EstadoEnCurso extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "EN_CURSO";
    }

    @Override
    public void completar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoCompletada(), t);
    }
}
