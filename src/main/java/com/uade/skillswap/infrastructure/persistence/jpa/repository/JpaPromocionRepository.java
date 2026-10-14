package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PromocionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.PromocionMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpaPromocionRepository extends SimpleJpaRepository
        implements PromocionRepository {

    public JpaPromocionRepository(JpaContexto contexto) {
        super(contexto);
    }

    @Override
    public Promocion guardar(Promocion promocion) {
        return escribir(em -> {
            PromocionJpaEntity e;
            if (promocion.getId() == null) {
                e = new PromocionJpaEntity();
                PromocionMapper.aplicar(promocion, e);
                em.persist(e);
            } else {
                e = em.find(PromocionJpaEntity.class, promocion.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe la promocion " + promocion.getId());
                }
                PromocionMapper.aplicar(promocion, e);
            }
            em.flush();
            PromocionMapper.confirmar(promocion, e);
            return promocion;
        });
    }

    /** Promociones con el flag 'activa'; la vigencia por fecha la evalua el dominio (Promocion.estaVigente). */
    @Override
    public List<Promocion> buscarActivasPorPublicacion(Long publicacionId) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("publicacionId", publicacionId);
            parametros.put("activa", true);
            List<PromocionJpaEntity> filas = listar(em, "select p from PromocionJpaEntity p "
                    + "where p.publicacionId = :publicacionId and p.activa = :activa order by p.id",
                    PromocionJpaEntity.class, parametros);
            List<Promocion> resultado = new ArrayList<>(filas.size());
            for (PromocionJpaEntity e : filas) {
                resultado.add(PromocionMapper.aDominio(e));
            }
            return resultado;
        });
    }

    @Override
    public Map<Long, List<Promocion>> buscarActivasPorPublicaciones(Collection<Long> publicacionIds) {
        Map<Long, List<Promocion>> porPublicacion = new LinkedHashMap<>();
        if (publicacionIds == null || publicacionIds.isEmpty()) {
            return porPublicacion;
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ids", new ArrayList<>(publicacionIds));
            parametros.put("activa", true);
            List<PromocionJpaEntity> filas = listar(em, "select p from PromocionJpaEntity p "
                    + "where p.publicacionId in :ids and p.activa = :activa order by p.id",
                    PromocionJpaEntity.class, parametros);
            for (PromocionJpaEntity e : filas) {
                porPublicacion.computeIfAbsent(e.getPublicacionId(), k -> new ArrayList<>()).add(PromocionMapper.aDominio(e));
            }
            return porPublicacion;
        });
    }
}
