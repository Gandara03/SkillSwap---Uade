package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

/** Value Object: rango de precios para filtrar busquedas. Cualquiera de los extremos puede ser null. */
public final class RangoPrecio {

    private final SkillCoins minimo;
    private final SkillCoins maximo;

    public RangoPrecio(SkillCoins minimo, SkillCoins maximo) {
        if (minimo != null && maximo != null && minimo.esMayorQue(maximo)) {
            throw new ReglaDeNegocioException("El precio minimo no puede superar al maximo");
        }
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public SkillCoins getMinimo() {
        return minimo;
    }

    public SkillCoins getMaximo() {
        return maximo;
    }

    public boolean incluye(SkillCoins precio) {
        return (minimo == null || !precio.esMenorQue(minimo)) && (maximo == null || !precio.esMayorQue(maximo));
    }
}
