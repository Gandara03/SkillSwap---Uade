package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.contratacion.AccionContratacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code franjaInicio}: inicio de la franja reservada (null si la publicacion ya no esta disponible).
 * {@code acciones}: las que puede pedir quien hizo la consulta, segun el estado y su parte en la contratacion.
 */
public record ContratacionDto(
        Long id,
        Long publicacionId,
        String publicacionTitulo,
        Long clienteId,
        Long proveedorId,
        Long franjaId,
        LocalDateTime franjaInicio,
        String mensaje,
        String objetivo,
        BigDecimal precioFinal,
        String estado,
        List<AccionContratacion> acciones,
        PropuestaReprogramacionDto propuestaReprogramacion,
        List<HistorialDto> historial,
        LocalDateTime creadaEn,
        LocalDateTime pagadaEn,
        LocalDateTime completadaEn) { }
