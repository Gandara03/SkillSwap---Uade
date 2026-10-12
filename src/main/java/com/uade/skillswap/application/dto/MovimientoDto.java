package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.wallet.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDto(Long id, TipoMovimiento tipo, BigDecimal monto, BigDecimal saldoResultante,
                            Long contratacionId, LocalDateTime fecha) { }
