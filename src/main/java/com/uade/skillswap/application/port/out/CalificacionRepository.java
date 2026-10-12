package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.List;

public interface CalificacionRepository {
    Calificacion guardar(Calificacion calificacion);
    boolean existe(Long contratacionId, Long autorId);
    List<Calificacion> buscarPorDestinatario(Long destinatarioId);
    Pagina<Calificacion> paginarPorDestinatario(Long destinatarioId, PaginaRequest pagina);
}
