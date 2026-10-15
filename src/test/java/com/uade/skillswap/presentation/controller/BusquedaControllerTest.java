package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integracion BusquedaController -> BusquedaUseCase -> JPQL sobre H2. La busqueda es publica: no usa actor. */
@DisplayName("BusquedaController (JPA/H2): filtros, paginacion, validacion y visibilidad")
class BusquedaControllerTest extends ControllerJpaTestBase {

    private IBusquedaController busqueda() {
        return app.getBusquedaController();
    }

    /** Tres mentorias publicadas: dos de Java (Ana, 30 y 60 SC) y una de SQL (Bruno, 40 SC). */
    private void catalogo() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        publicar("u-ana", mentoria("Java desde cero", "programacion", 30, 2, TipoPoliticaCancelacion.FLEXIBLE,
                Set.of("java"), Set.of("java"), franjasDiarias(1)));
        publicar("u-ana", mentoria("Java avanzado", "programacion", 60, 2, TipoPoliticaCancelacion.FLEXIBLE,
                Set.of("java"), Set.of("java"), franjasDiarias(1)));
        publicar("u-bruno", mentoria("SQL para todos", "datos", 40, 2, TipoPoliticaCancelacion.FLEXIBLE,
                Set.of("sql"), Set.of("sql"), franjasDiarias(1)));
    }

    private static FiltroBusqueda porTexto(String texto) {
        return new FiltroBusqueda(texto, null, null, null, null, null, null, null, null, null,
                CriterioOrden.PRECIO_ASC);
    }

    private static FiltroBusqueda conReputacionMinima(Double minima) {
        return new FiltroBusqueda(null, null, null, null, null, null, null, minima, null, null, null);
    }

    private static FiltroBusqueda disponibleEntre(LocalDateTime desde, LocalDateTime hasta) {
        return new FiltroBusqueda(null, null, null, null, null, null, null, null, desde, hasta, null);
    }

    private static List<String> titulos(Pagina<PublicacionResumenDto> pagina) {
        return pagina.contenido().stream().map(PublicacionResumenDto::titulo).toList();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("busca con filtro y orden, y pagina los resultados")
    void buscarConFiltroYPaginas() {
        catalogo();

        Pagina<PublicacionResumenDto> primera = ok(busqueda().buscar(porTexto("java"), PaginaRequest.de(0, 1)));
        Pagina<PublicacionResumenDto> segunda = ok(busqueda().buscar(porTexto("java"), PaginaRequest.de(1, 1)));

        assertEquals(2L, primera.totalElementos());
        assertEquals(List.of("Java desde cero"), titulos(primera), "orden por menor precio");
        assertTrue(primera.tieneSiguiente());
        assertEquals(List.of("Java avanzado"), titulos(segunda));
        assertFalse(segunda.tieneSiguiente());
    }

    @Test
    @DisplayName("un filtro nulo no filtra: devuelve todo lo publicado")
    void filtroNulo() {
        catalogo();

        assertEquals(3L, ok(busqueda().buscar(null, primeraPagina())).totalElementos());
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("pagina nula, reputacion minima fuera de 0 a 5 o disponibilidad invertida son VALIDACION")
    void entradaInvalida() {
        LocalDateTime manana = LocalDateTime.now().plusDays(1);

        falla(VALIDACION, busqueda().buscar(FiltroBusqueda.vacio(), null));
        falla(VALIDACION, busqueda().buscar(conReputacionMinima(5.5), primeraPagina()));
        falla(VALIDACION, busqueda().buscar(conReputacionMinima(-1.0), primeraPagina()));
        falla(VALIDACION, busqueda().buscar(disponibleEntre(manana.plusDays(2), manana), primeraPagina()));

        // los extremos del rango son validos
        ok(busqueda().buscar(conReputacionMinima(0.0), primeraPagina()));
        ok(busqueda().buscar(conReputacionMinima(5.0), primeraPagina()));
    }

    // ---------- permisos y visibilidad ----------

    @Test
    @DisplayName("es publica: anda sin sesion y nadie ve borradores ni publicaciones pausadas de otros")
    void publicaSinBorradoresNiPausadas() {
        catalogo();
        como("u-ana");
        ok(app.getPublicacionController().crearBorrador(mentoria("Borrador de Java", "programacion", 10, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), franjasDiarias(1))));
        PublicacionDto pausada = publicar("u-ana", mentoria("Java en pausa", "programacion", 10, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), franjasDiarias(1)));
        ok(app.getPublicacionController().pausar(pausada.id()));

        sinSesion();
        assertEquals(3L, ok(busqueda().buscar(FiltroBusqueda.vacio(), primeraPagina())).totalElementos());
        como("u-bruno");
        assertEquals(List.of("Java desde cero", "Java avanzado"),
                titulos(ok(busqueda().buscar(porTexto("java"), primeraPagina()))));
    }
}
