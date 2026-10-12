package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 9 (JPA/H2): promociones acumulables sin permitir precio negativo (Decorator de precio).
 * Las promociones se persisten y el precio final se recalcula al leer la publicacion.
 */
@DisplayName("Caso 9 (JPA/H2): promociones acumulables sin precio negativo")
class Caso09PromocionesAcumulablesJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 9: porcentaje + monto fijo mayor al precio deja el precio final en 0, nunca negativo")
    void caso09_promocionesAcumulablesNuncaDejanPrecioNegativo() {
        profesional("u-ana", "Ana");
        PublicacionDto mentoria = publicarMentoria("u-ana", 50, 1, 1);
        assertEquals(0, sc(50).compareTo(mentoria.precioFinal()), "sin promociones: precio base");

        PublicacionDto conPorcentaje = promocionar("u-ana", mentoria.id(), TipoPromocion.PORCENTAJE, 20);
        assertEquals(0, sc(40).compareTo(conPorcentaje.precioFinal()));

        PublicacionDto conAmbas = promocionar("u-ana", mentoria.id(), TipoPromocion.MONTO_FIJO, 100);
        assertEquals(0, BigDecimal.ZERO.compareTo(conAmbas.precioFinal()), "precio final: " + conAmbas.precioFinal());
        assertTrue(conAmbas.precioFinal().signum() >= 0);
        assertEquals(0, sc(50).compareTo(conAmbas.precioBase()), "el precio base no se modifica");

        // persistido: al releer la publicacion y en el listado el precio final sigue en 0
        assertEquals(0, BigDecimal.ZERO.compareTo(publicacionVista("u-ana", mentoria.id()).precioFinal()));
        PublicacionResumenDto enBusqueda = ok(app.getBusquedaController().buscar(FiltroBusqueda.vacio(), primeraPagina()))
                .contenido().get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(enBusqueda.precioFinal()));
    }

    @Test
    @DisplayName("Caso 9: dos promociones porcentuales se acumulan (100 -> 90 -> 81)")
    void caso09_dosPorcentajesSeAcumulan() {
        profesional("u-ana", "Ana");
        PublicacionDto mentoria = publicarMentoria("u-ana", 100, 1, 1);

        promocionar("u-ana", mentoria.id(), TipoPromocion.PORCENTAJE, 10);
        PublicacionDto tras = promocionar("u-ana", mentoria.id(), TipoPromocion.PORCENTAJE, 10);

        assertEquals(0, sc(81).compareTo(tras.precioFinal()), "precio final: " + tras.precioFinal());
    }

    @Test
    @DisplayName("Caso 9: con precio final 0 la contratacion se solicita y se paga sin tocar el saldo")
    void caso09_contratarConPrecioFinalCero() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 50, 1, 1);
        promocionar("u-ana", mentoria.id(), TipoPromocion.PORCENTAJE, 20);
        promocionar("u-ana", mentoria.id(), TipoPromocion.MONTO_FIJO, 100);

        ContratacionDto pagada = contratarYPagar("u-ana", "u-bruno", mentoria, 0);

        assertEquals(0, BigDecimal.ZERO.compareTo(pagada.precioFinal()));
        assertEquals("CONFIRMADA", pagada.estado());
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")), "no se cobro nada");
    }
}
