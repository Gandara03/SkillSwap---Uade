package com.uade.skillswap.acceptance.jpa;

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Caso de aceptacion 1 (JPA/H2): publicar una mentoria valida y encontrarla mediante busqueda filtrada.
 * La busqueda corre contra la base real (JPQL parametrizado, paginado y con una consulta por lote para los autores).
 */
@DisplayName("Caso 1 (JPA/H2): publicar una mentoria valida y encontrarla con busqueda filtrada")
class Caso01PublicarYBuscarJpaTest extends AceptacionJpaBase {

    private static FiltroBusqueda filtro(String categoria, TipoPublicacion tipo, Modalidad modalidad,
                                         RangoPrecio rango, String habilidad, String etiqueta) {
        return new FiltroBusqueda(null, categoria, tipo, modalidad, rango, habilidad, etiqueta, null, null, null,
                CriterioOrden.RECIENTES);
    }

    private Pagina<PublicacionResumenDto> buscar(FiltroBusqueda filtro) {
        return ok(app.getBusquedaController().buscar(filtro, primeraPagina()));
    }

    @Test
    @DisplayName("Caso 1: la mentoria publicada aparece en la busqueda por categoria, tipo, precio, habilidad y etiqueta")
    void caso01_publicarMentoriaValidaYEncontrarlaConBusquedaFiltrada() {
        PerfilDto ana = profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto deAna = publicar("u-ana", mentoria("Mentoria de Java", "programacion", 80, 3,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java", "backend"), Set.of("java"), franjasDiarias(2)));
        PublicacionDto deBruno = publicar("u-bruno", mentoria("Mentoria de Diseno", "diseno", 300, 3,
                TipoPoliticaCancelacion.ESTRICTA, Set.of("ux"), Set.of("figma"), franjasDiarias(1)));

        assertEquals("PUBLICADA", deAna.estado());
        assertEquals(2, deAna.franjas().size());
        assertNotNull(deAna.publicadaEn());
        assertNotEquals(deAna.id(), deBruno.id());
        assertEquals(2L, buscar(FiltroBusqueda.vacio()).totalElementos());

        Pagina<PublicacionResumenDto> encontradas = buscar(filtro("programacion", TipoPublicacion.MENTORIA,
                Modalidad.REMOTA, new RangoPrecio(SkillCoins.de(50), SkillCoins.de(100)), "java", "backend"));

        assertEquals(1L, encontradas.totalElementos());
        PublicacionResumenDto resumen = encontradas.contenido().get(0);
        assertEquals(deAna.id(), resumen.id());
        assertEquals("Mentoria de Java", resumen.titulo());
        assertEquals(ana.id(), resumen.autorId());
        assertEquals("Ana", resumen.autorNombre());
        assertEquals(0, resumen.precioBase().compareTo(sc(80)));
        assertEquals("PUBLICADA", resumen.estado());
    }

    @Test
    @DisplayName("Caso 1: cada filtro por separado deja afuera lo que no coincide")
    void caso01_cadaFiltroExcluyeLoQueNoCoincide() {
        profesional("u-ana", "Ana");
        PublicacionDto deJava = publicar("u-ana", mentoria("Mentoria de Java", "programacion", 80, 3,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), franjasDiarias(1)));

        assertEquals(1L, buscar(filtro("programacion", null, null, null, null, null)).totalElementos());
        assertEquals(0L, buscar(filtro("cocina", null, null, null, null, null)).totalElementos());
        assertEquals(0L, buscar(filtro(null, TipoPublicacion.SERVICIO, null, null, null, null)).totalElementos());
        assertEquals(0L, buscar(filtro(null, null, Modalidad.PRESENCIAL, null, null, null)).totalElementos());
        assertEquals(0L, buscar(filtro(null, null, null, new RangoPrecio(SkillCoins.de(200), null), null, null))
                .totalElementos());
        assertEquals(0L, buscar(filtro(null, null, null, null, "python", null)).totalElementos());
        assertEquals(List.of(deJava.id()), buscar(filtro(null, null, null,
                new RangoPrecio(null, SkillCoins.de(80)), "java", null)).contenido().stream()
                .map(PublicacionResumenDto::id).toList());
    }

    @Test
    @DisplayName("Caso 1: un borrador todavia no se encuentra; al publicarlo si")
    void caso01_soloLasPublicadasSeEncuentran() {
        profesional("u-ana", "Ana");
        como("u-ana");
        PublicacionDto borrador = ok(app.getPublicacionController().crearBorrador(mentoria("Mentoria de Java",
                "programacion", 80, 3, TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"),
                franjasDiarias(1))));
        assertEquals("BORRADOR", borrador.estado());
        FiltroBusqueda filtro = filtro("programacion", null, null, null, null, null);
        assertEquals(0L, buscar(filtro).totalElementos());

        ok(app.getPublicacionController().publicar(borrador.id()));

        Pagina<PublicacionResumenDto> tras = buscar(filtro);
        assertEquals(1L, tras.totalElementos());
        assertEquals(borrador.id(), tras.contenido().get(0).id());
    }
}
