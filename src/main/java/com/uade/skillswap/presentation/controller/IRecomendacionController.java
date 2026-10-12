package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.presentation.Resultado;

import java.util.List;

/** Contrato de la UI para recomendaciones personalizadas (la estrategia la elige quien llama). */
public interface IRecomendacionController {

    Resultado<List<PublicacionResumenDto>> recomendar(TipoEstrategiaRecomendacion estrategia);
}
