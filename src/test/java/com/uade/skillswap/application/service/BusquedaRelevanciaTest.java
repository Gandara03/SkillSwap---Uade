package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Busqueda: orden por relevancia (4.3)")
class BusquedaRelevanciaTest {

    private static FiltroBusqueda filtro(String texto, CriterioOrden orden) {
        return new FiltroBusqueda(texto, null, null, null, null, null, null, null, null, null, orden);
    }

    @Test
    @DisplayName("RELEVANCIA pone primero las coincidencias en el titulo aunque sean mas viejas; RECIENTES no")
    void relevanciaPriorizaTitulo() {
        Escenario e = new Escenario();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto enTitulo = e.publicada(ana, e.oferta().titulo("Mentoria de Java avanzado").descripcion("Patrones"));
        PublicacionDto enDescripcion = e.publicada(ana, e.oferta().titulo("Backend en la practica")
                .descripcion("Trabajamos con Java y bases de datos"));

        List<PublicacionResumenDto> porRelevancia = e.app.busquedaUseCase
                .buscar(filtro("java", CriterioOrden.RELEVANCIA), PaginaRequest.de(0, 10)).contenido();
        List<PublicacionResumenDto> porFecha = e.app.busquedaUseCase
                .buscar(filtro("java", CriterioOrden.RECIENTES), PaginaRequest.de(0, 10)).contenido();

        assertEquals(List.of(enTitulo.id(), enDescripcion.id()), porRelevancia.stream().map(PublicacionResumenDto::id).toList());
        assertEquals(List.of(enDescripcion.id(), enTitulo.id()), porFecha.stream().map(PublicacionResumenDto::id).toList());
    }

    @Test
    @DisplayName("RELEVANCIA sin texto no falla y devuelve todo paginado")
    void relevanciaSinTexto() {
        Escenario e = new Escenario();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        e.publicada(ana);
        assertEquals(1, e.app.busquedaUseCase.buscar(filtro(null, CriterioOrden.RELEVANCIA), PaginaRequest.de(0, 10))
                .totalElementos());
    }
}
