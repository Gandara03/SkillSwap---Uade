package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillCoins (Value Object)")
class SkillCoinsTest {

    @Test
    @DisplayName("se normaliza a escala 2 y la igualdad es por valor, no por escala")
    void igualdadPorValor() {
        assertEquals(SkillCoins.de(10), SkillCoins.de("10.00"));
        assertEquals(SkillCoins.de(10).hashCode(), SkillCoins.de(new BigDecimal("10.0")).hashCode());
        assertEquals(2, SkillCoins.de(5).getMonto().scale());
        assertNotEquals(SkillCoins.de(10), SkillCoins.de(11));
    }

    @Test
    @DisplayName("redondea half-up a dos decimales")
    void redondeo() {
        assertEquals(new BigDecimal("1.01"), SkillCoins.de("1.005").getMonto());
        assertEquals(new BigDecimal("1.00"), SkillCoins.de("1.004").getMonto());
    }

    @Test
    @DisplayName("no acepta un monto BigDecimal nulo")
    void montoNulo() {
        assertThrows(ReglaDeNegocioException.class, () -> SkillCoins.de((BigDecimal) null));
    }

    @Test
    @DisplayName("suma y resta devuelven nuevos valores sin mutar los originales")
    void sumarYRestar() {
        SkillCoins a = SkillCoins.de(30);
        SkillCoins b = SkillCoins.de("12.50");
        assertEquals(SkillCoins.de("42.50"), a.sumar(b));
        assertEquals(SkillCoins.de("17.50"), a.restar(b));
        assertEquals(SkillCoins.de(30), a);
        assertEquals(SkillCoins.de("12.50"), b);
    }

    @Test
    @DisplayName("la resta puede dar negativo como resultado intermedio")
    void restaNegativa() {
        SkillCoins r = SkillCoins.de(5).restar(SkillCoins.de(8));
        assertTrue(r.esNegativo());
        assertEquals(SkillCoins.de(-3), r);
    }

    @Test
    @DisplayName("noNegativo acota los negativos en cero y deja intactos los positivos")
    void noNegativo() {
        assertSame(SkillCoins.CERO, SkillCoins.de(-1).noNegativo());
        SkillCoins positivo = SkillCoins.de(4);
        assertSame(positivo, positivo.noNegativo());
        assertEquals(SkillCoins.CERO, SkillCoins.CERO.noNegativo());
    }

    @Test
    @DisplayName("porcentaje calcula la fraccion indicada del monto")
    void porcentaje() {
        assertEquals(SkillCoins.de(50), SkillCoins.de(100).porcentaje(50));
        assertEquals(SkillCoins.de("12.50"), SkillCoins.de(50).porcentaje(25));
        assertEquals(SkillCoins.CERO, SkillCoins.de(100).porcentaje(0));
        assertEquals(SkillCoins.de(100), SkillCoins.de(100).porcentaje(100));
        assertEquals(SkillCoins.de("3.33"), SkillCoins.de(10).porcentaje(new BigDecimal("33.3333")));
    }

    @Test
    @DisplayName("comparaciones: mayor, menor, cero y negativo")
    void comparaciones() {
        SkillCoins diez = SkillCoins.de(10);
        SkillCoins veinte = SkillCoins.de(20);
        assertTrue(veinte.esMayorQue(diez));
        assertFalse(diez.esMayorQue(veinte));
        assertTrue(diez.esMenorQue(veinte));
        assertFalse(diez.esMenorQue(diez));
        assertFalse(diez.esMayorQue(diez));
        assertTrue(SkillCoins.CERO.esCero());
        assertFalse(diez.esCero());
        assertFalse(SkillCoins.CERO.esNegativo());
        assertTrue(diez.compareTo(veinte) < 0);
        assertEquals(0, diez.compareTo(SkillCoins.de("10.00")));
    }

    @Test
    @DisplayName("toString expone el monto con la unidad SC")
    void textual() {
        assertEquals("10.00 SC", SkillCoins.de(10).toString());
    }

    @Test
    @DisplayName("el cero es el elemento neutro de la suma")
    void ceroNeutro() {
        assertEquals(SkillCoins.de(7), SkillCoins.de(7).sumar(SkillCoins.CERO));
    }
}
