package com.uade.skillswap.application.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator base. Cada promocion descuenta sobre el resultado de la anterior (se acumulan) y
 * el total se acota en cero, asi que nunca queda negativo.
 */
public abstract class PromocionDecorator implements PrecioPublicacion {

    protected final PrecioPublicacion interno;

    protected PromocionDecorator(PrecioPublicacion interno) {
        this.interno = interno;
    }

    protected abstract SkillCoins descuentoSobre(SkillCoins precioActual);

    protected abstract String descripcionPromocion();

    @Override
    public SkillCoins calcular() {
        SkillCoins actual = interno.calcular();
        return actual.restar(descuentoSobre(actual)).noNegativo();
    }

    @Override
    public List<String> detalle() {
        List<String> detalle = new ArrayList<>(interno.detalle());
        detalle.add(descripcionPromocion());
        return detalle;
    }
}
