package com.uade.skillswap.domain.model.politica;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.Duration;

/** Base comun de las politicas escalonadas (100 / parcial / 0 por anticipacion). */
abstract class PoliticaPorUmbrales implements PoliticaCancelacion {

    private final UmbralesCancelacion umbrales;

    protected PoliticaPorUmbrales(UmbralesCancelacion umbrales) {
        this.umbrales = umbrales;
    }

    @Override
    public SkillCoins calcularReintegro(SkillCoins montoPagado, Duration anticipacion) {
        if (montoPagado == null || montoPagado.esCero()) {
            return SkillCoins.CERO;
        }
        long horas = anticipacion.toHours();
        if (anticipacion.isNegative()) {
            return SkillCoins.CERO;
        }
        if (horas >= umbrales.horasReintegroTotal()) {
            return montoPagado;
        }
        if (horas >= umbrales.horasReintegroParcial()) {
            return montoPagado.porcentaje(umbrales.porcentajeParcial());
        }
        return SkillCoins.CERO;
    }

    public UmbralesCancelacion getUmbrales() {
        return umbrales;
    }
}
