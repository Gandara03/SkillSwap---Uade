package com.uade.skillswap.application.recomendacion;

/** Simple Factory de estrategias de recomendacion. */
public final class EstrategiaRecomendacionFactory {

    private EstrategiaRecomendacionFactory() { }

    public static EstrategiaRecomendacion crear(TipoEstrategiaRecomendacion tipo) {
        if (tipo == null) {
            return new RecomendacionPorAfinidad();
        }
        return switch (tipo) {
            case AFINIDAD -> new RecomendacionPorAfinidad();
            case POPULARIDAD -> new RecomendacionPorPopularidad();
        };
    }
}
