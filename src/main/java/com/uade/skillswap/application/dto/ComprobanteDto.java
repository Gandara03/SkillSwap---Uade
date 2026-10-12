package com.uade.skillswap.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprobanteDto(String numero, Long contratacionId, Long publicacionId, Long clienteId, Long proveedorId,
                             BigDecimal monto, LocalDateTime pagadoEn, LocalDateTime completadoEn) { }
