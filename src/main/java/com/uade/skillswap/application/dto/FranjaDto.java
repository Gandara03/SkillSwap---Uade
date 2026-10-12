package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record FranjaDto(Long id, LocalDateTime inicio, LocalDateTime fin, boolean ocupada) { }
