package com.uade.skillswap.domain.model.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Descuento porcentual sobre el precio acumulado hasta el momento. */
public class PromocionPorcentaje extends PromocionDecorator {

    private final BigDecimal porcentaje;
    private final String nombre;

    public PromocionPorcentaje(PrecioPublicacion interno, BigDecimal porcentaje, String nombre) {
        super(interno);
        this.porcentaje = porcentaje;
        this.nombre = nombre;
    }

    @Override
    public SkillCoins calcular() {
        SkillCoins actual = interno.calcular();
        return actual.restar(actual.porcentaje(porcentaje)).noNegativo();
    }

    @Override
    public List<String> detalle() {
        List<String> detalle = new ArrayList<>(interno.detalle());
        detalle.add("Promocion " + nombre + ": -" + porcentaje.stripTrailingZeros().toPlainString() + "%");
        return detalle;
    }
}
