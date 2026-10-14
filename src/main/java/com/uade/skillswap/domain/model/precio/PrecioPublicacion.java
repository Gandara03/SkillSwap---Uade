package com.uade.skillswap.domain.model.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.List;

/** Componente del patron Decorator: precio final de una publicacion. */
public interface PrecioPublicacion {

    SkillCoins calcular();

    /** Desglose legible: precio base y cada promocion aplicada. */
    List<String> detalle();
}
