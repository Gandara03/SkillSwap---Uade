package com.uade.skillswap.domain.model.precio;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.math.BigDecimal;

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
    protected SkillCoins descuentoSobre(SkillCoins precioActual) {
        return precioActual.porcentaje(porcentaje);
    }

    @Override
    protected String descripcionPromocion() {
        return "Promocion " + nombre + ": -" + porcentaje.stripTrailingZeros().toPlainString() + "%";
    }
}
