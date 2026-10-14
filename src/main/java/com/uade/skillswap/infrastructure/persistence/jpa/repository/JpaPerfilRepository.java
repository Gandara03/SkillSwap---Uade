package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PerfilJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.PerfilMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JpaPerfilRepository extends SimpleJpaRepository implements PerfilRepository {

    public JpaPerfilRepository(JpaContexto contexto) {
        super(contexto);
    }

    @Override
    public Perfil guardar(Perfil perfil) {
        return escribir(em -> {
            PerfilJpaEntity e;
            if (perfil.getId() == null) {
                e = new PerfilJpaEntity();
                PerfilMapper.aplicar(perfil, e);
                em.persist(e);
            } else {
                e = em.find(PerfilJpaEntity.class, perfil.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe el perfil " + perfil.getId());
                }
                exigirVersion("El perfil", perfil.getId(), perfil.getVersion(), e.getVersion());
                PerfilMapper.aplicar(perfil, e);
            }
            em.flush();
            PerfilMapper.confirmar(perfil, e);
            return perfil;
        });
    }

    @Override
    public Optional<Perfil> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return leer(em -> Optional.ofNullable(em.find(PerfilJpaEntity.class, id)).map(PerfilMapper::aDominio));
    }

    @Override
    public Optional<Perfil> buscarPorUserId(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("userId", userId);
            List<PerfilJpaEntity> encontrados = listar(em,
                    "select p from PerfilJpaEntity p where p.userId = :userId", PerfilJpaEntity.class, parametros);
            return encontrados.stream().findFirst().map(PerfilMapper::aDominio);
        });
    }

    /** Una consulta para los perfiles y una por coleccion (3): sin N+1 aunque se pidan muchos ids. */
    @Override
    public List<Perfil> buscarPorIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ids", ids);
            List<PerfilJpaEntity> entidades = listar(em,
                    "select p from PerfilJpaEntity p where p.id in :ids order by p.id", PerfilJpaEntity.class, parametros);
            precargar(em, PerfilJpaEntity.class, ids, "habilidades", "intereses", "disponibilidad");
            List<Perfil> perfiles = new ArrayList<>(entidades.size());
            for (PerfilJpaEntity e : entidades) {
                perfiles.add(PerfilMapper.aDominio(e));
            }
            return perfiles;
        });
    }
}
