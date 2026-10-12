package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.RangoPrecio;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusquedaService: filtros combinables, orden, paginacion y armado de resumenes por lote")
class BusquedaServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;
    private PerfilDto bruno;
    private PerfilDto techcorp;

    /**
     * Catalogo (todas PUBLICADAS salvo indicacion):
     * Java (ana, MENTORIA, programacion, REMOTA, 40, franjas 100/130 h), SQL (ana, SERVICIO, datos, PRESENCIAL, 80, 200/220 h),
     * Kotlin (bruno, MENTORIA, programacion, REMOTA, 20, 300 h), Microservicios (techcorp, DESAFIO, arquitectura, PRESENCIAL, 10),
     * mas un borrador, una pausada y una cerrada que nunca deben aparecer.
     */
    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        techcorp = e.organizacion("u-techcorp", "TechCorp");

        e.publicada(ana, e.oferta().titulo("Mentoria de Java").descripcion("Clean Architecture y patrones")
                .categoria("programacion").precio("40").etiquetas("java", "backend").habilidades("java").franjasEn(100L, 130L));
        e.avanzarHoras(1);
        e.publicada(ana, e.oferta().tipo(TipoPublicacion.SERVICIO).titulo("Taller de SQL").descripcion("Consultas y modelado")
                .categoria("datos").modalidad(Modalidad.PRESENCIAL).precio("80").etiquetas("bases").habilidades("sql").franjasEn(200L, 220L));
        e.avanzarHoras(1);
        e.publicada(bruno, e.oferta().titulo("Clases de Kotlin").descripcion("Kotlin desde cero")
                .categoria("programacion").precio("20").etiquetas("kotlin", "backend").habilidades("kotlin").franjasEn(300L));
        e.avanzarHoras(1);
        e.publicada(techcorp, e.oferta().tipo(TipoPublicacion.DESAFIO).titulo("Desafio de microservicios")
                .descripcion("Disena un sistema distribuido").categoria("arquitectura").modalidad(Modalidad.PRESENCIAL)
                .precio("10").etiquetas("cloud").habilidades("microservicios").franjasEn(400L));
        e.borrador(ana, e.oferta().titulo("Borrador oculto"));
        PublicacionDto pausada = e.publicada(bruno, e.oferta().titulo("Pausada oculta"));
        app.publicacionUseCase.pausar(bruno.id(), pausada.id());
        PublicacionDto cerrada = e.publicada(bruno, e.oferta().titulo("Cerrada oculta"));
        app.publicacionUseCase.cerrar(bruno.id(), cerrada.id());
    }

    private List<String> titulos(FiltroBusqueda filtro) {
        return app.busquedaUseCase.buscar(filtro, PaginaRequest.de(0, 50)).contenido().stream()
                .map(PublicacionResumenDto::titulo).toList();
    }

    private FiltroBusqueda filtro(String texto, String categoria, TipoPublicacion tipo, Modalidad modalidad,
                                  RangoPrecio precio, String habilidad, String etiqueta, Double reputacion,
                                  CriterioOrden orden) {
        return new FiltroBusqueda(texto, categoria, tipo, modalidad, precio, habilidad, etiqueta, reputacion, null, null, orden);
    }

    // ---------- que se busca ----------

    @Test
    @DisplayName("sin filtros devuelve solo las PUBLICADAS (no borradores, pausadas ni cerradas), de la mas reciente a la mas antigua")
    void soloPublicadas() {
        List<String> encontradas = titulos(FiltroBusqueda.vacio());

        assertEquals(List.of("Desafio de microservicios", "Clases de Kotlin", "Taller de SQL", "Mentoria de Java"), encontradas);
    }

    @Test
    @DisplayName("un filtro nulo equivale a no filtrar")
    void filtroNulo() {
        assertEquals(titulos(FiltroBusqueda.vacio()), titulos(null));
    }

    @Test
    @DisplayName("filtra por texto en titulo o descripcion, sin distinguir mayusculas")
    void filtroTexto() {
        assertEquals(List.of("Mentoria de Java"), titulos(filtro("CLEAN arch", null, null, null, null, null, null, null, null)));
        assertEquals(List.of("Taller de SQL"), titulos(filtro("sql", null, null, null, null, null, null, null, null)));
        assertTrue(titulos(filtro("inexistente", null, null, null, null, null, null, null, null)).isEmpty());
    }

    @Test
    @DisplayName("filtra por categoria, tipo y modalidad")
    void filtroCategoriaTipoModalidad() {
        assertEquals(List.of("Clases de Kotlin", "Mentoria de Java"),
                titulos(filtro(null, "programacion", null, null, null, null, null, null, null)));
        assertEquals(List.of("Desafio de microservicios"),
                titulos(filtro(null, null, TipoPublicacion.DESAFIO, null, null, null, null, null, null)));
        assertEquals(List.of("Taller de SQL"),
                titulos(filtro(null, null, TipoPublicacion.SERVICIO, null, null, null, null, null, null)));
        assertEquals(List.of("Desafio de microservicios", "Taller de SQL"),
                titulos(filtro(null, null, null, Modalidad.PRESENCIAL, null, null, null, null, null)));
    }

    @Test
    @DisplayName("filtra por rango de precio, con extremos incluidos y abiertos")
    void filtroPrecio() {
        RangoPrecio entre20y40 = new RangoPrecio(SkillCoins.de(20), SkillCoins.de(40));
        RangoPrecio desde50 = new RangoPrecio(SkillCoins.de(50), null);
        RangoPrecio hasta10 = new RangoPrecio(null, SkillCoins.de(10));

        assertEquals(List.of("Clases de Kotlin", "Mentoria de Java"), titulos(filtro(null, null, null, null, entre20y40, null, null, null, null)));
        assertEquals(List.of("Taller de SQL"), titulos(filtro(null, null, null, null, desde50, null, null, null, null)));
        assertEquals(List.of("Desafio de microservicios"), titulos(filtro(null, null, null, null, hasta10, null, null, null, null)));
    }

    @Test
    @DisplayName("filtra por habilidad y por etiqueta")
    void filtroHabilidadYEtiqueta() {
        assertEquals(List.of("Clases de Kotlin"), titulos(filtro(null, null, null, null, null, "Kotlin", null, null, null)));
        assertEquals(List.of("Taller de SQL"), titulos(filtro(null, null, null, null, null, "sql", null, null, null)));
        assertEquals(List.of("Clases de Kotlin", "Mentoria de Java"), titulos(filtro(null, null, null, null, null, null, "backend", null, null)));
        assertTrue(titulos(filtro(null, null, null, null, null, "cobol", null, null, null)).isEmpty());
    }

    @Test
    @DisplayName("filtra por disponibilidad: solo publicaciones con una franja LIBRE dentro de la ventana pedida")
    void filtroDisponibilidad() {
        var desde = app.reloj.ahora().plusHours(250 - 3); // el reloj ya avanzo 3 h desde que se armo el catalogo
        FiltroBusqueda tardias = new FiltroBusqueda(null, null, null, null, null, null, null, null, desde, null, CriterioOrden.RECIENTES);
        FiltroBusqueda ventana = new FiltroBusqueda(null, null, null, null, null, null, null, null,
                app.reloj.ahora().plusHours(95), app.reloj.ahora().plusHours(110), CriterioOrden.RECIENTES);

        assertEquals(List.of("Desafio de microservicios", "Clases de Kotlin"), titulos(tardias));
        assertEquals(List.of("Mentoria de Java"), titulos(ventana));
    }

    @Test
    @DisplayName("una franja ya reservada no cuenta como disponibilidad")
    void disponibilidadIgnoraFranjasOcupadas() {
        PublicacionDto java = e.publicada(ana, e.oferta().titulo("Unica franja").franjasEn(500L));
        FiltroBusqueda enLaVentana = new FiltroBusqueda(null, null, null, null, null, null, null, null,
                app.reloj.ahora().plusHours(490), app.reloj.ahora().plusHours(510), CriterioOrden.RECIENTES);
        assertEquals(List.of("Unica franja"), titulos(enLaVentana));

        e.aceptada(bruno, ana, java, 0);

        assertTrue(titulos(enLaVentana).isEmpty());
    }

    @Test
    @DisplayName("filtra por reputacion minima del autor, que se calcula a partir de calificaciones reales")
    void filtroReputacionMinima() {
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PublicacionDto sesion = e.publicada(ana, e.oferta().titulo("Sesion ya dictada").franjasEn(50L));
        ContratacionDto completada = e.completada(cecilia, ana, sesion, 0);
        e.calificar(cecilia, completada, 5);
        app.publicacionUseCase.cerrar(ana.id(), sesion.id());

        assertEquals(List.of("Taller de SQL", "Mentoria de Java"), titulos(filtro(null, null, null, null, null, null, null, 4.5, null)));
        assertTrue(titulos(filtro(null, null, null, null, null, null, null, 5.1, null)).isEmpty());
        assertEquals(4, titulos(filtro(null, null, null, null, null, null, null, 0.0, null)).size());
    }

    @Test
    @DisplayName("los filtros se combinan con AND")
    void filtrosCombinados() {
        assertEquals(List.of("Mentoria de Java"), titulos(filtro("java", "programacion", TipoPublicacion.MENTORIA, Modalidad.REMOTA,
                new RangoPrecio(SkillCoins.de(30), SkillCoins.de(50)), "java", "backend", null, null)));
        assertTrue(titulos(filtro("java", "datos", null, null, null, null, null, null, null)).isEmpty());
    }

    // ---------- orden ----------

    @Test
    @DisplayName("ordena por precio ascendente y descendente")
    void ordenPorPrecio() {
        assertEquals(List.of("Desafio de microservicios", "Clases de Kotlin", "Mentoria de Java", "Taller de SQL"),
                titulos(filtro(null, null, null, null, null, null, null, null, CriterioOrden.PRECIO_ASC)));
        assertEquals(List.of("Taller de SQL", "Mentoria de Java", "Clases de Kotlin", "Desafio de microservicios"),
                titulos(filtro(null, null, null, null, null, null, null, null, CriterioOrden.PRECIO_DESC)));
    }

    @Test
    @DisplayName("ordena por reputacion del autor (la mejor primero)")
    void ordenPorReputacion() {
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PublicacionDto sesion = e.publicada(bruno, e.oferta().titulo("Sesion de Bruno").franjasEn(50L));
        e.calificar(cecilia, e.completada(cecilia, bruno, sesion, 0), 5);
        app.publicacionUseCase.cerrar(bruno.id(), sesion.id());

        List<String> ordenadas = titulos(filtro(null, null, null, null, null, null, null, null, CriterioOrden.REPUTACION));

        assertEquals("Clases de Kotlin", ordenadas.get(0), "el autor con 5.0 va primero");
        assertEquals(4, ordenadas.size());
    }

    // ---------- paginacion ----------

    @Test
    @DisplayName("pagina los resultados con total, cantidad de paginas y orden estable")
    void paginacion() {
        Pagina<PublicacionResumenDto> primera = app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), PaginaRequest.de(0, 3));
        Pagina<PublicacionResumenDto> segunda = app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), PaginaRequest.de(1, 3));
        Pagina<PublicacionResumenDto> fuera = app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), PaginaRequest.de(5, 3));

        assertEquals(4L, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(3, primera.contenido().size());
        assertTrue(primera.tieneSiguiente());
        assertEquals(1, segunda.contenido().size());
        assertFalse(segunda.tieneSiguiente());
        assertEquals("Mentoria de Java", segunda.contenido().get(0).titulo());
        assertTrue(fuera.contenido().isEmpty());
        assertEquals(4L, fuera.totalElementos());
        assertEquals(0, primera.pagina());
        assertEquals(3, primera.tamano());
    }

    @Test
    @DisplayName("sin resultados devuelve una pagina vacia y no falla")
    void sinResultados() {
        Pagina<PublicacionResumenDto> vacia = app.busquedaUseCase.buscar(
                filtro("nada que ver", null, null, null, null, null, null, null, null), PaginaRequest.de(0, 10));

        assertTrue(vacia.contenido().isEmpty());
        assertEquals(0L, vacia.totalElementos());
        assertEquals(0, vacia.totalPaginas());
    }

    @Test
    @DisplayName("la pagina es obligatoria: nula es ValidacionException; tamanos fuera de rango los rechaza PaginaRequest")
    void paginaInvalida() {
        assertThrows(ValidacionException.class, () -> app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), null));
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO + 1));
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(-1, 10));
    }

    // ---------- resumen ----------

    @Test
    @DisplayName("cada fila trae autor, reputacion, cupo disponible y precio final con promociones vigentes")
    void resumenCompleto() {
        PublicacionDto java = app.publicacionUseCase.listarPropias(ana.id(), PaginaRequest.de(0, 50)).contenido().stream()
                .filter(r -> r.titulo().equals("Mentoria de Java")).findFirst().map(r -> app.publicacionUseCase.obtener(ana.id(), r.id())).orElseThrow();
        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(java.id(), "Promo", TipoPromocion.PORCENTAJE,
                new BigDecimal("25"), null, null));

        PublicacionResumenDto fila = app.busquedaUseCase.buscar(filtro("java", null, null, null, null, null, null, null, null),
                PaginaRequest.de(0, 10)).contenido().get(0);

        assertEquals(java.id(), fila.id());
        assertEquals("Ana", fila.autorNombre());
        assertEquals(ana.id(), fila.autorId());
        assertEquals(0.0, fila.autorReputacion(), 0.0001);
        assertEquals(1, fila.cupoDisponible());
        assertEquals("PUBLICADA", fila.estado());
        assertEquals(TipoPublicacion.MENTORIA, fila.tipo());
        Escenario.assertMonto("40", fila.precioBase());
        Escenario.assertMonto("30", fila.precioFinal());
    }

    // ---------- sin N+1 ----------

    /** Cuenta las consultas de perfiles para comprobar que se resuelven por lote. */
    private static final class PerfilesContados implements PerfilRepository {
        private final PerfilRepository real;
        int porId;
        int porIds;

        PerfilesContados(PerfilRepository real) {
            this.real = real;
        }

        @Override public Perfil guardar(Perfil perfil) { return real.guardar(perfil); }
        @Override public Optional<Perfil> buscarPorId(Long id) { porId++; return real.buscarPorId(id); }
        @Override public Optional<Perfil> buscarPorUserId(String userId) { return real.buscarPorUserId(userId); }
        @Override public List<Perfil> buscarPorIds(Set<Long> ids) { porIds++; return real.buscarPorIds(ids); }
    }

    /** Cuenta las consultas de promociones para comprobar que el listado usa la version por lote. */
    private static final class PromocionesContadas implements PromocionRepository {
        private final PromocionRepository real;
        int individuales;
        int porLote;

        PromocionesContadas(PromocionRepository real) {
            this.real = real;
        }

        @Override public Promocion guardar(Promocion promocion) { return real.guardar(promocion); }
        @Override public List<Promocion> buscarActivasPorPublicacion(Long publicacionId) { individuales++; return real.buscarActivasPorPublicacion(publicacionId); }
        @Override public Map<Long, List<Promocion>> buscarActivasPorPublicaciones(Collection<Long> ids) { porLote++; return real.buscarActivasPorPublicaciones(ids); }
    }

    @Test
    @DisplayName("SIN N+1: los autores y las promociones de toda la pagina se piden con UNA consulta por lote")
    void sinNMasUno() {
        PerfilesContados perfiles = new PerfilesContados(app.perfiles);
        PromocionesContadas promociones = new PromocionesContadas(app.promociones);
        BusquedaService servicio = new BusquedaService(app.publicaciones, perfiles, promociones, app.reloj);

        Pagina<PublicacionResumenDto> pagina = servicio.buscar(FiltroBusqueda.vacio(), PaginaRequest.de(0, 10));

        assertEquals(4, pagina.contenido().size());
        assertEquals(1, perfiles.porIds, "una consulta de autores para toda la pagina");
        assertEquals(0, perfiles.porId, "ninguna consulta por fila");
        assertEquals(1, promociones.porLote);
        assertEquals(0, promociones.individuales);
        assertTrue(pagina.contenido().stream().allMatch(r -> r.autorNombre() != null));
    }
}
