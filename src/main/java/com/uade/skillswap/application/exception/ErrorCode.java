package com.uade.skillswap.application.exception;

/** Codigos de error uniformes que la presentacion traduce a mensajes para el usuario. */
public enum ErrorCode {
    VALIDACION,
    NO_ENCONTRADO,
    NO_AUTORIZADO,
    REGLA_DE_NEGOCIO,
    TRANSICION_INVALIDA,
    SALDO_INSUFICIENTE,
    CUPO_AGOTADO,
    FRANJA_NO_DISPONIBLE,
    CONFLICTO_CONCURRENCIA,
    ERROR_INTERNO
}
