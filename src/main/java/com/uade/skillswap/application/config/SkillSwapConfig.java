package com.uade.skillswap.application.config;

import com.uade.skillswap.domain.model.politica.PoliticaEstricta;
import com.uade.skillswap.domain.model.politica.PoliticaFlexible;
import com.uade.skillswap.domain.model.politica.UmbralesCancelacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;

/** Parametros de negocio configurables (los inyecta el compositor). */
public record SkillSwapConfig(
        SkillCoins bonoInicial,
        int candidatasRecomendacion,
        int limiteRecomendaciones,
        int ultimasCalificacionesPerfilPublico,
        int ultimasPublicacionesPerfilPublico,
        UmbralesCancelacion cancelacionFlexible,
        UmbralesCancelacion cancelacionEstricta) {

    public static SkillSwapConfig porDefecto() {
        return new SkillSwapConfig(SkillCoins.de(100), 200, 10, 5, 5,
                PoliticaFlexible.UMBRALES_POR_DEFECTO, PoliticaEstricta.UMBRALES_POR_DEFECTO);
    }
}
