package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PromocionJpaEntity;

/** Promocion (dominio) <-> PromocionJpaEntity. */
public final class PromocionMapper {

    private PromocionMapper() {
    }

    public static Promocion aDominio(PromocionJpaEntity e) {
        return Promocion.reconstruir(e.getId(), e.getPublicacionId(), e.getDescripcion(), TipoPromocion.valueOf(e.getTipo()),
                e.getValor(), e.getVigenteDesde(), e.getVigenteHasta(), e.isActiva());
    }

    public static void aplicar(Promocion d, PromocionJpaEntity e) {
        e.setPublicacionId(d.getPublicacionId());
        e.setDescripcion(d.getDescripcion());
        e.setTipo(d.getTipo().name());
        e.setValor(d.getValor());
        e.setVigenteDesde(Fechas.enMicros(d.getVigenteDesde()));
        e.setVigenteHasta(Fechas.enMicros(d.getVigenteHasta()));
        e.setActiva(d.isActiva());
    }

    public static void confirmar(Promocion d, PromocionJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
    }
}
