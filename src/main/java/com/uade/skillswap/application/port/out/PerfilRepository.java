package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.perfil.Perfil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PerfilRepository {
    Perfil guardar(Perfil perfil);
    Optional<Perfil> buscarPorId(Long id);
    Optional<Perfil> buscarPorUserId(String userId);
    List<Perfil> buscarPorIds(Set<Long> ids);
}
