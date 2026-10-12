package com.uade.skillswap.application.service;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.application.port.out.PerfilRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Convierte publicaciones en filas de listado cargando todos los autores con UNA consulta (sin N+1). */
final class ArmadorResumenes {

    private final PerfilRepository perfilRepository;
    private final CalculadorPrecios precios;

    ArmadorResumenes(PerfilRepository perfilRepository, CalculadorPrecios precios) {
        this.perfilRepository = perfilRepository;
        this.precios = precios;
    }

    Map<Long, Perfil> autoresDe(Collection<Publicacion> publicaciones) {
        Set<Long> ids = new HashSet<>();
        for (Publicacion p : publicaciones) {
            ids.add(p.getAutorId());
        }
        Map<Long, Perfil> autores = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Perfil perfil : perfilRepository.buscarPorIds(ids)) {
                autores.put(perfil.getId(), perfil);
            }
        }
        return autores;
    }

    List<PublicacionResumenDto> resumir(List<Publicacion> publicaciones) {
        return resumir(publicaciones, autoresDe(publicaciones));
    }

    List<PublicacionResumenDto> resumir(List<Publicacion> publicaciones, Map<Long, Perfil> autores) {
        Map<Long, com.uade.skillswap.application.precio.PrecioPublicacion> preciosPorId = precios.paraTodas(publicaciones);
        return publicaciones.stream()
                .map(p -> Mapeador.resumen(p, autores.get(p.getAutorId()), preciosPorId.get(p.getId())))
                .toList();
    }
}
