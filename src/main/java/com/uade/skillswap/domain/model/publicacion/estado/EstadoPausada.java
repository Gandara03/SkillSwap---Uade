package com.uade.skillswap.domain.model.publicacion.estado;

import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.Set;

/** PAUSADA: visible para su autor, pero no admite nuevas solicitudes. */
public class EstadoPausada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "PAUSADA";
    }

    @Override
    public void reanudar(Publicacion p, ContextoTransicion t) {
        p.cambiarEstado(new EstadoPublicada(), t);
    }

    @Override
    public void cerrar(Publicacion p, ContextoTransicion t) {
        p.cambiarEstado(new EstadoCerrada(), t);
    }

    @Override
    public void cancelar(Publicacion p, ContextoTransicion t) {
        p.cambiarEstado(new EstadoCancelada(), t.exigirMotivo());
    }

    @Override
    public Set<AccionPublicacion> accionesPermitidas() {
        return Set.of(AccionPublicacion.REANUDAR, AccionPublicacion.CERRAR, AccionPublicacion.CANCELAR);
    }
}
