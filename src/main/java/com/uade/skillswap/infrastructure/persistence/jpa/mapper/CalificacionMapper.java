package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.CalificacionJpaEntity;

/** Calificacion (dominio) <-> CalificacionJpaEntity. */
public final class CalificacionMapper {

    private CalificacionMapper() {
    }

    public static Calificacion aDominio(CalificacionJpaEntity e) {
        return Calificacion.reconstruir(e.getId(), e.getContratacionId(), e.getAutorId(), e.getDestinatarioId(),
                Puntaje.de(e.getPuntaje()), e.getComentario(), e.getFecha());
    }

    public static CalificacionJpaEntity aEntidad(Calificacion d) {
        CalificacionJpaEntity e = new CalificacionJpaEntity();
        e.setContratacionId(d.getContratacionId());
        e.setAutorId(d.getAutorId());
        e.setDestinatarioId(d.getDestinatarioId());
        e.setPuntaje(d.getPuntaje().getValor());
        e.setComentario(d.getComentario());
        e.setFecha(Fechas.enMicros(d.getFecha()));
        return e;
    }

    public static void confirmar(Calificacion d, CalificacionJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
    }
}
