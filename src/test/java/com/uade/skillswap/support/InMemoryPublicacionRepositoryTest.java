package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.RangoPrecio;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica el fake: los tests de servicios confian en que filtra, ordena y pagina como el repositorio real. */
@DisplayName("InMemoryPublicacionRepository (fake de busqueda)")
class InMemoryPublicacionRepositoryTest {

    private InMemoryPerfilRepository perfiles;
    private InMemoryPublicacionRepository repo;
    private Perfil ana;
    private Perfil bruno;

    @BeforeEach
    void preparar() {
        perfiles = new InMemoryPerfilRepository();
        repo = new InMemoryPublicacionRepository(perfiles);
        ana = perfiles.guardar(Perfil.crear("u-ana", "Ana", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA));
        bruno = perfiles.guardar(Perfil.crear("u-bruno", "Bruno", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA));
        Datos.asignarId(ana, ana.getId());
        reputacion(ana, 5);
        reputacion(bruno, 2);
    }

    private void reputacion(Perfil p, int puntaje) {
        com.uade.skillswap.domain.model.contratacion.Contratacion c = Datos.completada();
        p.recalcularReputacion(List.of(Datos.calificacion(c, Datos.CLIENTE, puntaje)));
    }

    private Publicacion nueva(Perfil autor, String titulo, String categoria, TipoPublicacion tipo, Modalidad modalidad,
                              int precio, List<String> etiquetas, List<Habilidad> habilidades, LocalDateTime franjaInicio,
                              boolean publicar) {
        Publicacion p = Publicacion.crearBorrador(tipo, autor.getId(), TipoPerfil.PROFESIONAL, titulo, "descripcion de " + titulo,
                categoria, modalidad, 60, 1, SkillCoins.de(precio), etiquetas, habilidades,
                TipoPoliticaCancelacion.FLEXIBLE, AHORA);
        p.agregarFranja(franjaInicio, franjaInicio.plusHours(1), AHORA);
        repo.guardar(p);
        if (publicar) {
            p.publicar(autor.getId(), AHORA.plusMinutes(repo.cantidad()));
        }
        return p;
    }

    private Publicacion simple(Perfil autor, String titulo, int precio) {
        return nueva(autor, titulo, "programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA, precio, List.of(),
                List.of(), AHORA.plusDays(2), true);
    }

    private List<String> titulos(Pagina<Publicacion> pagina) {
        return pagina.contenido().stream().map(Publicacion::getTitulo).collect(Collectors.toList());
    }

    private FiltroBusqueda filtro(String texto, String categoria, TipoPublicacion tipo, Modalidad modalidad,
                                  RangoPrecio rango, String habilidad, String etiqueta, Double repMin,
                                  LocalDateTime desde, LocalDateTime hasta, CriterioOrden orden) {
        return new FiltroBusqueda(texto, categoria, tipo, modalidad, rango, habilidad, etiqueta, repMin, desde, hasta, orden);
    }

    private static final PaginaRequest TODAS = PaginaRequest.de(0, 50);

    @Test
    @DisplayName("guardar asigna ids a la publicacion y a sus franjas")
    void asignaIds() {
        Publicacion p = simple(ana, "A", 10);
        assertNotNull(p.getId());
        assertNotNull(p.getFranjas().get(0).getId());
        assertSame(p, repo.buscarPorId(p.getId()).orElseThrow());
        assertTrue(repo.buscarPorId(999L).isEmpty());
    }

    @Test
    @DisplayName("solo devuelve publicaciones PUBLICADAS")
    void soloPublicadas() {
        simple(ana, "Publicada", 10);
        nueva(ana, "Borrador", "programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(),
                AHORA.plusDays(2), false);
        Publicacion pausada = simple(ana, "Pausada", 10);
        pausada.pausar(ana.getId(), AHORA);
        assertEquals(List.of("Publicada"), titulos(repo.buscar(FiltroBusqueda.vacio(), TODAS)));
    }

