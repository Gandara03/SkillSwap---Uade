package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

public interface BusquedaUseCase {

    /** Solo publicaciones PUBLICADAS, con filtros combinables y paginacion. */
    Pagina<PublicacionResumenDto> buscar(FiltroBusqueda filtro, PaginaRequest pagina);
}
