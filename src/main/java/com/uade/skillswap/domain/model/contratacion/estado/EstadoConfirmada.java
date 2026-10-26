package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

import java.util.Set;

/** CONFIRMADA: iniciar, reprogramar o cancelar segun politica. */
public class EstadoConfirmada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "CONFIRMADA";
    }

    @Override
    public void iniciar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoEnCurso(), t);
    }

    @Override
    public void cancelar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoCancelada(), t.exigirMotivo());
    }

    @Override
    public Set<AccionContratacion> accionesPermitidas() {
        return Set.of(AccionContratacion.INICIAR, AccionContratacion.CANCELAR);
    }

    @Override
    public boolean permiteReprogramacion() {
        return true;
    }
}
