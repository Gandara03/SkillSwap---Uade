package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.presentation.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 2 (JPA/H2): rechazar la publicacion de una oferta incompleta.
 * El borrador incompleto se guarda; publicar falla con REGLA_DE_NEGOCIO y el estado persistido sigue en BORRADOR.
 */
@DisplayName("Caso 2 (JPA/H2): rechazar la publicacion de una oferta incompleta")
class Caso02OfertaIncompletaJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 2: sin titulo, categoria, precio ni franjas no se puede publicar y sigue en BORRADOR")
    void caso02_rechazarPublicacionDeOfertaIncompleta() {
        profesional("u-ana", "Ana");
        como("u-ana");
        CrearPublicacionCommand incompleta = new CrearPublicacionCommand(TipoPublicacion.MENTORIA, null, null, null,
                Modalidad.REMOTA, 60, 1, null, Set.of(), Set.of(), null, List.of());
        PublicacionDto borrador = ok(app.getPublicacionController().crearBorrador(incompleta));
        assertEquals("BORRADOR", borrador.estado());

        Resultado<PublicacionDto> intento = app.getPublicacionController().publicar(borrador.id());

        assertTrue(intento.esError(), "publicar una oferta incompleta debe fallar");
        assertEquals(ErrorCode.REGLA_DE_NEGOCIO, intento.codigo());
        assertTrue(intento.mensaje().contains("titulo"), intento.mensaje());
        assertTrue(intento.mensaje().contains("categoria"), intento.mensaje());
        assertTrue(intento.mensaje().contains("franja"), intento.mensaje());

        // lo persistido no cambio y la oferta no es visible en la busqueda
        assertEquals("BORRADOR", publicacionVista("u-ana", borrador.id()).estado());
        assertEquals(0L, ok(app.getBusquedaController().buscar(FiltroBusqueda.vacio(), primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("Caso 2: completar lo que faltaba (agregar la franja) habilita la publicacion")
    void caso02_completarElBorradorPermitePublicar() {
        profesional("u-ana", "Ana");
        como("u-ana");
        PublicacionDto borrador = ok(app.getPublicacionController().crearBorrador(mentoria("Mentoria de Java",
                "programacion", 80, 2, TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), List.of())));
        assertEquals(ErrorCode.REGLA_DE_NEGOCIO, app.getPublicacionController().publicar(borrador.id()).codigo());

        var franja = franja(java.time.Duration.ofDays(3));
        ok(app.getPublicacionController().agregarFranja(borrador.id(), franja.inicio(), franja.fin()));
        PublicacionDto publicada = ok(app.getPublicacionController().publicar(borrador.id()));

        assertEquals("PUBLICADA", publicada.estado());
        assertEquals(1L, ok(app.getBusquedaController().buscar(FiltroBusqueda.vacio(), primeraPagina())).totalElementos());
    }
}
