package com.uade.skillswap.application.dto;

import java.math.BigDecimal;

/** Resultado de cancelar: estado final y como se repartio lo retenido segun la politica. */
public record ResultadoCancelacionDto(ContratacionDto contratacion, String politica, BigDecimal reintegroCliente,
                                      BigDecimal compensacionProveedor) { }
