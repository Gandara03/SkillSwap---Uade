package com.uade.skillswap.domain.model.politica;

/** Umbrales configurables de cada politica, comunes a todas las publicaciones. */
public record ConfiguracionCancelacion(UmbralesCancelacion flexible, UmbralesCancelacion estricta) {

    public static ConfiguracionCancelacion porDefecto() {
        return new ConfiguracionCancelacion(PoliticaFlexible.UMBRALES_POR_DEFECTO, PoliticaEstricta.UMBRALES_POR_DEFECTO);
    }
}
