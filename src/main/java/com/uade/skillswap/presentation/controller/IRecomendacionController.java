package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para recomendaciones personalizadas (la estrategia la elige quien llama). */
public interface IRecomendacionController {

    Resultado<Pagina<PublicacionResumenDto>> recomendar(TipoEstrategiaRecomendacion estrategia, PaginaRequest pagina);
}
