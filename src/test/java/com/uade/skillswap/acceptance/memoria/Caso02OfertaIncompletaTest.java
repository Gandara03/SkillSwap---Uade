package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 2: rechazar la publicacion de una oferta incompleta.
 * El borrador puede guardarse incompleto; la completitud se exige recien al publicar (regla del dominio).
 */
@DisplayName("Caso 2 (memoria): rechazar la publicacion de una oferta incompleta")
class Caso02OfertaIncompletaTest {

    @Test
    @DisplayName("Caso 2: sin titulo, categoria, precio ni franjas no se puede publicar y sigue en BORRADOR")
    void caso02_rechazarPublicacionDeOfertaIncompleta() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        CrearPublicacionCommand incompleta = new CrearPublicacionCommand(TipoPublicacion.MENTORIA, null, null, null,
                Modalidad.REMOTA, 60, 1, null, Set.of(), Set.of(), null, List.of());

        PublicacionDto borrador = e.app.publicacionUseCase.crearBorrador(ana.id(), incompleta);
        assertEquals("BORRADOR", borrador.estado());

        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> e.app.publicacionUseCase.publicar(ana.id(), borrador.id()));
        // el mensaje enumera lo que falta
        assertTrue(error.getMessage().contains("titulo"), error.getMessage());
        assertTrue(error.getMessage().contains("categoria"), error.getMessage());
        assertTrue(error.getMessage().contains("precio"), error.getMessage());
        assertTrue(error.getMessage().contains("franja"), error.getMessage());

        // no cambio de estado y no es visible en la busqueda
        assertEquals("BORRADOR", e.app.publicacionUseCase.obtener(ana.id(), borrador.id()).estado());
        assertEquals(0L, e.app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), e.primeraPagina()).totalElementos());
    }

    @Test
    @DisplayName("Caso 2: una oferta con todo menos las franjas de disponibilidad tampoco se publica")
    void caso02_sinFranjasNoSePublica() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto borrador = e.app.publicacionUseCase.crearBorrador(ana.id(), e.mentoria("Mentoria de Java",
                "programacion", 80, 2, TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), List.of()));

        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> e.app.publicacionUseCase.publicar(ana.id(), borrador.id()));
        assertTrue(error.getMessage().contains("franja"), error.getMessage());
        assertEquals("BORRADOR", e.app.publicacionUseCase.obtener(ana.id(), borrador.id()).estado());
    }

    @Test
    @DisplayName("Caso 2: completar lo que faltaba (agregar la franja) habilita la publicacion")
    void caso02_completarElBorradorPermitePublicar() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PublicacionDto borrador = e.app.publicacionUseCase.crearBorrador(ana.id(), e.mentoria("Mentoria de Java",
                "programacion", 80, 2, TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), List.of()));
        assertThrows(ReglaDeNegocioException.class, () -> e.app.publicacionUseCase.publicar(ana.id(), borrador.id()));

        var franja = e.franja(java.time.Duration.ofDays(3));
        e.app.publicacionUseCase.agregarFranja(ana.id(), borrador.id(), franja.inicio(), franja.fin());
        PublicacionDto publicada = e.app.publicacionUseCase.publicar(ana.id(), borrador.id());

        assertEquals("PUBLICADA", publicada.estado());
        assertEquals(1L, e.app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), e.primeraPagina()).totalElementos());
    }

    @Test
    @DisplayName("Caso 2: un precio negativo no se acepta (nunca llega a haber una oferta publicable)")
    void caso02_precioNegativoSeRechaza() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        CrearPublicacionCommand conPrecioNegativo = new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Mentoria",
                "desc", "programacion", Modalidad.REMOTA, 60, 1, new BigDecimal("-5"), Set.of(), Set.of(), null,
                e.franjasDiarias(1));

        // segun donde lo valide el sistema (VO de precio o regla de publicacion) el error es de dominio o de entrada
        Throwable error = assertThrows(RuntimeException.class, () -> {
            PublicacionDto borrador = e.app.publicacionUseCase.crearBorrador(ana.id(), conPrecioNegativo);
            e.app.publicacionUseCase.publicar(ana.id(), borrador.id());
        });
        assertTrue(error instanceof ReglaDeNegocioException || error instanceof ValidacionException
                        || error instanceof IllegalArgumentException, error.getClass().getName());
        assertEquals(0L, e.app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), e.primeraPagina()).totalElementos());
    }
}
