package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Operaciones comunes de los mappers JPA: campos que asigna la persistencia, fechas y colecciones. */
final class SoporteMapeo {

    private SoporteMapeo() {
    }

    /**
     * Los agregados del dominio no exponen setters de id/version (los asigna la persistencia). Para que la instancia
     * que recibe {@code guardar} quede coherente con la fila, como pasaria con {@code em.persist}, el repositorio
     * escribe esos campos por reflexion.
     */
    static void asignarCampo(Object objeto, String campo, Object valor) {
        try {
            Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo asignar '" + campo + "' en " + objeto.getClass().getName(), e);
        }
    }

    /**
     * Las columnas son DATETIME(6) (microsegundos). Si el dominio trae nanosegundos, la base los redondea o trunca
     * segun el motor y Hibernate veria el valor como "modificado" en cada guardado (UPDATE y version de mas).
     * Se trunca a microsegundos antes de escribir para que el valor leido sea siempre igual al escrito.
     */
    static LocalDateTime enMicros(LocalDateTime fecha) {
        return fecha == null ? null : fecha.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Sincroniza una coleccion de la entidad gestionada con el estado del dominio tocando solo las diferencias.
     * Si se hiciera clear()+addAll() Hibernate marcaria la coleccion como modificada en cada guardado
     * (borrar y reinsertar todo, e incrementar la version aunque no cambie nada).
     */
    static <T> void sincronizarConjunto(Set<T> destino, Collection<T> origen) {
        destino.removeIf(e -> !origen.contains(e));
        for (T e : origen) {
            destino.add(e);
        }
    }

    /** Listas ordenadas con @OrderColumn: si son iguales no se toca; si difieren se reemplazan completas. */
    static <T> void sincronizarLista(List<T> destino, List<T> origen) {
        if (!destino.equals(origen)) {
            List<T> copia = new ArrayList<>(origen);
            destino.clear();
            destino.addAll(copia);
        }
    }
}
