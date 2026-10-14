package com.uade.skillswap.domain.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Objetos de consulta: Pagina, PaginaRequest, FiltroBusqueda")
class ConsultasTest {

    @Test
    @DisplayName("PaginaRequest calcula el offset y valida pagina y tamano")
    void paginaRequest() {
        assertEquals(20, PaginaRequest.de(2, 10).offset());
        assertEquals(0, PaginaRequest.de(0, 5).offset());
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO + 1));
        assertDoesNotThrow(() -> PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO));
    }

    @Test
    @DisplayName("Pagina calcula total de paginas y si hay siguiente")
    void pagina() {
        Pagina<Integer> p = new Pagina<>(List.of(1, 2, 3), 0, 3, 7);
        assertEquals(3, p.totalPaginas());
        assertTrue(p.tieneSiguiente());
        Pagina<Integer> ultima = new Pagina<>(List.of(7), 2, 3, 7);
        assertFalse(ultima.tieneSiguiente());
        assertEquals(0, new Pagina<Integer>(List.of(), 0, 0, 0).totalPaginas());
        assertEquals(1, new Pagina<Integer>(List.of(1), 0, 10, 10).totalPaginas());
    }

    @Test
    @DisplayName("Pagina.mapear transforma el contenido y conserva los metadatos")
    void mapear() {
        Pagina<String> p = new Pagina<>(List.of(1, 2), 1, 2, 5).mapear(i -> "n" + i);
        assertEquals(List.of("n1", "n2"), p.contenido());
        assertEquals(1, p.pagina());
        assertEquals(5L, p.totalElementos());
    }

    @Test
    @DisplayName("el contenido de una Pagina es inmutable")
    void inmutable() {
        Pagina<Integer> p = new Pagina<>(new java.util.ArrayList<>(List.of(1)), 0, 1, 1);
        assertThrows(UnsupportedOperationException.class, () -> p.contenido().add(2));
    }

    @Test
    @DisplayName("Pagina.recortar corta una lista ordenada: primera pagina, ultima incompleta y fuera de rango")
    void recortar() {
        List<Integer> ordenados = List.of(1, 2, 3, 4, 5);

        Pagina<Integer> primera = Pagina.recortar(ordenados, PaginaRequest.de(0, 2));
        assertEquals(List.of(1, 2), primera.contenido());
        assertEquals(5L, primera.totalElementos());
        assertTrue(primera.tieneSiguiente());

        Pagina<Integer> ultima = Pagina.recortar(ordenados, PaginaRequest.de(2, 2));
        assertEquals(List.of(5), ultima.contenido());
        assertEquals(2, ultima.pagina());
        assertFalse(ultima.tieneSiguiente());

        Pagina<Integer> afuera = Pagina.recortar(ordenados, PaginaRequest.de(7, 2));
        assertTrue(afuera.contenido().isEmpty());
        assertEquals(5L, afuera.totalElementos(), "una pagina fuera de rango igual informa el total");

        assertEquals(0L, Pagina.recortar(List.<Integer>of(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("FiltroBusqueda.vacio no filtra y ordena por RECIENTES; orden nulo equivale a RECIENTES")
    void filtro() {
        FiltroBusqueda f = FiltroBusqueda.vacio();
        assertNull(f.texto());
        assertNull(f.rangoPrecio());
        assertEquals(CriterioOrden.RECIENTES, f.ordenEfectivo());
        FiltroBusqueda sinOrden = new FiltroBusqueda(null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(CriterioOrden.RECIENTES, sinOrden.ordenEfectivo());
    }
}
