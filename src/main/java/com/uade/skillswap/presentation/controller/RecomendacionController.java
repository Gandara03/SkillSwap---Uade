package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.presentation.Resultado;

import java.util.List;

/** Adaptador delgado: recomienda para el usuario con sesion iniciada. */
public class RecomendacionController implements IRecomendacionController {

    private final RecomendacionUseCase recomendaciones;
    private final SesionUseCase sesion;

    public RecomendacionController(RecomendacionUseCase recomendaciones, SesionUseCase sesion) {
        this.recomendaciones = recomendaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<List<PublicacionResumenDto>> recomendar(TipoEstrategiaRecomendacion estrategia) {
        return Ejecucion.conActor(sesion, actor -> recomendaciones.recomendar(actor, estrategia));
    }
}
