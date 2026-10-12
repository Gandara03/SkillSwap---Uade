package com.uade.skillswap.domain.query;

import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.RangoPrecio;

import java.time.LocalDateTime;

/** Filtros combinables de busqueda. Todos opcionales (null = sin filtrar). Solo se buscan publicaciones PUBLICADAS. */
public record FiltroBusqueda(
        String texto,
        String categoria,
        TipoPublicacion tipo,
        Modalidad modalidad,
        RangoPrecio rangoPrecio,
        String habilidad,
        String etiqueta,
        Double reputacionMinima,
        LocalDateTime disponibleDesde,
        LocalDateTime disponibleHasta,
        CriterioOrden orden) {

    public static FiltroBusqueda vacio() {
        return new FiltroBusqueda(null, null, null, null, null, null, null, null, null, null, CriterioOrden.RECIENTES);
    }

    public CriterioOrden ordenEfectivo() {
        return orden == null ? CriterioOrden.RECIENTES : orden;
    }
}
