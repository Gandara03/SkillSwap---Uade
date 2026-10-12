package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.HistorialBaseJpaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Mapeo del historial de estados (comun a publicaciones y contrataciones). */
final class HistorialMapper {

    private HistorialMapper() {
    }

    static HistorialEstado aDominio(HistorialBaseJpaEntity e) {
        return HistorialEstado.reconstruir(e.getId(), e.getEstadoAnterior(), e.getEstadoNuevo(), e.getActorId(),
                e.getMotivo(), e.getFecha());
    }

    static <H extends HistorialBaseJpaEntity> List<HistorialEstado> aDominio(List<H> entidades) {
        List<HistorialEstado> resultado = new ArrayList<>(entidades.size());
        for (H e : entidades) {
            resultado.add(aDominio(e));
        }
        return resultado;
    }

    /**
     * El historial es append-only: las entradas del dominio que exceden las ya persistidas son las nuevas
     * (no se usa el id porque la entrada de dominio ya guardada puede conservar id null en la misma instancia).
     */
    static <H extends HistorialBaseJpaEntity> void agregarNuevas(List<H> persistidas, List<HistorialEstado> dominio,
                                                                  Supplier<H> fabrica, Consumer<H> vincular) {
        for (int i = persistidas.size(); i < dominio.size(); i++) {
            HistorialEstado origen = dominio.get(i);
            H fila = fabrica.get();
            fila.setEstadoAnterior(origen.getEstadoAnterior());
            fila.setEstadoNuevo(origen.getEstadoNuevo());
            fila.setActorId(origen.getActorId());
            fila.setMotivo(origen.getMotivo());
            fila.setFecha(Fechas.enMicros(origen.getFecha()));
            vincular.accept(fila);
            persistidas.add(fila);
        }
    }
}
