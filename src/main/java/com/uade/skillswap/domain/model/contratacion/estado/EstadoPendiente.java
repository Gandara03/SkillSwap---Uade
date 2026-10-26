package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

import java.util.Set;

/** PENDIENTE: aceptar o rechazar. */
public class EstadoPendiente extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "PENDIENTE";
    }

    @Override
    public void aceptar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoAceptada(), t);
    }

    @Override
    public void rechazar(Contratacion c, ContextoTransicion t) {
        c.cambiarEstado(new EstadoRechazada(), t.exigirMotivo());
    }

    @Override
    public Set<AccionContratacion> accionesPermitidas() {
        return Set.of(AccionContratacion.ACEPTAR, AccionContratacion.RECHAZAR);
    }
}
