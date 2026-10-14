package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.BusquedaUseCase;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: valida la entrada y busca. La busqueda es publica, no requiere actor. */
public class BusquedaController implements IBusquedaController {

    private final BusquedaUseCase busqueda;

    public BusquedaController(BusquedaUseCase busqueda) {
        this.busqueda = busqueda;
    }

    @Override
    public Resultado<Pagina<PublicacionResumenDto>> buscar(FiltroBusqueda filtro, PaginaRequest pagina) {
        return Ejecucion.sinActor(() -> {
            ValidadorEntrada.exigirPagina(pagina);
            if (filtro != null) {
                validar(filtro);
            }
            return busqueda.buscar(filtro, pagina);
        });
    }

    /** El filtro es opcional (null = sin filtrar) y todos sus campos tambien; solo se revisan los rangos. */
    private static void validar(FiltroBusqueda filtro) {
        ValidadorEntrada.exigirEntre(filtro.reputacionMinima(), 0, ValidadorEntrada.PUNTAJE_MAXIMO,
                "la reputacion minima");
        ValidadorEntrada.exigirAnterior(filtro.disponibleDesde(), filtro.disponibleHasta(),
                "el rango de disponibilidad");
    }
}
