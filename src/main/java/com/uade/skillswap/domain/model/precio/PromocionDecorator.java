package com.uade.skillswap.domain.model.precio;

/**
 * Decorator base: guarda el precio que envuelve. Cada promocion concreta descuenta sobre ese precio (asi se
 * acumulan), acota el total en cero y agrega su linea al detalle.
 */
public abstract class PromocionDecorator implements PrecioPublicacion {

    protected final PrecioPublicacion interno;

    protected PromocionDecorator(PrecioPublicacion interno) {
        this.interno = interno;
    }
}
