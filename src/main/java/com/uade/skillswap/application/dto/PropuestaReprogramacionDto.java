package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record PropuestaReprogramacionDto(Long nuevaFranjaId, Long propuestaPorId, LocalDateTime fecha) { }
