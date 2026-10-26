package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Fila de listado (busqueda/recomendacion). Incluye datos del autor para evitar N+1 en la presentacion.
 * {@code accionesDelAutor}: las que el autor puede pedir en el estado actual (no dependen de quien consulta).
 */
public record PublicacionResumenDto(
        Long id,
        TipoPublicacion tipo,
        String titulo,
        String descripcion,
        String categoria,
        Modalidad modalidad,
        int duracionMinutos,
        BigDecimal precioBase,
        BigDecimal precioFinal,
        Long autorId,
        String autorNombre,
        double autorReputacion,
        int cupoDisponible,
        Set<String> etiquetas,
        String estado,
        List<AccionPublicacion> accionesDelAutor) { }
