package com.uade.skillswap.domain.query;

import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.RangoPrecio;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.math.BigDecimal;
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

    /**
     * Arma el rango de precios a partir de montos sueltos, para que quien filtra no tenga que conocer los value objects.
     * Cualquiera de los extremos puede ser null; si faltan los dos, devuelve null (sin filtro de precio).
     */
    public static RangoPrecio rangoEntre(BigDecimal minimo, BigDecimal maximo) {
        if (minimo == null && maximo == null) {
            return null;
        }
        return new RangoPrecio(minimo == null ? null : SkillCoins.de(minimo),
                maximo == null ? null : SkillCoins.de(maximo));
    }

    public CriterioOrden ordenEfectivo() {
        return orden == null ? CriterioOrden.RECIENTES : orden;
    }
}
