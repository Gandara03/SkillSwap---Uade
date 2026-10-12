package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Busqueda dinamica de publicaciones contra H2: filtros combinables, orden, paginacion con count aparte y
 * colecciones cargadas en lote. Catalogo de prueba (todas PUBLICADAS salvo indicacion):
 * <pre>
 *  P1 ana    Mentoria de Java avanzada   programacion MENTORIA  REMOTA     100  java,spring   backend,Top   franja D+1
 *  P2 ana    Clases de diseno grafico    diseno       SERVICIO  PRESENCIAL  50  photoshop     creativo      franja D+3  (desc: "50% off_promo")
 *  P3 bruno  Mentoria de Python          programacion MENTORIA  REMOTA     200  python        datos         franja D+2
 *  P4 techc. Desafio de datos            datos        DESAFIO   REMOTA     500  python,sql     -            franja D+4
 *  P5 bruno  Mentoria secreta de Java    BORRADOR      P6 ana  Mentoria pausada de Java   PAUSADA
 * </pre>
 * Reputacion: ana 4.8, bruno 3.0, techcorp 0.
 */
class JpaBusquedaPublicacionTest extends JpaTestBase {

    private static final PaginaRequest TODAS = PaginaRequest.de(0, 50);

    private Perfil ana;
    private Publicacion p1;
    private Publicacion p2;
    private Publicacion p3;
    private Publicacion p4;

    @BeforeEach
    void crearCatalogo() {
        ana = conReputacion(profesional("u-ana"), 4.8, 5);
        Perfil bruno = conReputacion(profesional("u-bruno"), 3.0, 2);
        Perfil techcorp = organizacion("u-techcorp");

        p1 = publicacion(ana).titulo("Mentoria de Java avanzada").descripcion("Patrones y arquitectura")
                .categoria("programacion").modalidad(Modalidad.REMOTA).precio(100)
                .habilidades("Java", "Spring").etiquetas("backend", "Top")
                .franjasEn(AHORA.plusDays(1)).publicadaEn(AHORA.plusMinutes(1)).guardar();
        p2 = publicacion(ana).tipo(TipoPublicacion.SERVICIO).titulo("Clases de diseno grafico")
                .descripcion("Todo con 50% off_promo este mes").categoria("diseno").modalidad(Modalidad.PRESENCIAL)
                .precio(50).habilidades("Photoshop").etiquetas("creativo")
                .franjasEn(AHORA.plusDays(3)).publicadaEn(AHORA.plusMinutes(2)).guardar();
        p3 = publicacion(bruno).titulo("Mentoria de Python").descripcion("Aprende analisis de datos")
                .categoria("programacion").modalidad(Modalidad.REMOTA).precio(200)
                .habilidades("Python").etiquetas("datos")
                .franjasEn(AHORA.plusDays(2)).publicadaEn(AHORA.plusMinutes(3)).guardar();
        p4 = publicacion(techcorp).titulo("Desafio de datos").descripcion("Modelo predictivo")
                .categoria("datos").modalidad(Modalidad.REMOTA).precio(500)
                .habilidades("Python", "SQL").franjasEn(AHORA.plusDays(4)).publicadaEn(AHORA.plusMinutes(4)).guardar();

        publicacion(bruno).titulo("Mentoria secreta de Java").borrador().guardar();
        Publicacion pausada = publicacion(ana).titulo("Mentoria pausada de Java").guardar();
        pausada.pausar(ana.getId(), AHORA.plusMinutes(9));
        publicaciones.guardar(pausada);
    }

    // ---------- helper para armar filtros ----------

    private static final class Filtro {
        String texto;
        String categoria;
        TipoPublicacion tipo;
        Modalidad modalidad;
        RangoPrecio rango;
        String habilidad;
        String etiqueta;
        Double reputacionMinima;
        LocalDateTime desde;
        LocalDateTime hasta;
        CriterioOrden orden = CriterioOrden.RECIENTES;

