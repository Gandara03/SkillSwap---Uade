package com.uade.skillswap.application.dto;

import java.util.List;

/**
 * Resumen de perfil publico: datos visibles + reputacion + ultimas calificaciones y publicaciones (Facade).
 * El historial resumido son las contrataciones completadas como proveedor: el total y las mas recientes.
 */
public record PerfilPublicoDto(
        Long id,
        String nombreVisible,
        String biografia,
        java.util.Set<String> habilidades,
        double reputacion,
        int cantidadCalificaciones,
        List<CalificacionDto> ultimasCalificaciones,
        List<PublicacionResumenDto> publicacionesActivas,
        int contratacionesCompletadas,
        List<ContratacionCompletadaDto> ultimasCompletadas) { }
