package com.uade.skillswap.application.politica;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.Duration;

/** Strategy: calcula cuanto se reintegra al cliente segun la anticipacion con que cancela. */
public interface PoliticaCancelacion {

    TipoPoliticaCancelacion getTipo();

    /**
     * @param montoPagado  monto retenido en la contratacion
     * @param anticipacion tiempo entre el momento de cancelar y el inicio de la franja (puede ser negativo)
     * @return monto a reintegrar al cliente, nunca negativo ni mayor al pagado
     */
    SkillCoins calcularReintegro(SkillCoins montoPagado, Duration anticipacion);
}
