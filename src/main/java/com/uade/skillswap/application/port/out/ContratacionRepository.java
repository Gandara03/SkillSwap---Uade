package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.Optional;

public interface ContratacionRepository {
    Contratacion guardar(Contratacion contratacion);
    Optional<Contratacion> buscarPorId(Long id);
    Pagina<Contratacion> buscarPorParticipante(Long perfilId, PaginaRequest pagina);
    long contarPorPublicacion(Long publicacionId);
}
