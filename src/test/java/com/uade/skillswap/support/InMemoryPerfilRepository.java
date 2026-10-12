package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.port.out.PerfilRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Fake de {@link PerfilRepository}: asigna ids secuenciales y guarda referencias. */
public class InMemoryPerfilRepository implements PerfilRepository {

    private final Map<Long, Perfil> perfiles = new LinkedHashMap<>();
    private long secuencia = 0;

    @Override
    public Perfil guardar(Perfil perfil) {
        if (perfil.getId() == null) {
            Reflejo.asignar(perfil, "id", ++secuencia);
        }
        Reflejo.asignar(perfil, "version", Reflejo.version(perfil.getVersion()));
        perfiles.put(perfil.getId(), perfil);
        return perfil;
    }

    @Override
    public Optional<Perfil> buscarPorId(Long id) {
        return Optional.ofNullable(perfiles.get(id));
    }

    @Override
    public Optional<Perfil> buscarPorUserId(String userId) {
        return perfiles.values().stream().filter(p -> p.getUserId().equals(userId)).findFirst();
    }

    @Override
    public List<Perfil> buscarPorIds(Set<Long> ids) {
        List<Perfil> resultado = new ArrayList<>();
        for (Perfil p : perfiles.values()) {
            if (ids.contains(p.getId())) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    public int cantidad() {
        return perfiles.size();
    }
}
