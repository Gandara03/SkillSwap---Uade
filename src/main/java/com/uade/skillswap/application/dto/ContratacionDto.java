package com.uade.skillswap.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContratacionDto(
        Long id,
        Long publicacionId,
        String publicacionTitulo,
        Long clienteId,
        Long proveedorId,
        Long franjaId,
        String mensaje,
        String objetivo,
        BigDecimal precioFinal,
        String estado,
        PropuestaReprogramacionDto propuestaReprogramacion,
        List<HistorialDto> historial,
        LocalDateTime creadaEn,
        LocalDateTime pagadaEn,
        LocalDateTime completadaEn) { }
