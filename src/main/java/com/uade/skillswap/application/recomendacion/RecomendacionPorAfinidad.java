package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Prioriza publicaciones que coinciden con los intereses del perfil (habilidades, etiquetas, categoria). */
public class RecomendacionPorAfinidad implements EstrategiaRecomendacion {

    @Override
    public TipoEstrategiaRecomendacion getTipo() {
        return TipoEstrategiaRecomendacion.AFINIDAD;
    }

    @Override
    public List<Publicacion> recomendar(DatosRecomendacion datos, List<Publicacion> candidatas, int limite) {
        Perfil perfil = datos.perfil();
        Set<String> intereses = perfil.getIntereses().stream()
                .map(i -> i.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return candidatas.stream()
                .filter(p -> p.esVisiblePara(perfil.getId()) && !p.esAutor(perfil.getId()))
                .map(p -> new Puntuada(p, afinidad(p, intereses)))
                .filter(pp -> pp.puntos > 0)
                .sorted(Comparator.<Puntuada>comparingInt(pp -> -pp.puntos)
                        .thenComparing(pp -> -datos.reputacionDe(pp.publicacion.getAutorId()))
                        .thenComparing(pp -> pp.publicacion.getId(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(Math.max(limite, 0))
                .map(pp -> pp.publicacion)
                .collect(Collectors.toList());
    }

    private int afinidad(Publicacion p, Set<String> intereses) {
        int puntos = 0;
        for (Habilidad h : p.getHabilidades()) {
            if (intereses.contains(h.getNombre())) {
                puntos += 2;
            }
        }
        for (String etiqueta : p.getEtiquetas()) {
            if (intereses.contains(etiqueta.trim().toLowerCase(Locale.ROOT))) {
                puntos += 1;
            }
        }
        if (p.getCategoria() != null && intereses.contains(p.getCategoria().trim().toLowerCase(Locale.ROOT))) {
            puntos += 1;
        }
        return puntos;
    }

    private record Puntuada(Publicacion publicacion, int puntos) { }
}
