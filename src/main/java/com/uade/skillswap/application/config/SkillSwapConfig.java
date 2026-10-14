package com.uade.skillswap.application.config;

import com.uade.skillswap.domain.model.politica.ConfiguracionCancelacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;

/** Parametros de negocio configurables (inyectados por el compositor, no hardcodeados en los servicios). */
public record SkillSwapConfig(
        SkillCoins bonoInicial,
        int candidatasRecomendacion,
        int limiteRecomendaciones,
        int ultimasCalificacionesPerfilPublico,
        int ultimasPublicacionesPerfilPublico,
        ConfiguracionCancelacion cancelacion) {

    public static SkillSwapConfig porDefecto() {
        return new SkillSwapConfig(SkillCoins.de(100), 200, 10, 5, 5, ConfiguracionCancelacion.porDefecto());
    }
}
