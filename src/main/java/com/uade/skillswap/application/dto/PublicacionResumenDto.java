package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;

import java.math.BigDecimal;

/** Fila de listado (busqueda/recomendacion). Incluye datos del autor para evitar N+1 en la presentacion. */
public record PublicacionResumenDto(
        Long id,
        TipoPublicacion tipo,
        String titulo,
        String categoria,
        Modalidad modalidad,
        BigDecimal precioBase,
        BigDecimal precioFinal,
        Long autorId,
        String autorNombre,
        double autorReputacion,
        int cupoDisponible,
        String estado) { }
