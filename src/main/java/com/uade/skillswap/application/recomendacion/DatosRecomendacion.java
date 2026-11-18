package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;

import java.util.Map;

/**
 * Datos que las estrategias necesitan: el perfil, la reputacion de los autores de las candidatas y cuantas
 * contrataciones pagadas tuvo cada candidata en la ventana reciente.
 */
public record DatosRecomendacion(Perfil perfil, Map<Long, Double> reputacionAutores,
                                 Map<Long, Long> pagadasRecientes) {

    public DatosRecomendacion {
        reputacionAutores = reputacionAutores == null ? Map.of() : Map.copyOf(reputacionAutores);
        pagadasRecientes = pagadasRecientes == null ? Map.of() : Map.copyOf(pagadasRecientes);
    }

    public double reputacionDe(Long autorId) {
        return reputacionAutores.getOrDefault(autorId, 0d);
    }

    public long pagadasRecientesDe(Long publicacionId) {
        return pagadasRecientes.getOrDefault(publicacionId, 0L);
    }
}
