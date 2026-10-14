package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

public interface RecomendacionUseCase {

    /**
     * La estrategia se elige por parametro: cambiarla no modifica el caso de uso. El ranking tiene como maximo
     * SkillSwapConfig.limiteRecomendaciones elementos y se devuelve la pagina pedida.
     */
    Pagina<PublicacionResumenDto> recomendar(Long actorId, TipoEstrategiaRecomendacion estrategia,
                                             PaginaRequest pagina);
}
