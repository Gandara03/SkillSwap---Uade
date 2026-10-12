package com.uade.skillswap.presentation.ui;

/**
 * Lo unico que el menu necesita del servicio de identidad: abrir/cerrar sesion.
 * El compositor lo implementa con el adapter de infraestructura (la UI no lo conoce).
 */
public interface Autenticador {

    /** @throws IllegalArgumentException si el usuario no existe en el proveedor de identidad */
    void iniciarSesion(String userId);

    void cerrarSesion();
}
