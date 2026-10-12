package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record CalificacionDto(Long id, Long contratacionId, Long autorId, Long destinatarioId, int puntaje,
                              String comentario, LocalDateTime fecha) { }
