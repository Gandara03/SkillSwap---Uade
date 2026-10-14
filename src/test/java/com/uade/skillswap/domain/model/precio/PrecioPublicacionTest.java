package com.uade.skillswap.domain.model.precio;

import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Precio con promociones (Decorator + Simple Factory)")
class PrecioPublicacionTest {

    private static Promocion porcentaje(int valor) {
        return Promocion.crear(1L, valor + "% off", TipoPromocion.PORCENTAJE, BigDecimal.valueOf(valor), null, null);
    }

    private static Promocion montoFijo(int valor) {
        return Promocion.crear(1L, valor + " SC off", TipoPromocion.MONTO_FIJO, BigDecimal.valueOf(valor), null, null);
    }

    @Test
    @DisplayName("sin promociones el precio es el base")
    void sinPromociones() {
        assertEquals(SkillCoins.de(100), PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(), AHORA).calcular());
        assertEquals(SkillCoins.de(100), PrecioPublicacionFactory.construir(SkillCoins.de(100), null, AHORA).calcular());
    }

    @Test
    @DisplayName("una promocion porcentual descuenta ese porcentaje")
    void porcentajeSimple() {
        assertEquals(SkillCoins.de(80),
                PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(porcentaje(20)), AHORA).calcular());
    }

    @Test
    @DisplayName("una promocion de monto fijo resta ese monto")
    void montoFijoSimple() {
        assertEquals(SkillCoins.de(70),
                PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(montoFijo(30)), AHORA).calcular());
    }

    @Test
    @DisplayName("las promociones son acumulables: cada una descuenta sobre el resultado de la anterior")
    void acumulables() {
        // 100 -> -10 % = 90 -> -20 SC = 70
        PrecioPublicacion precio = PrecioPublicacionFactory.construir(SkillCoins.de(100),
                List.of(porcentaje(10), montoFijo(20)), AHORA);
        assertEquals(SkillCoins.de(70), precio.calcular());
        // dos porcentajes: 100 -> 90 -> 81 (no 80)
        assertEquals(SkillCoins.de(81), PrecioPublicacionFactory.construir(SkillCoins.de(100),
                List.of(porcentaje(10), porcentaje(10)), AHORA).calcular());
    }

    @Test
    @DisplayName("el orden de aplicacion importa para porcentaje y monto fijo")
    void ordenImporta() {
        // monto primero: 100 - 20 = 80 -> -10 % = 72
        assertEquals(SkillCoins.de(72), PrecioPublicacionFactory.construir(SkillCoins.de(100),
                List.of(montoFijo(20), porcentaje(10)), AHORA).calcular());
    }

    @Test
    @DisplayName("el precio nunca queda negativo: un descuento mayor al precio deja cero")
    void nuncaNegativo() {
        assertEquals(SkillCoins.CERO,
                PrecioPublicacionFactory.construir(SkillCoins.de(50), List.of(montoFijo(80)), AHORA).calcular());
        assertEquals(SkillCoins.CERO, PrecioPublicacionFactory.construir(SkillCoins.de(50),
                List.of(montoFijo(30), montoFijo(30), montoFijo(30)), AHORA).calcular());
    }

    @Test
    @DisplayName("un 100 % deja el precio en cero")
    void descuentoTotal() {
        assertEquals(SkillCoins.CERO,
                PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(porcentaje(100)), AHORA).calcular());
    }

    @Test
    @DisplayName("un precio base negativo se acota en cero")
    void baseNegativa() {
        assertEquals(SkillCoins.CERO, new PrecioBase(SkillCoins.de(-5)).calcular());
    }

    @Test
    @DisplayName("solo se aplican las promociones vigentes en el instante dado")
    void vigencia() {
        Promocion futura = Promocion.crear(1L, "futura", TipoPromocion.PORCENTAJE, BigDecimal.TEN,
                AHORA.plusDays(1), AHORA.plusDays(2));
        Promocion vencida = Promocion.crear(1L, "vencida", TipoPromocion.PORCENTAJE, BigDecimal.TEN,
                AHORA.minusDays(2), AHORA.minusDays(1));
        Promocion vigente = Promocion.crear(1L, "vigente", TipoPromocion.PORCENTAJE, BigDecimal.TEN,
                AHORA.minusDays(1), AHORA.plusDays(1));
        assertEquals(SkillCoins.de(90), PrecioPublicacionFactory.construir(SkillCoins.de(100),
                List.of(futura, vencida, vigente), AHORA).calcular());
    }

    @Test
    @DisplayName("una promocion desactivada no se aplica")
    void desactivada() {
        Promocion p = porcentaje(50);
        p.desactivar();
        assertEquals(SkillCoins.de(100),
                PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(p), AHORA).calcular());
    }

    @Test
    @DisplayName("el detalle lista el precio base y cada promocion aplicada")
    void detalle() {
        PrecioPublicacion precio = PrecioPublicacionFactory.construir(SkillCoins.de(100),
                List.of(porcentaje(10), montoFijo(5)), AHORA);
        List<String> d = precio.detalle();
        assertEquals(3, d.size());
        assertTrue(d.get(0).contains("Precio base"));
        assertTrue(d.get(1).contains("10%"));
        assertTrue(d.get(2).contains("5.00"));
    }

    @Test
    @DisplayName("calcular es repetible: no acumula estado entre llamadas")
    void idempotente() {
        PrecioPublicacion precio = PrecioPublicacionFactory.construir(SkillCoins.de(100), List.of(porcentaje(10)), AHORA);
        assertEquals(precio.calcular(), precio.calcular());
    }
}
