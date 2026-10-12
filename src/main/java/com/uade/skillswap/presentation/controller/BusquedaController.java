package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.BusquedaUseCase;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: la busqueda es publica, no requiere actor. */
public class BusquedaController implements IBusquedaController {

    private final BusquedaUseCase busqueda;

    public BusquedaController(BusquedaUseCase busqueda) {
        this.busqueda = busqueda;
    }

    @Override
    public Resultado<Pagina<PublicacionResumenDto>> buscar(FiltroBusqueda filtro, PaginaRequest pagina) {
        return Ejecucion.sinActor(() -> busqueda.buscar(filtro, pagina));
    }
}
