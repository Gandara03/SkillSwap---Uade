package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 9: aplicar promociones acumulables sin permitir precio negativo (Decorator de precio).
 * Las promociones se cargan por PublicacionUseCase.agregarPromocion y se verifica el precio final del DTO.
 */
@DisplayName("Caso 9 (memoria): promociones acumulables sin precio negativo")
class Caso09PromocionesAcumulablesTest {

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    @Test
    @DisplayName("Caso 9: porcentaje + monto fijo mayor al precio deja el precio final en 0, nunca negativo")
    void caso09_promocionesAcumulablesNuncaDejanPrecioNegativo() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 50, 1, 1);
        assertEquals(0, sc(50).compareTo(mentoria.precioFinal()), "sin promociones: precio base");

        // 20 % de descuento: 50 -> 40
        PublicacionDto conPorcentaje = e.promocionar(ana.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 20);
        assertEquals(0, sc(40).compareTo(conPorcentaje.precioFinal()));

        // descuento fijo de 100 (mayor que el precio): se acumula y el total se clampea en 0
        PublicacionDto conAmbas = e.promocionar(ana.id(), mentoria.id(), TipoPromocion.MONTO_FIJO, 100);
        assertEquals(0, BigDecimal.ZERO.compareTo(conAmbas.precioFinal()), "precio final: " + conAmbas.precioFinal());
        assertTrue(conAmbas.precioFinal().signum() >= 0);
        assertEquals(0, sc(50).compareTo(conAmbas.precioBase()), "el precio base no se modifica");
        assertEquals(2, conAmbas.detallePrecio().stream().filter(l -> l.contains("Promocion")).count(),
                "el detalle muestra las dos promociones: " + conAmbas.detallePrecio());

        // el mismo precio final se ve al consultar y en los listados
        assertEquals(0, BigDecimal.ZERO.compareTo(e.app.publicacionUseCase.obtener(ana.id(), mentoria.id()).precioFinal()));
        PublicacionResumenDto enBusqueda = e.app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), e.primeraPagina())
                .contenido().get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(enBusqueda.precioFinal()));
    }

    @Test
    @DisplayName("Caso 9: el resultado no depende del orden en que se cargan las promociones")
    void caso09_elOrdenDeCargaNoImporta() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 50, 1, 1);

        e.promocionar(ana.id(), mentoria.id(), TipoPromocion.MONTO_FIJO, 100);
        PublicacionDto tras = e.promocionar(ana.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 20);

        assertEquals(0, BigDecimal.ZERO.compareTo(tras.precioFinal()));
    }

    @Test
    @DisplayName("Caso 9: dos promociones porcentuales se acumulan (100 -> 90 -> 81)")
    void caso09_dosPorcentajesSeAcumulan() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 100, 1, 1);

        e.promocionar(ana.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 10);
        PublicacionDto tras = e.promocionar(ana.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 10);

        assertEquals(0, sc(81).compareTo(tras.precioFinal()), "precio final: " + tras.precioFinal());
    }

    @Test
    @DisplayName("Caso 9: una promocion fuera de su vigencia no se aplica")
    void caso09_unaPromocionVencidaNoSeAplica() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 50, 1, 1);

        PublicacionDto tras = e.app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(
                mentoria.id(), "Vencida", TipoPromocion.MONTO_FIJO, sc(30),
                e.ahora().minusDays(10), e.ahora().minusDays(1)));

        assertEquals(0, sc(50).compareTo(tras.precioFinal()));
    }

    @Test
    @DisplayName("Caso 9: con precio final 0 la contratacion se solicita y se paga sin tocar el saldo")
    void caso09_contratarConPrecioFinalCero() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 50, 1, 1);
        e.promocionar(ana.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 20);
        e.promocionar(ana.id(), mentoria.id(), TipoPromocion.MONTO_FIJO, 100);

        ContratacionDto pagada = e.contratarYPagar(ana.id(), bruno.id(), mentoria, 0);

        assertEquals(0, BigDecimal.ZERO.compareTo(pagada.precioFinal()));
        assertEquals("CONFIRMADA", pagada.estado());
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())), "no se cobro nada");
    }

    @Test
    @DisplayName("Caso 9: solo el autor puede agregar promociones a su publicacion")
    void caso09_soloElAutorPromociona() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 50, 1, 1);

        assertThrows(com.uade.skillswap.application.exception.NoAutorizadoException.class,
                () -> e.promocionar(bruno.id(), mentoria.id(), TipoPromocion.PORCENTAJE, 50));
        assertEquals(0, sc(50).compareTo(e.app.publicacionUseCase.obtener(ana.id(), mentoria.id()).precioFinal()));
    }
}
