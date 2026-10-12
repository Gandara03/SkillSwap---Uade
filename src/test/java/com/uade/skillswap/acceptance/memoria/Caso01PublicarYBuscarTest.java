package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.RangoPrecio;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 1: publicar una mentoria valida y encontrarla mediante busqueda filtrada.
 * Casos de uso reales (PublicacionUseCase + BusquedaUseCase) sobre repositorios en memoria.
 */
@DisplayName("Caso 1 (memoria): publicar una mentoria valida y encontrarla con busqueda filtrada")
class Caso01PublicarYBuscarTest {

    private static FiltroBusqueda filtro(String categoria, TipoPublicacion tipo, Modalidad modalidad,
                                         RangoPrecio rango, String habilidad, String etiqueta) {
        return new FiltroBusqueda(null, categoria, tipo, modalidad, rango, habilidad, etiqueta, null, null, null,
                CriterioOrden.RECIENTES);
    }

    @Test
    @DisplayName("Caso 1: la mentoria publicada aparece en la busqueda por categoria, tipo, precio y habilidad")
    void caso01_publicarMentoriaValidaYEncontrarlaConBusquedaFiltrada() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");

        // Ana publica una mentoria valida; Bruno publica otra de otra categoria y precio (no debe aparecer)
        PublicacionDto deAna = e.publicar(ana.id(), e.mentoria("Mentoria de Java", "programacion", 80, 3,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java", "backend"), Set.of("java"), e.franjasDiarias(2)));
        PublicacionDto deBruno = e.publicar(bruno.id(), e.mentoria("Mentoria de Diseno", "diseno", 300, 3,
                TipoPoliticaCancelacion.ESTRICTA, Set.of("ux"), Set.of("figma"), e.franjasDiarias(1)));

        assertEquals("PUBLICADA", deAna.estado());
        assertEquals(2, deAna.franjas().size());
        assertNotNull(deAna.publicadaEn());

        // sin filtro: ambas
        Pagina<PublicacionResumenDto> todas = e.app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), e.primeraPagina());
        assertEquals(2L, todas.totalElementos());

        // filtros combinados: categoria + tipo + modalidad + rango de precio + habilidad + etiqueta
        FiltroBusqueda filtrado = filtro("programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA,
                new RangoPrecio(SkillCoins.de(50), SkillCoins.de(100)), "java", "backend");
        Pagina<PublicacionResumenDto> encontradas = e.app.busquedaUseCase.buscar(filtrado, e.primeraPagina());

        assertEquals(1L, encontradas.totalElementos());
        PublicacionResumenDto resumen = encontradas.contenido().get(0);
        assertEquals(deAna.id(), resumen.id());
        assertEquals("Mentoria de Java", resumen.titulo());
        assertEquals(ana.id(), resumen.autorId());
        assertEquals("Ana", resumen.autorNombre());
        assertEquals(0, resumen.precioBase().compareTo(java.math.BigDecimal.valueOf(80)));
        assertEquals("PUBLICADA", resumen.estado());
        assertNotEquals(deAna.id(), deBruno.id(), "las dos publicaciones tienen ids distintos");
    }

    @Test
    @DisplayName("Caso 1: cada filtro por separado deja afuera lo que no coincide")
    void caso01_cadaFiltroExcluyeLoQueNoCoincide() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto deJava = e.publicar(ana.id(), e.mentoria("Mentoria de Java", "programacion", 80, 3,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), e.franjasDiarias(1)));

        assertEquals(1L, e.app.busquedaUseCase.buscar(
                filtro("programacion", null, null, null, null, null), e.primeraPagina()).totalElementos());
        assertEquals(0L, e.app.busquedaUseCase.buscar(
                filtro("cocina", null, null, null, null, null), e.primeraPagina()).totalElementos());
        assertEquals(0L, e.app.busquedaUseCase.buscar(
                filtro(null, TipoPublicacion.SERVICIO, null, null, null, null), e.primeraPagina()).totalElementos());
        assertEquals(0L, e.app.busquedaUseCase.buscar(
                filtro(null, null, Modalidad.PRESENCIAL, null, null, null), e.primeraPagina()).totalElementos());
        assertEquals(0L, e.app.busquedaUseCase.buscar(
                filtro(null, null, null, new RangoPrecio(SkillCoins.de(200), null), null, null),
                e.primeraPagina()).totalElementos());
        assertEquals(0L, e.app.busquedaUseCase.buscar(
                filtro(null, null, null, null, "python", null), e.primeraPagina()).totalElementos());
        List<Long> ids = e.app.busquedaUseCase.buscar(
                filtro(null, null, null, new RangoPrecio(null, SkillCoins.de(80)), "java", null),
                e.primeraPagina()).contenido().stream().map(PublicacionResumenDto::id).toList();
        assertEquals(List.of(deJava.id()), ids);
    }

    @Test
    @DisplayName("Caso 1: un borrador todavia no se encuentra; al publicarlo si")
    void caso01_soloLasPublicadasSeEncuentran() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto borrador = e.app.publicacionUseCase.crearBorrador(ana.id(), e.mentoria("Mentoria de Java",
                "programacion", 80, 3, TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"),
                e.franjasDiarias(1)));
        assertEquals("BORRADOR", borrador.estado());

        FiltroBusqueda filtro = filtro("programacion", null, null, null, null, null);
        assertEquals(0L, e.app.busquedaUseCase.buscar(filtro, e.primeraPagina()).totalElementos());

        e.app.publicacionUseCase.publicar(ana.id(), borrador.id());
        Pagina<PublicacionResumenDto> tras = e.app.busquedaUseCase.buscar(filtro, e.primeraPagina());
        assertEquals(1L, tras.totalElementos());
        assertEquals(borrador.id(), tras.contenido().get(0).id());
        assertTrue(tras.contenido().stream().noneMatch(r -> !"PUBLICADA".equals(r.estado())));
        assertFalse(tras.contenido().isEmpty());
    }
}
