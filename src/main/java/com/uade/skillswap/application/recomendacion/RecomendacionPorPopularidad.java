package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prioriza las publicaciones con mas contrataciones pagadas en la ventana reciente y, a igual cantidad, las de
 * autores con mejor reputacion.
 */
public class RecomendacionPorPopularidad implements EstrategiaRecomendacion {

    @Override
    public TipoEstrategiaRecomendacion getTipo() {
        return TipoEstrategiaRecomendacion.POPULARIDAD;
    }

    @Override
    public List<Publicacion> recomendar(DatosRecomendacion datos, List<Publicacion> candidatas, int limite) {
        Perfil perfil = datos.perfil();
        return candidatas.stream()
                .filter(p -> p.esVisiblePara(perfil.getId()) && !p.esAutor(perfil.getId()))
                .sorted(Comparator.<Publicacion>comparingLong(p -> -datos.pagadasRecientesDe(p.getId()))
                        .thenComparing(p -> -datos.reputacionDe(p.getAutorId()))
                        .thenComparing(Publicacion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(Math.max(limite, 0))
                .collect(Collectors.toList());
    }
}
