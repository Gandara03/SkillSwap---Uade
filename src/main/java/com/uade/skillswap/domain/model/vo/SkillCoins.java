package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object: cantidad de SkillCoins (moneda ficticia). Inmutable, escala 2.
 * Puede ser negativo solo como resultado intermedio de un calculo; los saldos y
 * precios finales se validan en las entidades.
 */
public final class SkillCoins implements Comparable<SkillCoins> {

    public static final SkillCoins CERO = new SkillCoins(BigDecimal.ZERO);

    private final BigDecimal monto;

    private SkillCoins(BigDecimal monto) {
        this.monto = monto.setScale(2, RoundingMode.HALF_UP);
    }

    public static SkillCoins de(BigDecimal monto) {
        if (monto == null) {
            throw new ReglaDeNegocioException("El monto de SkillCoins es obligatorio");
        }
        return new SkillCoins(monto);
    }

    public static SkillCoins de(long monto) {
        return new SkillCoins(BigDecimal.valueOf(monto));
    }

    public static SkillCoins de(double monto) {
        return new SkillCoins(BigDecimal.valueOf(monto));
    }

    public static SkillCoins de(String monto) {
        return new SkillCoins(new BigDecimal(monto));
    }

    public SkillCoins sumar(SkillCoins otro) {
        return new SkillCoins(monto.add(otro.monto));
    }

    public SkillCoins restar(SkillCoins otro) {
        return new SkillCoins(monto.subtract(otro.monto));
    }

    /** Devuelve el porcentaje indicado de este monto (ej: 50 -> la mitad). */
    public SkillCoins porcentaje(BigDecimal porcentaje) {
        return new SkillCoins(monto.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public SkillCoins porcentaje(int porcentaje) {
        return porcentaje(BigDecimal.valueOf(porcentaje));
    }

    /** Si el valor es negativo devuelve CERO (usado para que nunca haya totales negativos). */
    public SkillCoins noNegativo() {
        return esNegativo() ? CERO : this;
    }

    public boolean esNegativo() {
        return monto.signum() < 0;
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }

    public boolean esMayorQue(SkillCoins otro) {
        return compareTo(otro) > 0;
    }

    public boolean esMenorQue(SkillCoins otro) {
        return compareTo(otro) < 0;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    @Override
    public int compareTo(SkillCoins otro) {
        return monto.compareTo(otro.monto);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SkillCoins s && monto.compareTo(s.monto) == 0;
    }

    @Override
    public int hashCode() {
        return monto.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return monto.toPlainString() + " SC";
    }
}
