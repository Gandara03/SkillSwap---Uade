package com.uade.skillswap.application.port.in;

/** Resuelve quien es el actor actual (userId del servicio de identidad -> perfil). */
public interface SesionUseCase {

    /** @throws com.uade.skillswap.application.exception.NoAutorizadoException si no hay sesion o no hay perfil activo */
    Long perfilIdActual();
}
