package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** {@code accionesDelAutor}: las que el autor puede pedir en el estado actual (no dependen de quien consulta). */
public record PublicacionDto(
        Long id,
        TipoPublicacion tipo,
        Long autorId,
        String titulo,
        String descripcion,
        String categoria,
        Modalidad modalidad,
        int duracionMinutos,
        int cupo,
        int cupoDisponible,
        BigDecimal precioBase,
        BigDecimal precioFinal,
        List<String> detallePrecio,
        TipoPoliticaCancelacion politicaCancelacion,
        Set<String> etiquetas,
        Set<String> habilidades,
        List<FranjaDto> franjas,
        String estado,
        List<AccionPublicacion> accionesDelAutor,
        List<HistorialDto> historial,
        LocalDateTime creadaEn,
        LocalDateTime publicadaEn) { }
