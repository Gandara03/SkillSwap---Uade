package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.List;

/**
 * Strategy: ordena publicaciones candidatas para un perfil. Intercambiable sin tocar el caso de uso.
 * Vive en application y no en domain: no es una regla de negocio sino un criterio para ordenar un listado,
 * que combina datos de varios agregados que arma el caso de uso (ver {@link ContextoRecomendacion}).
 */
public interface EstrategiaRecomendacion {

    TipoEstrategiaRecomendacion getTipo();

    /** Devuelve a lo sumo {@code limite} publicaciones visibles, ya ordenadas por relevancia. */
    List<Publicacion> recomendar(ContextoRecomendacion contexto, List<Publicacion> candidatas, int limite);
}
