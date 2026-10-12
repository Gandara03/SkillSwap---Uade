package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

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
}
