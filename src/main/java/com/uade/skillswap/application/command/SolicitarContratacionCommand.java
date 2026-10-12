package com.uade.skillswap.application.command;

public record SolicitarContratacionCommand(Long publicacionId, Long franjaId, String mensaje, String objetivo) { }
