package com.uade.skillswap.support;

import java.lang.reflect.Field;

/**
 * Utilidad de tests: los agregados del dominio no exponen setters de id/version (los asigna la
 * persistencia), asi que los repositorios en memoria simulan esa asignacion por reflexion.
 */
final class Reflejo {

    private Reflejo() { }

    static void asignar(Object objeto, String campo, Object valor) {
        try {
            Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo asignar " + campo + " en " + objeto.getClass(), e);
        }
    }

    static Long version(Long actual) {
        return actual == null ? 0L : actual + 1;
    }
}
