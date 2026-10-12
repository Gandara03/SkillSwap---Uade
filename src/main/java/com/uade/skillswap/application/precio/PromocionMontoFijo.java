package com.uade.skillswap.application.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

/** Descuento de monto fijo. Si supera el precio acumulado, el total queda en cero. */
public class PromocionMontoFijo extends PromocionDecorator {

    private final SkillCoins monto;
    private final String nombre;

    public PromocionMontoFijo(PrecioPublicacion interno, SkillCoins monto, String nombre) {
        super(interno);
        this.monto = monto;
        this.nombre = nombre;
    }

    @Override
    protected SkillCoins descuentoSobre(SkillCoins precioActual) {
        return monto;
    }

    @Override
    protected String descripcionPromocion() {
        return "Promocion " + nombre + ": -" + monto;
    }
}
