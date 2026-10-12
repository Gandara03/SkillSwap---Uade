package com.uade.skillswap.application.command;

import com.uade.skillswap.domain.model.promocion.TipoPromocion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CrearPromocionCommand(
        Long publicacionId,
        String descripcion,
        TipoPromocion tipo,
        BigDecimal valor,
        LocalDateTime vigenteDesde,
        LocalDateTime vigenteHasta) { }
