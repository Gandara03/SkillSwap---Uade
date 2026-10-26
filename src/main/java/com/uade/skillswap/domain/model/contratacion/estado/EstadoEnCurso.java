package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

import java.util.Set;

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

    @Override
    public Set<AccionContratacion> accionesPermitidas() {
        return Set.of(AccionContratacion.COMPLETAR);
    }
}
