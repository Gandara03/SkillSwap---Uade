package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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

    /** Las habilidades sin repetidos que solo difieran en acentos (ver {@link Textos}): queda la primera. */
    public static Set<Habilidad> sinRepetir(Collection<Habilidad> habilidades) {
        Set<Habilidad> resultado = new LinkedHashSet<>();
        if (habilidades == null) {
            return resultado;
        }
        Set<String> claves = new HashSet<>();
        for (Habilidad habilidad : habilidades) {
            if (claves.add(Textos.clave(habilidad.nombre))) {
                resultado.add(habilidad);
            }
        }
        return resultado;
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
