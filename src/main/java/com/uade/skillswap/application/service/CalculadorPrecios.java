package com.uade.skillswap.application.service;

import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.precio.PrecioPublicacion;
import com.uade.skillswap.application.precio.PrecioPublicacionFactory;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.application.port.out.PromocionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el precio de una publicacion con sus promociones vigentes (Decorator via PrecioPublicacionFactory).
 * Para listados usa {@link #paraTodas} (una consulta de promociones por lote, sin N+1).
 */
final class CalculadorPrecios {

    private final PromocionRepository promocionRepository;
    private final RelojPort reloj;

    CalculadorPrecios(PromocionRepository promocionRepository, RelojPort reloj) {
        this.promocionRepository = promocionRepository;
        this.reloj = reloj;
    }

    PrecioPublicacion para(Publicacion publicacion) {
        return para(publicacion, reloj.ahora());
    }

    /** @return el precio con promociones, o null si la publicacion (borrador) todavia no tiene precio base. */
    PrecioPublicacion para(Publicacion publicacion, LocalDateTime ahora) {
        if (publicacion.getPrecioBase() == null) {
            return null;
        }
        return PrecioPublicacionFactory.construir(publicacion.getPrecioBase(),
                promocionRepository.buscarActivasPorPublicacion(publicacion.getId()), ahora);
    }

    /** Precios de varias publicaciones con una sola consulta de promociones (null donde no hay precio base). */
    Map<Long, PrecioPublicacion> paraTodas(List<Publicacion> publicaciones) {
        LocalDateTime ahora = reloj.ahora();
        List<Long> ids = publicaciones.stream().map(Publicacion::getId).toList();
        Map<Long, List<com.uade.skillswap.domain.model.promocion.Promocion>> promos =
                ids.isEmpty() ? Map.of() : promocionRepository.buscarActivasPorPublicaciones(ids);
        Map<Long, PrecioPublicacion> resultado = new HashMap<>();
        for (Publicacion p : publicaciones) {
            resultado.put(p.getId(), p.getPrecioBase() == null ? null
                    : PrecioPublicacionFactory.construir(p.getPrecioBase(), promos.getOrDefault(p.getId(), List.of()), ahora));
        }
        return resultado;
    }
}
