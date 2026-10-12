package com.uade.skillswap.application.port.out;

import java.util.Optional;

/**
 * Puerto hacia el servicio de identidad externo. El Adapter (infraestructura) traduce el SDK del
 * proveedor a este contrato; la aplicacion nunca ve el SDK.
 */
public interface IdentidadPort {

    record UsuarioIdentidad(String userId, String nombre, String email, boolean activo) { }

    /** Usuario con sesion iniciada, o vacio si no hay sesion. */
    Optional<UsuarioIdentidad> usuarioActual();

    Optional<UsuarioIdentidad> buscarPorUserId(String userId);
}
