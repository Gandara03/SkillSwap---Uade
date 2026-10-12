package com.uade.skillswap.application.dto;

import java.util.List;

/** Resumen de perfil publico: datos visibles + reputacion + ultimas calificaciones y publicaciones (Facade). */
public record PerfilPublicoDto(
        Long id,
        String nombreVisible,
        String biografia,
        java.util.Set<String> habilidades,
        double reputacion,
        int cantidadCalificaciones,
        List<CalificacionDto> ultimasCalificaciones,
        List<PublicacionResumenDto> publicacionesActivas) { }
