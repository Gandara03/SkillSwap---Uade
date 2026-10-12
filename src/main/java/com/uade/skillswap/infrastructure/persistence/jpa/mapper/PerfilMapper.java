package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.BloqueDisponibilidadEmbebible;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PerfilJpaEntity;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Perfil (dominio) <-> PerfilJpaEntity. Lectura con {@code Perfil.reconstruir}; escritura campo a campo. */
public final class PerfilMapper {

    private PerfilMapper() {
    }

    public static Perfil aDominio(PerfilJpaEntity e) {
        List<Habilidad> habilidades = new ArrayList<>();
        for (String h : e.getHabilidades()) {
            habilidades.add(Habilidad.de(h));
        }
        List<BloqueDisponibilidad> disponibilidad = new ArrayList<>();
        for (BloqueDisponibilidadEmbebible b : e.getDisponibilidad()) {
            disponibilidad.add(new BloqueDisponibilidad(DayOfWeek.valueOf(b.getDia()), b.getDesde(), b.getHasta()));
        }
        return Perfil.reconstruir(e.getId(), e.getUserId(), TipoPerfil.valueOf(e.getTipo()), e.getNombreVisible(),
                e.getBiografia(), ZoneId.of(e.getZonaHoraria()), habilidades, new LinkedHashSet<>(e.getIntereses()),
                disponibilidad, CanalNotificacion.valueOf(e.getCanalPreferido()), e.getReputacion(),
                e.getCantidadCalificaciones(), e.isActivo(), e.getCreadoEn(), e.getVersion());
    }

    /** Vuelca el estado del dominio en la entidad (nueva o gestionada). Las colecciones se sincronizan por diferencia. */
    public static void aplicar(Perfil d, PerfilJpaEntity e) {
        e.setUserId(d.getUserId());
        e.setTipo(d.getTipo().name());
        e.setNombreVisible(d.getNombreVisible());
        e.setBiografia(d.getBiografia());
        e.setZonaHoraria(d.getZonaHoraria().getId());
        e.setCanalPreferido(d.getCanalPreferido().name());
        e.setReputacion(d.getReputacion());
        e.setCantidadCalificaciones(d.getCantidadCalificaciones());
        e.setActivo(d.estaActivo());
        e.setCreadoEn(Fechas.enMicros(d.getCreadoEn()));

        Set<String> habilidades = new LinkedHashSet<>();
        for (Habilidad h : d.getHabilidades()) {
            habilidades.add(h.getNombre());
        }
        Sincronizador.conjunto(e.getHabilidades(), habilidades);
        Sincronizador.conjunto(e.getIntereses(), d.getIntereses());

        List<BloqueDisponibilidadEmbebible> bloques = new ArrayList<>();
        for (BloqueDisponibilidad b : d.getDisponibilidad()) {
            BloqueDisponibilidadEmbebible fila = new BloqueDisponibilidadEmbebible();
            fila.setDia(b.getDia().name());
            fila.setDesde(b.getDesde());
            fila.setHasta(b.getHasta());
            bloques.add(fila);
        }
        Sincronizador.lista(e.getDisponibilidad(), bloques);
    }

    /** Despues del flush: la instancia de dominio recibe el id y la version asignados por la base. */
    public static void confirmar(Perfil d, PerfilJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
        CamposDominio.asignar(d, "version", e.getVersion());
    }
}
