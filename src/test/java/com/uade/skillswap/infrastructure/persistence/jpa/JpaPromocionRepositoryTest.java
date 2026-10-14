package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PromocionJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaPromocionRepositoryTest extends JpaTestBase {

    private Publicacion publicacion;
    private Publicacion otra;

    @BeforeEach
    void crearPublicaciones() {
        Perfil ana = profesional("u-ana");
        publicacion = publicacion(ana).guardar();
        otra = publicacion(ana).titulo("Otra").guardar();
    }

    @Test
    @DisplayName("guardar y recuperar promociones activas: tipo, valor, vigencia y orden de creacion")
    void guardarYRecuperarActivas() {
        Promocion porcentaje = promociones.guardar(Promocion.crear(publicacion.getId(), "10% de descuento",
                TipoPromocion.PORCENTAJE, new BigDecimal("10"), AHORA, AHORA.plusDays(7)));
        Promocion fijo = promociones.guardar(Promocion.crear(publicacion.getId(), "25 SC menos",
                TipoPromocion.MONTO_FIJO, new BigDecimal("25"), null, null));

        assertNotNull(porcentaje.getId());
        List<Promocion> activas = promociones.buscarActivasPorPublicacion(publicacion.getId());
        assertEquals(2, activas.size());
        Promocion p = activas.get(0);
        assertEquals(porcentaje.getId(), p.getId());
        assertEquals("10% de descuento", p.getDescripcion());
        assertEquals(TipoPromocion.PORCENTAJE, p.getTipo());
        assertEquals(0, new BigDecimal("10").compareTo(p.getValor()));
        assertEquals(AHORA, p.getVigenteDesde());
        assertEquals(AHORA.plusDays(7), p.getVigenteHasta());
        assertTrue(p.isActiva());
        assertTrue(p.estaVigente(AHORA.plusDays(1)));
        Promocion f = activas.get(1);
        assertEquals(fijo.getId(), f.getId());
        assertEquals(TipoPromocion.MONTO_FIJO, f.getTipo());
        assertNull(f.getVigenteDesde());
        assertNull(f.getVigenteHasta());
    }

    @Test
    @DisplayName("una promocion desactivada deja de aparecer entre las activas")
    void desactivar() {
        Promocion promo = promociones.guardar(Promocion.crear(publicacion.getId(), "Promo", TipoPromocion.PORCENTAJE,
                new BigDecimal("15"), null, null));
        assertEquals(1, promociones.buscarActivasPorPublicacion(publicacion.getId()).size());

        promo.desactivar();
        promociones.guardar(promo);

        assertTrue(promociones.buscarActivasPorPublicacion(publicacion.getId()).isEmpty());
        assertEquals(1, contarFilas(ctx, PromocionJpaEntity.class));
        assertFalse(ctx.leer(em -> em.find(PromocionJpaEntity.class, promo.getId())).isActiva());
    }

    @Test
    @DisplayName("las promociones de una publicacion no se mezclan con las de otra")
    void aisladasPorPublicacion() {
        promociones.guardar(Promocion.crear(publicacion.getId(), "A", TipoPromocion.PORCENTAJE, new BigDecimal("5"), null, null));
        promociones.guardar(Promocion.crear(otra.getId(), "B", TipoPromocion.MONTO_FIJO, new BigDecimal("8"), null, null));

        assertEquals("A", promociones.buscarActivasPorPublicacion(publicacion.getId()).get(0).getDescripcion());
        assertEquals("B", promociones.buscarActivasPorPublicacion(otra.getId()).get(0).getDescripcion());
        assertTrue(promociones.buscarActivasPorPublicacion(9999L).isEmpty());
    }
}
