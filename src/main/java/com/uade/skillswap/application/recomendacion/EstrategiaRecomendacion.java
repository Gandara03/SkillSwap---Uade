package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.List;

/** Strategy: ordena publicaciones candidatas para un perfil. Intercambiable sin tocar el caso de uso. */
public interface EstrategiaRecomendacion {

    TipoEstrategiaRecomendacion getTipo();

    /** Devuelve a lo sumo {@code limite} publicaciones visibles, ya ordenadas por relevancia. */
    List<Publicacion> recomendar(DatosRecomendacion datos, List<Publicacion> candidatas, int limite);
}
