package com.uade.skillswap.application.politica;

/** Umbrales configurables de cada politica (el reglamento no obliga a persistirlos por publicacion). */
public record ConfiguracionCancelacion(UmbralesCancelacion flexible, UmbralesCancelacion estricta) {

    public static ConfiguracionCancelacion porDefecto() {
        return new ConfiguracionCancelacion(PoliticaFlexible.UMBRALES_POR_DEFECTO, PoliticaEstricta.UMBRALES_POR_DEFECTO);
    }
}
