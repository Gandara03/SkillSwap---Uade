package com.uade.skillswap.domain.model.publicacion.estado;

import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

/**
 * State Pattern - State: define que puede hacerse con una Publicacion en cada estado.
 * Las acciones invalidas lanzan TransicionInvalidaException (ver EstadoPublicacionBase).
 */
public interface EstadoPublicacion {

    String getNombre();

    void publicar(Publicacion publicacion, ContextoTransicion t);

    void pausar(Publicacion publicacion, ContextoTransicion t);

    void reanudar(Publicacion publicacion, ContextoTransicion t);

    void cerrar(Publicacion publicacion, ContextoTransicion t);

    void cancelar(Publicacion publicacion, ContextoTransicion t);

    boolean permiteEdicion();

    boolean admiteSolicitudes();

    boolean esVisiblePublicamente();

    boolean esTerminal();
}
