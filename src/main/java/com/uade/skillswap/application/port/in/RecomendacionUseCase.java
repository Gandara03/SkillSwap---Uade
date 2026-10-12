package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;

import java.util.List;

public interface RecomendacionUseCase {

    /** La estrategia se elige por parametro: cambiarla no modifica el caso de uso. */
    List<PublicacionResumenDto> recomendar(Long actorId, TipoEstrategiaRecomendacion estrategia);
}
