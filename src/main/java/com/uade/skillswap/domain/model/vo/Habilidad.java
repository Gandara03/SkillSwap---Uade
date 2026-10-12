package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.util.Locale;

/** Value Object: habilidad o skill. El nombre se normaliza (minusculas, sin espacios sobrantes). */
public final class Habilidad {

    private final String nombre;

    private Habilidad(String nombre) {
        this.nombre = nombre;
    }

    public static Habilidad de(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre de la habilidad es obligatorio");
        }
        return new Habilidad(nombre.trim().toLowerCase(Locale.ROOT));
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Habilidad h && h.nombre.equals(nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
