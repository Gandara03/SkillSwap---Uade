package com.uade.skillswap.domain.query;

public enum CriterioOrden {
    /** Coincidencia en el titulo primero, despues reputacion del autor y fecha (sin texto: reputacion y fecha). */
    RELEVANCIA,
    RECIENTES,
    PRECIO_ASC,
    PRECIO_DESC,
    REPUTACION
}
