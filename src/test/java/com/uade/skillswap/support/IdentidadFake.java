package com.uade.skillswap.support;

import com.uade.skillswap.application.port.out.IdentidadPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fake de {@link IdentidadPort} para tests de servicios: usuarios y sesion en memoria. */
public class IdentidadFake implements IdentidadPort {

    private final Map<String, UsuarioIdentidad> usuarios = new HashMap<>();
    private String sesionActual;

    public IdentidadFake agregarUsuario(String userId, String nombre) {
        return agregarUsuario(userId, nombre, userId + "@test.local", true);
    }

    public IdentidadFake agregarUsuario(String userId, String nombre, String email, boolean activo) {
        usuarios.put(userId, new UsuarioIdentidad(userId, nombre, email, activo));
        return this;
    }

    public void iniciarSesion(String userId) {
        if (!usuarios.containsKey(userId)) {
            throw new IllegalArgumentException("Usuario desconocido en el fake: " + userId);
        }
        sesionActual = userId;
    }

    public void cerrarSesion() {
        sesionActual = null;
    }

    @Override
    public Optional<UsuarioIdentidad> usuarioActual() {
        return buscarPorUserId(sesionActual);
    }

    @Override
    public Optional<UsuarioIdentidad> buscarPorUserId(String userId) {
        return Optional.ofNullable(userId == null ? null : usuarios.get(userId));
    }
}
