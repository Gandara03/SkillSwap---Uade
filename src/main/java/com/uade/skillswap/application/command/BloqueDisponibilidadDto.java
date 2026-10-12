package com.uade.skillswap.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record BloqueDisponibilidadDto(DayOfWeek dia, LocalTime desde, LocalTime hasta) { }
