package com.uade.skillswap.domain.model.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.ArrayList;
import java.util.List;

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
    public SkillCoins calcular() {
        return interno.calcular().restar(monto).noNegativo();
    }

    @Override
    public List<String> detalle() {
        List<String> detalle = new ArrayList<>(interno.detalle());
        detalle.add("Promocion " + nombre + ": -" + monto);
        return detalle;
    }
}
