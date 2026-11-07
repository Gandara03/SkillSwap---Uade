package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.contratacion.PropuestaReprogramacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.ContratacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.HistorialContratacionJpaEntity;

/** Contratacion (dominio) <-> ContratacionJpaEntity. Eventos pendientes y observers NO se persisten (son transitorios). */
public final class ContratacionMapper {

    private ContratacionMapper() {
    }

    public static Contratacion aDominio(ContratacionJpaEntity e) {
        PropuestaReprogramacion propuesta = e.getPropuestaFranjaId() == null ? null
                : new PropuestaReprogramacion(e.getPropuestaFranjaId(), e.getPropuestaPorId(), e.getPropuestaFecha());
        return Contratacion.reconstruir(e.getId(), e.getPublicacionId(), e.getClienteId(), e.getProveedorId(),
                e.getFranjaId(), e.getMensaje(), e.getObjetivo(), SkillCoins.de(e.getPrecioFinal()), e.getEstado(),
                propuesta, HistorialMapper.aDominio(e.getHistorial()), e.getCreadaEn(), e.getPagadaEn(),
                e.getCompletadaEn(), e.getCanceladaPorId(), e.getVersion());
    }

    public static void aplicar(Contratacion d, ContratacionJpaEntity e) {
        e.setPublicacionId(d.getPublicacionId());
        e.setClienteId(d.getClienteId());
        e.setProveedorId(d.getProveedorId());
        e.setFranjaId(d.getFranjaId());
        e.setMensaje(d.getMensaje());
        e.setObjetivo(d.getObjetivo());
        e.setPrecioFinal(d.getPrecioFinal().getMonto());
        e.setEstado(d.getEstado());
        PropuestaReprogramacion p = d.getPropuestaReprogramacion();
        e.setPropuestaFranjaId(p == null ? null : p.getNuevaFranjaId());
        e.setPropuestaPorId(p == null ? null : p.getPropuestaPorId());
        e.setPropuestaFecha(p == null ? null : SoporteMapeo.enMicros(p.getFecha()));
        e.setCreadaEn(SoporteMapeo.enMicros(d.getCreadaEn()));
        e.setPagadaEn(SoporteMapeo.enMicros(d.getPagadaEn()));
        e.setCompletadaEn(SoporteMapeo.enMicros(d.getCompletadaEn()));
        e.setCanceladaPorId(d.getCanceladaPorId());
        HistorialMapper.agregarNuevas(e.getHistorial(), d.getHistorial(), HistorialContratacionJpaEntity::new,
                fila -> fila.setContratacion(e));
    }

    public static void confirmar(Contratacion d, ContratacionJpaEntity e) {
        SoporteMapeo.asignarCampo(d, "id", e.getId());
        SoporteMapeo.asignarCampo(d, "version", e.getVersion());
    }
}
