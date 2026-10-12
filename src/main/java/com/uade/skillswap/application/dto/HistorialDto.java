package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record HistorialDto(String estadoAnterior, String estadoNuevo, Long actorId, String motivo, LocalDateTime fecha) { }
