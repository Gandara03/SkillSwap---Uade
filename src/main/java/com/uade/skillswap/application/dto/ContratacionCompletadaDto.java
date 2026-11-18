package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

/** Fila del historial publico de un proveedor: que publicacion se completo y cuando, sin datos del cliente. */
public record ContratacionCompletadaDto(String tituloPublicacion, LocalDateTime completadaEn) { }
