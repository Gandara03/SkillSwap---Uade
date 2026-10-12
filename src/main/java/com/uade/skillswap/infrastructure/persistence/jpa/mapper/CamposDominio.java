package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import java.lang.reflect.Field;

/**
 * Los agregados del dominio no exponen setters de id/version (los asigna la persistencia). Para que la instancia
 * que recibe {@code guardar} quede coherente con la fila, como pasaria con {@code em.persist}, el repositorio
 * escribe esos campos por reflexion. Es la unica concesion de la infraestructura: el dominio no se toca.
 */
final class CamposDominio {

    private CamposDominio() {
    }

    static void asignar(Object objeto, String campo, Object valor) {
        try {
            Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo asignar '" + campo + "' en " + objeto.getClass().getName(), e);
        }
    }
}