        FiltroBusqueda construir() {
            return new FiltroBusqueda(texto, categoria, tipo, modalidad, rango, habilidad, etiqueta, reputacionMinima,
                    desde, hasta, orden);
        }
    }

    private List<Long> ids(Filtro f) {
        return ids(f.construir());
    }

    private List<Long> ids(FiltroBusqueda f) {
        return publicaciones.buscar(f, TODAS).contenido().stream().map(Publicacion::getId).toList();
    }

    private static RangoPrecio rango(Integer min, Integer max) {
        return new RangoPrecio(min == null ? null : SkillCoins.de(min), max == null ? null : SkillCoins.de(max));
    }

    // ---------- alcance ----------

    @Test
    @DisplayName("sin filtros devuelve solo las PUBLICADAS (no borradores ni pausadas), las mas recientes primero")
    void soloPublicadas() {
        Pagina<Publicacion> pagina = publicaciones.buscar(FiltroBusqueda.vacio(), TODAS);

        assertEquals(4, pagina.totalElementos());
        assertIterableEquals(List.of(p4.getId(), p3.getId(), p2.getId(), p1.getId()),
                pagina.contenido().stream().map(Publicacion::getId).toList());
    }

    @Test
    @DisplayName("un filtro null equivale al filtro vacio")
    void filtroNulo() {
        assertEquals(4, publicaciones.buscar(null, TODAS).totalElementos());
    }

    @Test
    @DisplayName("las publicaciones vienen con franjas, etiquetas, habilidades e historial cargados")
    void coleccionesCargadas() {
        Publicacion leida = publicaciones.buscar(FiltroBusqueda.vacio(), TODAS).contenido().stream()
                .filter(p -> p.getId().equals(p1.getId())).findFirst().orElseThrow();

        assertEquals(1, leida.getFranjas().size());
        assertEquals(2, leida.getEtiquetas().size());
        assertEquals(2, leida.getHabilidades().size());
        assertEquals(2, leida.getHistorial().size());
        assertEquals("PUBLICADA", leida.getEstado());
    }

    // ---------- filtros ----------

    @Test
    @DisplayName("texto: busca en titulo y en descripcion, sin distinguir mayusculas")
    void filtroTexto() {
        Filtro f = new Filtro();
        f.texto = "JAVA";
        assertIterableEquals(List.of(p1.getId()), ids(f));

        f.texto = "analisis";
        assertIterableEquals(List.of(p3.getId()), ids(f));

        f.texto = "inexistente";
        assertTrue(ids(f).isEmpty());
    }

    @Test
    @DisplayName("texto: los comodines % y _ se tratan como caracteres literales")
    void filtroTextoEscapaComodines() {
        Filtro f = new Filtro();
        f.texto = "50%";
        assertIterableEquals(List.of(p2.getId()), ids(f));

        f.texto = "off_promo";
        assertIterableEquals(List.of(p2.getId()), ids(f));

        // si '_' fuera comodin, "o_f" encontraria "off"
        f.texto = "o_f";
        assertTrue(ids(f).isEmpty());
        f.texto = "%";
        assertEquals(List.of(p2.getId()), ids(f));
    }

    @Test
    @DisplayName("categoria, tipo y modalidad")
    void filtroCategoriaTipoModalidad() {
        Filtro f = new Filtro();
        f.categoria = "Programacion";
        assertIterableEquals(List.of(p3.getId(), p1.getId()), ids(f));

        f = new Filtro();
        f.tipo = TipoPublicacion.DESAFIO;
        assertIterableEquals(List.of(p4.getId()), ids(f));

        f.tipo = TipoPublicacion.MENTORIA;
        assertIterableEquals(List.of(p3.getId(), p1.getId()), ids(f));

        f = new Filtro();
        f.modalidad = Modalidad.PRESENCIAL;
        assertIterableEquals(List.of(p2.getId()), ids(f));
    }

