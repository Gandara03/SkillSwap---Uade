package com.uade.skillswap.domain.model.publicacion.estado;

import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.Set;

/** Rechaza por defecto toda accion; cada estado concreto habilita solo las suyas. */
public abstract class EstadoPublicacionBase implements EstadoPublicacion {

    @Override
    public void publicar(Publicacion p, ContextoTransicion t) {
        throw new TransicionInvalidaException("publicacion", getNombre(), "publicar");
    }

    @Override
    public void pausar(Publicacion p, ContextoTransicion t) {
        throw new TransicionInvalidaException("publicacion", getNombre(), "pausar");
    }

    @Override
    public void reanudar(Publicacion p, ContextoTransicion t) {
        throw new TransicionInvalidaException("publicacion", getNombre(), "reanudar");
    }

    @Override
    public void cerrar(Publicacion p, ContextoTransicion t) {
        throw new TransicionInvalidaException("publicacion", getNombre(), "cerrar");
    }

    @Override
    public void cancelar(Publicacion p, ContextoTransicion t) {
        throw new TransicionInvalidaException("publicacion", getNombre(), "cancelar");
    }

    @Override
    public Set<AccionPublicacion> accionesPermitidas() {
        return Set.of();
    }

    @Override
    public boolean permiteEdicion() {
        return false;
    }

    @Override
    public boolean admiteSolicitudes() {
        return false;
    }

    @Override
    public boolean esVisiblePublicamente() {
        return false;
    }

    @Override
    public boolean esTerminal() {
        return false;
    }
}
