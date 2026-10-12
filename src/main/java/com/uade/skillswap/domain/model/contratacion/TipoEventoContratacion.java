package com.uade.skillswap.domain.model.contratacion;

/** Eventos relevantes del ciclo de vida de una contratacion (los que disparan notificaciones). */
public enum TipoEventoContratacion {
    SOLICITUD_RECIBIDA,
    SOLICITUD_ACEPTADA,
    SOLICITUD_RECHAZADA,
    PAGO_CONFIRMADO,
    REPROGRAMACION_PROPUESTA,
    REPROGRAMACION_ACEPTADA,
    REPROGRAMACION_RECHAZADA,
    CANCELACION,
    SESION_COMPLETADA
}