    @Test
    @DisplayName("rango de precio: extremos inclusivos y cualquiera de los dos puede faltar")
    void filtroRangoPrecio() {
        Filtro f = new Filtro();
        f.rango = rango(60, 250);
        assertIterableEquals(List.of(p3.getId(), p1.getId()), ids(f));

        f.rango = rango(200, null);
        assertIterableEquals(List.of(p4.getId(), p3.getId()), ids(f));

        f.rango = rango(null, 50);
        assertIterableEquals(List.of(p2.getId()), ids(f));

        f.rango = rango(100, 100);
        assertIterableEquals(List.of(p1.getId()), ids(f));
    }

    @Test
    @DisplayName("habilidad (normalizada) y etiqueta (sin distinguir mayusculas)")
    void filtroHabilidadYEtiqueta() {
        Filtro f = new Filtro();
        f.habilidad = " JAVA ";
        assertIterableEquals(List.of(p1.getId()), ids(f));

        f.habilidad = "python";
        assertIterableEquals(List.of(p4.getId(), p3.getId()), ids(f));

        f = new Filtro();
        f.etiqueta = "top";
        assertIterableEquals(List.of(p1.getId()), ids(f));

        f.etiqueta = "inexistente";
        assertTrue(ids(f).isEmpty());
    }

    @Test
    @DisplayName("reputacion minima del autor (inclusiva)")
    void filtroReputacionMinima() {
        Filtro f = new Filtro();
        f.reputacionMinima = 4.0;
        assertIterableEquals(List.of(p2.getId(), p1.getId()), ids(f));

        f.reputacionMinima = 3.0;
        assertIterableEquals(List.of(p3.getId(), p2.getId(), p1.getId()), ids(f));

        f.reputacionMinima = 4.9;
        assertTrue(ids(f).isEmpty());
    }

    @Test
    @DisplayName("disponibilidad: existe una franja LIBRE completamente dentro del rango pedido")
    void filtroDisponibilidad() {
        Filtro f = new Filtro();
        f.desde = AHORA.plusDays(1);
        f.hasta = AHORA.plusDays(2).plusHours(1);
        assertIterableEquals(List.of(p3.getId(), p1.getId()), ids(f));

        f = new Filtro();
        f.desde = AHORA.plusDays(3);
        assertIterableEquals(List.of(p4.getId(), p2.getId()), ids(f));

        f = new Filtro();
        f.hasta = AHORA.plusDays(1).plusHours(1);
        assertIterableEquals(List.of(p1.getId()), ids(f));

        // una franja que empieza antes de 'desde' no esta "dentro" del rango
        f = new Filtro();
        f.desde = AHORA.plusDays(1).plusMinutes(30);
        f.hasta = AHORA.plusDays(1).plusHours(3);
        assertTrue(ids(f).isEmpty());
    }

    @Test
    @DisplayName("disponibilidad: una franja ocupada no cuenta")
    void filtroDisponibilidadIgnoraFranjasOcupadas() {
        Publicacion editable = publicaciones.buscarPorId(p1.getId()).orElseThrow();
        editable.reservarFranja(editable.getFranjas().get(0).getId(), AHORA);
        publicaciones.guardar(editable);

        Filtro f = new Filtro();
        f.desde = AHORA.plusDays(1);
        f.hasta = AHORA.plusDays(1).plusHours(1);

        assertTrue(ids(f).isEmpty());
    }

    @Test
    @DisplayName("filtros combinados: todos tienen que cumplirse a la vez")
    void filtrosCombinados() {
        Filtro f = new Filtro();
        f.categoria = "programacion";
        f.reputacionMinima = 4.0;
        f.habilidad = "java";
        f.rango = rango(null, 150);
        f.modalidad = Modalidad.REMOTA;
        assertIterableEquals(List.of(p1.getId()), ids(f));

        f.habilidad = "python";
        assertTrue(ids(f).isEmpty());
    }

    // ---------- orden ----------

