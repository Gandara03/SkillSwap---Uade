package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Sincroniza colecciones de una entidad gestionada con el estado del dominio tocando solo las diferencias.
 * Si se hiciera clear()+addAll() Hibernate marcaria la coleccion como modificada en cada guardado
 * (borrar y reinsertar todo, e incrementar la version aunque no cambie nada).
 */
final class Sincronizador {

    private Sincronizador() {
    }

    static <T> void conjunto(Set<T> destino, Collection<T> origen) {
        destino.removeIf(e -> !origen.contains(e));
        for (T e : origen) {
            destino.add(e);
        }
    }

    /** Listas ordenadas con @OrderColumn: si son iguales no se toca; si difieren se reemplazan completas. */
    static <T> void lista(List<T> destino, List<T> origen) {
        if (!destino.equals(origen)) {
            List<T> copia = new ArrayList<>(origen);
            destino.clear();
            destino.addAll(copia);
        }
    }
}
