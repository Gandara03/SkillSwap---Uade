package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.FranjaJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.HistorialPublicacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PublicacionJpaEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Publicacion (dominio) <-> PublicacionJpaEntity, incluyendo franjas, etiquetas, habilidades e historial. */
public final class PublicacionMapper {

    private PublicacionMapper() {
    }

    public static Publicacion aDominio(PublicacionJpaEntity e) {
        List<Habilidad> habilidades = new ArrayList<>();
        for (String h : e.getHabilidades()) {
            habilidades.add(Habilidad.de(h));
        }
        List<Franja> franjas = new ArrayList<>();
        for (FranjaJpaEntity f : e.getFranjas()) {
            franjas.add(new Franja(f.getId(), f.getInicio(), f.getFin(), f.isOcupada()));
        }
        return Publicacion.reconstruir(e.getId(), TipoPublicacion.valueOf(e.getTipo()), e.getAutorId(), e.getTitulo(),
                e.getDescripcion(), e.getCategoria(),
                e.getModalidad() == null ? null : Modalidad.valueOf(e.getModalidad()),
                e.getDuracionMinutos(), e.getCupo(), e.getCupoOcupado(),
                e.getPrecioBase() == null ? null : SkillCoins.de(e.getPrecioBase()),
                TipoPoliticaCancelacion.valueOf(e.getPoliticaCancelacion()),
                new LinkedHashSet<>(e.getEtiquetas()), habilidades, franjas, e.getEstado(),
                HistorialMapper.aDominio(e.getHistorial()), e.getCreadaEn(), e.getPublicadaEn(), e.getVersion());
    }

    /**
     * Vuelca el dominio en la entidad. Franjas e historial son solo-agregado: el dominio nunca las borra, asi que
     * se actualiza 'ocupada' de las existentes (por id) y se agregan las nuevas (id null).
     */
    public static void aplicar(Publicacion d, PublicacionJpaEntity e) {
        e.setTipo(d.getTipo().name());
        e.setAutorId(d.getAutorId());
        e.setTitulo(d.getTitulo());
        e.setDescripcion(d.getDescripcion());
        e.setCategoria(d.getCategoria());
        e.setModalidad(d.getModalidad() == null ? null : d.getModalidad().name());
        e.setDuracionMinutos(d.getDuracionMinutos());
        e.setCupo(d.getCupo());
        e.setCupoOcupado(d.getCupoOcupado());
        e.setPrecioBase(d.getPrecioBase() == null ? null : d.getPrecioBase().getMonto());
        e.setPoliticaCancelacion(d.getPoliticaCancelacion().name());
        e.setEstado(d.getEstado());
        e.setCreadaEn(Fechas.enMicros(d.getCreadaEn()));
        e.setPublicadaEn(Fechas.enMicros(d.getPublicadaEn()));

        Set<String> habilidades = new LinkedHashSet<>();
        for (Habilidad h : d.getHabilidades()) {
            habilidades.add(h.getNombre());
        }
        Sincronizador.conjunto(e.getHabilidades(), habilidades);
        Sincronizador.conjunto(e.getEtiquetas(), d.getEtiquetas());

        boolean franjasCambiaron = sincronizarFranjas(d, e);
        if (franjasCambiaron && e.getId() != null) {
            // Fuerza que la fila de la publicacion cambie (y por lo tanto su @Version) cuando solo se movieron franjas.
            e.setRevision(e.getRevision() + 1);
        }

        HistorialMapper.agregarNuevas(e.getHistorial(), d.getHistorial(), HistorialPublicacionJpaEntity::new,
                fila -> fila.setPublicacion(e));
    }

    private static boolean sincronizarFranjas(Publicacion d, PublicacionJpaEntity e) {
        Map<Long, FranjaJpaEntity> existentes = new HashMap<>();
        for (FranjaJpaEntity f : e.getFranjas()) {
            existentes.put(f.getId(), f);
        }
        boolean cambio = false;
        for (Franja franja : d.getFranjas()) {
            if (franja.getId() == null) {
                FranjaJpaEntity nueva = new FranjaJpaEntity();
                nueva.setPublicacion(e);
                nueva.setInicio(Fechas.enMicros(franja.getInicio()));
                nueva.setFin(Fechas.enMicros(franja.getFin()));
                nueva.setOcupada(franja.estaOcupada());
                e.getFranjas().add(nueva);
                cambio = true;
                continue;
            }
            FranjaJpaEntity fila = existentes.get(franja.getId());
            if (fila == null) {
                throw new IllegalStateException("La franja " + franja.getId() + " no pertenece a la publicacion " + e.getId());
            }
            if (fila.isOcupada() != franja.estaOcupada()) {
                fila.setOcupada(franja.estaOcupada());
                cambio = true;
            }
        }
        return cambio;
    }

    /** Despues del flush: id/version de la publicacion e ids de las franjas nuevas en la instancia de dominio. */
    public static void confirmar(Publicacion d, PublicacionJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
        CamposDominio.asignar(d, "version", e.getVersion());
        Set<Long> conocidas = new HashSet<>();
        for (Franja f : d.getFranjas()) {
            if (f.getId() != null) {
                conocidas.add(f.getId());
            }
        }
        // Las franjas nuevas del dominio (id null) se agregaron a la entidad en el mismo orden.
        List<FranjaJpaEntity> nuevas = new ArrayList<>();
        for (FranjaJpaEntity f : e.getFranjas()) {
            if (!conocidas.contains(f.getId())) {
                nuevas.add(f);
            }
        }
        int i = 0;
        for (Franja f : d.getFranjas()) {
            if (f.getId() == null) {
                CamposDominio.asignar(f, "id", nuevas.get(i++).getId());
            }
        }
    }
}
