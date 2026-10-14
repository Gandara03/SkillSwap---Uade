package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.model.vo.SkillCoins;

/**
 * Como se reparte lo retenido (escrow) al cancelar: lo que vuelve al cliente y lo que compensa al proveedor.
 * Entre los dos suman lo pagado. Lo calcula {@link Contratacion#repartirRetenido}.
 */
public final class RepartoCancelacion {

    private final SkillCoins reintegro;
    private final SkillCoins compensacion;

    RepartoCancelacion(SkillCoins reintegro, SkillCoins compensacion) {
        this.reintegro = reintegro;
        this.compensacion = compensacion;
    }

    public SkillCoins getReintegro() {
        return reintegro;
    }

    public SkillCoins getCompensacion() {
        return compensacion;
    }
}
