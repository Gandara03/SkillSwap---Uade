package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

import java.util.Set;

/** ACEPTADA: pagar, cancelar o proponer reprogramacion. */
public class EstadoAceptada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "ACEPTADA";
    }

    @Override
    public void pagar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoConfirmada(), t);
    }

    @Override
    public void cancelar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoCancelada(), t.exigirMotivo());
    }

    @Override
    public Set<AccionContratacion> accionesPermitidas() {
        return Set.of(AccionContratacion.PAGAR, AccionContratacion.CANCELAR);
    }

    @Override
    public boolean permiteReprogramacion() {
        return true;
    }
}
