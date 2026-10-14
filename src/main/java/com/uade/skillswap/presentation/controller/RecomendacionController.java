package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: valida la entrada y recomienda para el usuario con sesion iniciada. */
public class RecomendacionController implements IRecomendacionController {

    private final RecomendacionUseCase recomendaciones;
    private final SesionUseCase sesion;

    public RecomendacionController(RecomendacionUseCase recomendaciones, SesionUseCase sesion) {
        this.recomendaciones = recomendaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<Pagina<PublicacionResumenDto>> recomendar(TipoEstrategiaRecomendacion estrategia,
                                                              PaginaRequest pagina) {
        // la estrategia es opcional: sin estrategia, la factory usa AFINIDAD
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirPagina(pagina);
            return recomendaciones.recomendar(actor, estrategia, pagina);
        });
    }
}