    @Test
    @DisplayName("filtra por texto en titulo o descripcion sin distinguir mayusculas")
    void filtroTexto() {
        simple(ana, "Clases de Java", 10);
        simple(ana, "Cocina", 10);
        assertEquals(List.of("Clases de Java"), titulos(repo.buscar(
                filtro("jAVA", null, null, null, null, null, null, null, null, null, null), TODAS)));
        assertEquals(2, repo.buscar(filtro("descripcion", null, null, null, null, null, null, null, null, null, null), TODAS)
                .totalElementos());
    }

    @Test
    @DisplayName("filtra por categoria, tipo y modalidad")
    void filtroCategoriaTipoModalidad() {
        nueva(ana, "M1", "programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(), AHORA.plusDays(2), true);
        nueva(ana, "S1", "diseno", TipoPublicacion.SERVICIO, Modalidad.PRESENCIAL, 10, List.of(), List.of(), AHORA.plusDays(2), true);
        assertEquals(List.of("S1"), titulos(repo.buscar(filtro(null, "DISENO", null, null, null, null, null, null, null, null, null), TODAS)));
        assertEquals(List.of("S1"), titulos(repo.buscar(filtro(null, null, TipoPublicacion.SERVICIO, null, null, null, null, null, null, null, null), TODAS)));
        assertEquals(List.of("M1"), titulos(repo.buscar(filtro(null, null, null, Modalidad.REMOTA, null, null, null, null, null, null, null), TODAS)));
    }

    @Test
    @DisplayName("filtra por rango de precio incluyendo los extremos")
    void filtroPrecio() {
        simple(ana, "barata", 5);
        simple(ana, "media", 50);
        simple(ana, "cara", 500);
        RangoPrecio rango = new RangoPrecio(SkillCoins.de(5), SkillCoins.de(50));
        assertEquals(2, repo.buscar(filtro(null, null, null, null, rango, null, null, null, null, null, null), TODAS).totalElementos());
    }

    @Test
    @DisplayName("filtra por habilidad (normalizada) y por etiqueta")
    void filtroHabilidadEtiqueta() {
        nueva(ana, "Java", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of("backend"), List.of(Habilidad.de("Java")), AHORA.plusDays(2), true);
        nueva(ana, "Go", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of("infra"), List.of(Habilidad.de("Go")), AHORA.plusDays(2), true);
        assertEquals(List.of("Java"), titulos(repo.buscar(filtro(null, null, null, null, null, " JAVA", null, null, null, null, null), TODAS)));
        assertEquals(List.of("Go"), titulos(repo.buscar(filtro(null, null, null, null, null, null, "Infra", null, null, null, null), TODAS)));
    }

    @Test
    @DisplayName("filtra por reputacion minima del autor")
    void filtroReputacion() {
        simple(ana, "de Ana", 10);
        simple(bruno, "de Bruno", 10);
        assertEquals(List.of("de Ana"), titulos(repo.buscar(filtro(null, null, null, null, null, null, null, 4.0, null, null, null), TODAS)));
        assertEquals(2, repo.buscar(filtro(null, null, null, null, null, null, null, 2.0, null, null, null), TODAS).totalElementos());
    }

    @Test
    @DisplayName("filtra por disponibilidad: franja LIBRE dentro de la ventana pedida")
    void filtroDisponibilidad() {
        nueva(ana, "en 2 dias", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(), AHORA.plusDays(2), true);
        nueva(ana, "en 10 dias", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(), AHORA.plusDays(10), true);
        Publicacion ocupada = nueva(ana, "ocupada", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(), AHORA.plusDays(3), true);
        ocupada.reservarFranja(ocupada.getFranjas().get(0).getId(), AHORA);

        assertEquals(List.of("en 10 dias"), titulos(repo.buscar(filtro(null, null, null, null, null, null, null, null,
                AHORA.plusDays(5), null, null), TODAS)));
        assertEquals(List.of("en 2 dias"), titulos(repo.buscar(filtro(null, null, null, null, null, null, null, null,
                null, AHORA.plusDays(5), CriterioOrden.RECIENTES), TODAS)),
                "la franja ocupada no cuenta como disponible");
        assertEquals(1, repo.buscar(filtro(null, null, null, null, null, null, null, null,
                AHORA.plusDays(1), AHORA.plusDays(4), null), TODAS).totalElementos());
    }

    @Test
    @DisplayName("ordena por precio ascendente y descendente")
    void ordenPrecio() {
        simple(ana, "B-20", 20);
        simple(ana, "A-10", 10);
        simple(ana, "C-30", 30);
        assertEquals(List.of("A-10", "B-20", "C-30"), titulos(repo.buscar(
                filtro(null, null, null, null, null, null, null, null, null, null, CriterioOrden.PRECIO_ASC), TODAS)));
        assertEquals(List.of("C-30", "B-20", "A-10"), titulos(repo.buscar(
                filtro(null, null, null, null, null, null, null, null, null, null, CriterioOrden.PRECIO_DESC), TODAS)));
    }

    @Test
    @DisplayName("ordena por reputacion del autor descendente")
    void ordenReputacion() {
        simple(bruno, "de Bruno", 10);
        simple(ana, "de Ana", 10);
        assertEquals(List.of("de Ana", "de Bruno"), titulos(repo.buscar(
                filtro(null, null, null, null, null, null, null, null, null, null, CriterioOrden.REPUTACION), TODAS)));
    }

    @Test
    @DisplayName("RECIENTES (por defecto) pone primero la publicada mas recientemente")
    void ordenRecientes() {
        simple(ana, "primera", 10);
        simple(ana, "segunda", 10);
        simple(ana, "tercera", 10);
        assertEquals(List.of("tercera", "segunda", "primera"), titulos(repo.buscar(FiltroBusqueda.vacio(), TODAS)));
    }

    @Test
    @DisplayName("pagina de verdad: tamano, offset, total y ultima pagina parcial")
    void paginacion() {
        for (int i = 1; i <= 5; i++) {
            simple(ana, "P" + i, i * 10);
        }
        FiltroBusqueda porPrecio = filtro(null, null, null, null, null, null, null, null, null, null, CriterioOrden.PRECIO_ASC);
        Pagina<Publicacion> p0 = repo.buscar(porPrecio, PaginaRequest.de(0, 2));
        Pagina<Publicacion> p1 = repo.buscar(porPrecio, PaginaRequest.de(1, 2));
        Pagina<Publicacion> p2 = repo.buscar(porPrecio, PaginaRequest.de(2, 2));
        assertEquals(List.of("P1", "P2"), titulos(p0));
        assertEquals(List.of("P3", "P4"), titulos(p1));
        assertEquals(List.of("P5"), titulos(p2));
        assertEquals(5L, p0.totalElementos());
        assertEquals(3, p0.totalPaginas());
        assertTrue(p0.tieneSiguiente());
        assertFalse(p2.tieneSiguiente());
        assertTrue(repo.buscar(porPrecio, PaginaRequest.de(9, 2)).contenido().isEmpty());
    }

    @Test
    @DisplayName("los filtros se combinan con AND")
    void filtrosCombinados() {
        nueva(ana, "Java remoto", "programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(Habilidad.de("Java")), AHORA.plusDays(2), true);
        nueva(ana, "Java presencial", "programacion", TipoPublicacion.MENTORIA, Modalidad.PRESENCIAL, 10, List.of(), List.of(Habilidad.de("Java")), AHORA.plusDays(2), true);
        assertEquals(List.of("Java remoto"), titulos(repo.buscar(
                filtro("java", "programacion", null, Modalidad.REMOTA, null, "java", null, 4.0, null, null, null), TODAS)));
    }

    @Test
    @DisplayName("buscarPorAutor incluye todos los estados y buscarPublicadas respeta el limite")
    void autorYPublicadas() {
        simple(ana, "uno", 10);
        simple(ana, "dos", 10);
        nueva(ana, "borrador", "x", TipoPublicacion.MENTORIA, Modalidad.REMOTA, 10, List.of(), List.of(), AHORA.plusDays(2), false);
        simple(bruno, "de bruno", 10);
        assertEquals(3L, repo.buscarPorAutor(ana.getId(), TODAS).totalElementos());
        assertEquals(1L, repo.buscarPorAutor(bruno.getId(), TODAS).totalElementos());
        assertEquals(3, repo.buscarPublicadas(10).size());
        assertEquals(2, repo.buscarPublicadas(2).size());
    }
}
