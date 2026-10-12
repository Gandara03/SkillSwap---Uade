package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para buscar publicaciones (solo las PUBLICADAS). */
public interface IBusquedaController {

    Resultado<Pagina<PublicacionResumenDto>> buscar(FiltroBusqueda filtro, PaginaRequest pagina);
}
