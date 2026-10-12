package com.uade.skillswap.domain.model.promocion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Promocion")
class PromocionTest {

    @Test
    @DisplayName("una promocion nueva nace activa")
    void nace() {
        Promocion p = Promocion.crear(1L, "d", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null);
        assertTrue(p.isActiva());
        assertNull(p.getId());
        assertEquals(1L, (long) p.getPublicacionId());
        assertEquals(TipoPromocion.PORCENTAJE, p.getTipo());
    }

    @Test
    @DisplayName("sin limites de vigencia siempre esta vigente mientras este activa")
    void sinLimites() {
        Promocion p = Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.TEN, null, null);
        assertTrue(p.estaVigente(AHORA));
        assertTrue(p.estaVigente(AHORA.plusYears(50)));
    }

    @Test
    @DisplayName("la vigencia incluye los extremos y excluye lo anterior y posterior")
    void vigenciaExtremos() {
        Promocion p = Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.TEN, AHORA, AHORA.plusDays(1));
        assertTrue(p.estaVigente(AHORA));
        assertTrue(p.estaVigente(AHORA.plusDays(1)));
        assertTrue(p.estaVigente(AHORA.plusHours(5)));
        assertFalse(p.estaVigente(AHORA.minusSeconds(1)));
        assertFalse(p.estaVigente(AHORA.plusDays(1).plusSeconds(1)));
    }

    @Test
    @DisplayName("solo desde o solo hasta tambien son validos")
    void vigenciaAbierta() {
        Promocion soloDesde = Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.TEN, AHORA, null);
        assertFalse(soloDesde.estaVigente(AHORA.minusDays(1)));
        assertTrue(soloDesde.estaVigente(AHORA.plusDays(400)));
        Promocion soloHasta = Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.TEN, null, AHORA);
        assertTrue(soloHasta.estaVigente(AHORA.minusDays(400)));
        assertFalse(soloHasta.estaVigente(AHORA.plusSeconds(1)));
    }

    @Test
    @DisplayName("desactivar la deja fuera de vigencia")
    void desactivar() {
        Promocion p = Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.TEN, null, null);
        p.desactivar();
        assertFalse(p.isActiva());
        assertFalse(p.estaVigente(AHORA));
    }

    @Test
    @DisplayName("exige publicacion, tipo y valor positivo")
    void invalidas() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Promocion.crear(null, "d", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null));
        assertThrows(ReglaDeNegocioException.class, () -> Promocion.crear(1L, "d", null, BigDecimal.TEN, null, null));
        assertThrows(ReglaDeNegocioException.class,
                () -> Promocion.crear(1L, "d", TipoPromocion.PORCENTAJE, null, null, null));
        assertThrows(ReglaDeNegocioException.class,
                () -> Promocion.crear(1L, "d", TipoPromocion.PORCENTAJE, BigDecimal.ZERO, null, null));
        assertThrows(ReglaDeNegocioException.class,
                () -> Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.valueOf(-1), null, null));
    }

    @Test
    @DisplayName("un porcentaje no puede superar 100 (el monto fijo si puede ser grande)")
    void porcentajeMaximo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Promocion.crear(1L, "d", TipoPromocion.PORCENTAJE, BigDecimal.valueOf(101), null, null));
        assertDoesNotThrow(() -> Promocion.crear(1L, "d", TipoPromocion.PORCENTAJE, BigDecimal.valueOf(100), null, null));
        assertDoesNotThrow(() -> Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO, BigDecimal.valueOf(500), null, null));
    }

    @Test
    @DisplayName("la vigencia no puede terminar antes de empezar")
    void vigenciaInvertida() {
        assertThrows(ReglaDeNegocioException.class, () -> Promocion.crear(1L, "d", TipoPromocion.MONTO_FIJO,
                BigDecimal.TEN, AHORA, AHORA.minusDays(1)));
    }

    @Test
    @DisplayName("reconstruir respeta el flag activa")
    void reconstruir() {
        Promocion p = Promocion.reconstruir(5L, 1L, "d", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null, false);
        assertEquals(5L, (long) p.getId());
        assertFalse(p.estaVigente(AHORA));
    }
}
