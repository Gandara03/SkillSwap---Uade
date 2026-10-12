package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;

import java.util.Map;

/** Datos que las estrategias necesitan: el perfil y la reputacion de los autores de las candidatas. */
public record ContextoRecomendacion(Perfil perfil, Map<Long, Double> reputacionAutores) {

    public ContextoRecomendacion {
        reputacionAutores = reputacionAutores == null ? Map.of() : Map.copyOf(reputacionAutores);
    }

    public double reputacionDe(Long autorId) {
        return reputacionAutores.getOrDefault(autorId, 0d);
    }
}
