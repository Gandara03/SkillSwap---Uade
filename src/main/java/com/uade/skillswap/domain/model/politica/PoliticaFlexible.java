package com.uade.skillswap.domain.model.politica;

/** Flexible: 100 % con 24 h o mas, 50 % con 2 h o mas, 0 % por debajo (valores por defecto). */
public class PoliticaFlexible extends PoliticaPorUmbrales {

    public static final UmbralesCancelacion UMBRALES_POR_DEFECTO = new UmbralesCancelacion(24, 2, 50);

    public PoliticaFlexible() {
        this(UMBRALES_POR_DEFECTO);
    }

    public PoliticaFlexible(UmbralesCancelacion umbrales) {
        super(umbrales);
    }

    @Override
    public TipoPoliticaCancelacion getTipo() {
        return TipoPoliticaCancelacion.FLEXIBLE;
    }
}
