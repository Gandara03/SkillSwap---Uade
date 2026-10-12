package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Prioriza publicaciones mas contratadas (cupo ocupado) y de autores con mejor reputacion. */
public class RecomendacionPorPopularidad implements EstrategiaRecomendacion {

    @Override
    public TipoEstrategiaRecomendacion getTipo() {
        return TipoEstrategiaRecomendacion.POPULARIDAD;
    }

    @Override
    public List<Publicacion> recomendar(ContextoRecomendacion contexto, List<Publicacion> candidatas, int limite) {
        Perfil perfil = contexto.perfil();
        return candidatas.stream()
                .filter(p -> p.esVisiblePara(perfil.getId()) && !p.esAutor(perfil.getId()))
                .sorted(Comparator.<Publicacion>comparingInt(p -> -p.getCupoOcupado())
                        .thenComparing(p -> -contexto.reputacionDe(p.getAutorId()))
                        .thenComparing(Publicacion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(Math.max(limite, 0))
                .collect(Collectors.toList());
    }
}
