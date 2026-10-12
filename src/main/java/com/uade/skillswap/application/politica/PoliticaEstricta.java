package com.uade.skillswap.application.politica;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;

/** Estricta: 100 % con 72 h o mas, 50 % con 48 h o mas, 0 % por debajo (valores por defecto). */
public class PoliticaEstricta extends PoliticaPorUmbrales {

    public static final UmbralesCancelacion UMBRALES_POR_DEFECTO = new UmbralesCancelacion(72, 48, 50);

    public PoliticaEstricta() {
        this(UMBRALES_POR_DEFECTO);
    }

    public PoliticaEstricta(UmbralesCancelacion umbrales) {
        super(umbrales);
    }

    @Override
    public TipoPoliticaCancelacion getTipo() {
        return TipoPoliticaCancelacion.ESTRICTA;
    }
}