    @Test
    @DisplayName("orden por precio ascendente y descendente")
    void ordenPorPrecio() {
        Filtro f = new Filtro();
        f.orden = CriterioOrden.PRECIO_ASC;
        assertIterableEquals(List.of(p2.getId(), p1.getId(), p3.getId(), p4.getId()), ids(f));

        f.orden = CriterioOrden.PRECIO_DESC;
        assertIterableEquals(List.of(p4.getId(), p3.getId(), p1.getId(), p2.getId()), ids(f));
    }

    @Test
    @DisplayName("orden por reputacion del autor (empata por mas reciente) y por recientes")
    void ordenPorReputacionYRecientes() {
        Filtro f = new Filtro();
        f.orden = CriterioOrden.REPUTACION;
        assertIterableEquals(List.of(p2.getId(), p1.getId(), p3.getId(), p4.getId()), ids(f));

        f.orden = CriterioOrden.RECIENTES;
        assertIterableEquals(List.of(p4.getId(), p3.getId(), p2.getId(), p1.getId()), ids(f));
    }

    // ---------- paginacion ----------

    @Test
    @DisplayName("paginacion: el total viene de un count aparte y cada pagina trae su porcion en orden")
    void paginacion() {
        FiltroBusqueda vacio = FiltroBusqueda.vacio();

        Pagina<Publicacion> primera = publicaciones.buscar(vacio, PaginaRequest.de(0, 3));
        Pagina<Publicacion> segunda = publicaciones.buscar(vacio, PaginaRequest.de(1, 3));
        Pagina<Publicacion> fuera = publicaciones.buscar(vacio, PaginaRequest.de(2, 3));

        assertEquals(4, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertTrue(primera.tieneSiguiente());
        assertIterableEquals(List.of(p4.getId(), p3.getId(), p2.getId()),
                primera.contenido().stream().map(Publicacion::getId).toList());
        assertIterableEquals(List.of(p1.getId()), segunda.contenido().stream().map(Publicacion::getId).toList());
        assertFalse(segunda.tieneSiguiente());
        assertTrue(fuera.contenido().isEmpty());
        assertEquals(4, fuera.totalElementos());
    }

    @Test
    @DisplayName("paginacion con filtro sobre colecciones: sin filas duplicadas y total correcto")
    void paginacionConFiltroDeColecciones() {
        Filtro f = new Filtro();
        f.habilidad = "python";

        Pagina<Publicacion> primera = publicaciones.buscar(f.construir(), PaginaRequest.de(0, 1));
        Pagina<Publicacion> segunda = publicaciones.buscar(f.construir(), PaginaRequest.de(1, 1));

        assertEquals(2, primera.totalElementos());
        assertEquals(p4.getId(), primera.contenido().get(0).getId());
        assertEquals(p3.getId(), segunda.contenido().get(0).getId());
    }

    @Test
    @DisplayName("con muchas publicaciones la pagina respeta el tamano y el orden")
    void paginacionConMuchasPublicaciones() {
        for (int i = 0; i < 25; i++) {
            publicacion(ana).titulo("Extra " + i).categoria("masivo").precio(10 + i)
                    .habilidades("java", "extra" + i).etiquetas("e" + i).publicadaEn(AHORA.plusHours(1).plusMinutes(i))
                    .guardar();
        }
        Filtro f = new Filtro();
        f.categoria = "masivo";
        f.orden = CriterioOrden.PRECIO_ASC;

        Pagina<Publicacion> pagina = publicaciones.buscar(f.construir(), PaginaRequest.de(2, 10));

        assertEquals(25, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
        assertEquals(5, pagina.contenido().size());
        assertEquals(SkillCoins.de(30), pagina.contenido().get(0).getPrecioBase());
        assertEquals(2, pagina.contenido().get(0).getHabilidades().size());
        assertEquals(1, pagina.contenido().get(0).getEtiquetas().size());
    }
}
