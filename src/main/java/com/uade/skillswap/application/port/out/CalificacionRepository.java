package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CalificacionRepository {
    Calificacion guardar(Calificacion calificacion);
    boolean existe(Long contratacionId, Long autorId);
    /** De esas contrataciones, las que ese autor ya califico (una sola consulta para un listado). */
    Set<Long> calificadasPor(Long autorId, Collection<Long> contratacionIds);
    List<Calificacion> buscarPorDestinatario(Long destinatarioId);
    Pagina<Calificacion> paginarPorDestinatario(Long destinatarioId, PaginaRequest pagina);
}
