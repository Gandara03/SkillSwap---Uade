package com.uade.skillswap.infrastructure.identidad.sdk;

/**
 * Estructura propia del proveedor de identidad (no es nuestro modelo): usa otros nombres y
 * representa el estado de la cuenta como texto.
 */
public record UsuarioExterno(String uid, String displayName, String correoElectronico, String estadoCuenta) {

    public static final String CUENTA_ACTIVA = "ACTIVE";
    public static final String CUENTA_SUSPENDIDA = "SUSPENDED";
}
