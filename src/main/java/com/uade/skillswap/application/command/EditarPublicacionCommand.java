package com.uade.skillswap.application.command;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;

import java.math.BigDecimal;
import java.util.Set;

public record EditarPublicacionCommand(
        String titulo,
        String descripcion,
        String categoria,
        Modalidad modalidad,
        int duracionMinutos,
        int cupo,
        BigDecimal precioBase,
        Set<String> etiquetas,
        Set<String> habilidades,
        TipoPoliticaCancelacion politicaCancelacion) { }
