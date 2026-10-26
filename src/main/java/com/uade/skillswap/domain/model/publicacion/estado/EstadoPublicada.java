package com.uade.skillswap.domain.model.publicacion.estado;

import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.Set;

/** PUBLICADA: visible y disponible para recibir solicitudes. */
public class EstadoPublicada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "PUBLICADA";
    }

    @Override
    public void pausar(Publicacion p, ContextoTransicion t) {
        p.cambiarEstado(new EstadoPausada(), t);
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
        return Set.of(AccionPublicacion.PAUSAR, AccionPublicacion.CERRAR, AccionPublicacion.CANCELAR);
    }

    @Override
    public boolean admiteSolicitudes() {
        return true;
    }

    @Override
    public boolean esVisiblePublicamente() {
        return true;
    }
}
