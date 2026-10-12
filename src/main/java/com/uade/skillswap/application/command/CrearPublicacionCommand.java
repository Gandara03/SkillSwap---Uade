package com.uade.skillswap.application.command;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Crea un borrador; puede venir incompleto (la completitud se exige al publicar). */
public record CrearPublicacionCommand(
        TipoPublicacion tipo,
        String titulo,
        String descripcion,
        String categoria,
        Modalidad modalidad,
        int duracionMinutos,
        int cupo,
        BigDecimal precioBase,
        Set<String> etiquetas,
        Set<String> habilidades,
        TipoPoliticaCancelacion politicaCancelacion,
        List<FranjaCommand> franjas) { }
