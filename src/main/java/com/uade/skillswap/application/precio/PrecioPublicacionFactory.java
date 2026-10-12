package com.uade.skillswap.application.precio;

import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.LocalDateTime;
import java.util.List;

/** Simple Factory: arma la cadena de decorators con las promociones vigentes. */
public final class PrecioPublicacionFactory {

    private PrecioPublicacionFactory() { }

    public static PrecioPublicacion construir(SkillCoins precioBase, List<Promocion> promociones, LocalDateTime ahora) {
        PrecioPublicacion precio = new PrecioBase(precioBase);
        if (promociones == null) {
            return precio;
        }
        for (Promocion promo : promociones) {
            if (!promo.estaVigente(ahora)) {
                continue;
            }
            precio = switch (promo.getTipo()) {
                case PORCENTAJE -> new PromocionPorcentaje(precio, promo.getValor(), promo.getDescripcion());
                case MONTO_FIJO -> new PromocionMontoFijo(precio, SkillCoins.de(promo.getValor()), promo.getDescripcion());
            };
        }
        return precio;
    }
}
