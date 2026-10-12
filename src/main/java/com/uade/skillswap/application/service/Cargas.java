package com.uade.skillswap.application.service;

import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.application.port.out.WalletRepository;

/** Carga de agregados por id con el error uniforme de la capa (RecursoNoEncontradoException). */
final class Cargas {

    private Cargas() { }

    static Perfil perfil(PerfilRepository repo, Long id) {
        Entradas.exigir(id, "El perfil");
        return repo.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Perfil", id));
    }

    /** Perfil que ademas puede operar: un perfil dado de baja no puede ejecutar acciones. */
    static Perfil perfilActivo(PerfilRepository repo, Long id) {
        Perfil perfil = perfil(repo, id);
        if (!perfil.estaActivo()) {
            throw new NoAutorizadoException("El perfil esta dado de baja");
        }
        return perfil;
    }

    static Publicacion publicacion(PublicacionRepository repo, Long id) {
        Entradas.exigir(id, "La publicacion");
        return repo.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));
    }

    static Contratacion contratacion(ContratacionRepository repo, Long id) {
        Entradas.exigir(id, "La contratacion");
        return repo.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Contratacion", id));
    }

    static Wallet wallet(WalletRepository repo, Long perfilId) {
        Entradas.exigir(perfilId, "El perfil");
        return repo.buscarPorPerfilId(perfilId).orElseThrow(() -> new RecursoNoEncontradoException("Wallet", perfilId));
    }
}
