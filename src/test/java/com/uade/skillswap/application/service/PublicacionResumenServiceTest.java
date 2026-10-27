package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.CANCELAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.CERRAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.PAUSAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.PUBLICAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.REANUDAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Resumen de publicacion: descripcion, duracion, etiquetas y acciones del autor")
class PublicacionResumenServiceTest {

    private static final PaginaRequest PAGINA = PaginaRequest.de(0, 10);

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
    }

    private PublicacionResumenDto propia() {
        return app.publicacionUseCase.listarPropias(ana.id(), PAGINA).contenido().get(0);
    }

    @Test
    @DisplayName("mis publicaciones informa los datos de la oferta que antes no traia el resumen")
    void datosDeLaOferta() {
        e.borrador(ana, e.oferta());

        PublicacionResumenDto resumen = propia();

        assertEquals("Clean Architecture y patrones", resumen.descripcion());
        assertEquals(60, resumen.duracionMinutos());
        assertEquals(Set.of("java", "backend"), resumen.etiquetas());
    }

    @Test
    @DisplayName("las acciones del autor siguen al estado de la publicacion")
    void accionesSegunEstado() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());
        assertEquals(List.of(PUBLICAR, CANCELAR), propia().accionesDelAutor());

        app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertEquals(List.of(PAUSAR, CERRAR, CANCELAR), propia().accionesDelAutor());

        app.publicacionUseCase.pausar(ana.id(), borrador.id());
        assertEquals(List.of(REANUDAR, CERRAR, CANCELAR), propia().accionesDelAutor());

        app.publicacionUseCase.cerrar(ana.id(), borrador.id());
        assertEquals(List.of(), propia().accionesDelAutor());
    }

    @Test
    @DisplayName("el detalle y lo que devuelven las operaciones tambien traen las acciones del autor")
    void detalleConAccionesDelAutor() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());
        assertEquals(List.of(PUBLICAR, CANCELAR), borrador.accionesDelAutor());

        PublicacionDto publicada = app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertEquals(List.of(PAUSAR, CERRAR, CANCELAR), publicada.accionesDelAutor());
        assertEquals(publicada.accionesDelAutor(), e.publicacion(ana, publicada).accionesDelAutor());
    }

    @Test
    @DisplayName("la busqueda informa las mismas acciones del autor: no dependen de quien busca")
    void busquedaInformaAccionesDelAutor() {
        e.publicada(ana);

        PublicacionResumenDto encontrada = app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), PAGINA).contenido().get(0);

        assertEquals(List.of(PAUSAR, CERRAR, CANCELAR), encontrada.accionesDelAutor());
        assertEquals(Set.of("java", "backend"), encontrada.etiquetas());
    }
}
