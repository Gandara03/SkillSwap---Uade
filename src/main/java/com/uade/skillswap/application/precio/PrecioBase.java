package com.uade.skillswap.application.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.List;

/** Componente concreto: el precio base de la publicacion. */
public class PrecioBase implements PrecioPublicacion {

    private final SkillCoins base;

    public PrecioBase(SkillCoins base) {
        this.base = base.noNegativo();
    }

    @Override
    public SkillCoins calcular() {
        return base;
    }

    @Override
    public List<String> detalle() {
        return List.of("Precio base: " + base);
    }
}
