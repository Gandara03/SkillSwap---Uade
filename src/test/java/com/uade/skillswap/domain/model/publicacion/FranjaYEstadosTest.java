package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoBorrador;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoCancelada;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoCerrada;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPausada;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicacion;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicacionFactory;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Franja y fabrica de estados de Publicacion")
class FranjaYEstadosTest {

    @Test
    @DisplayName("Franja exige inicio anterior al fin")
    void franjaValida() {
        assertThrows(ReglaDeNegocioException.class, () -> Franja.nueva(AHORA, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Franja.nueva(AHORA.plusHours(1), AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Franja.nueva(null, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Franja.nueva(AHORA, null));
        Franja f = Franja.nueva(AHORA, AHORA.plusHours(1));
        assertFalse(f.estaOcupada());
        assertNull(f.getId());
    }

    @Test
    @DisplayName("yaComenzo es verdadero si el inicio es ahora o ya paso")
    void yaComenzo() {
        Franja f = Franja.nueva(AHORA.plusHours(1), AHORA.plusHours(2));
        assertFalse(f.yaComenzo(AHORA));
        assertTrue(f.yaComenzo(AHORA.plusHours(1)));
        assertTrue(f.yaComenzo(AHORA.plusHours(5)));
    }

    @Test
    @DisplayName("la fabrica reconstruye cada estado desde su nombre")
    void fabrica() {
        assertInstanceOf(EstadoBorrador.class, EstadoPublicacionFactory.desdeNombre("BORRADOR"));
        assertInstanceOf(EstadoPublicada.class, EstadoPublicacionFactory.desdeNombre("PUBLICADA"));
        assertInstanceOf(EstadoPausada.class, EstadoPublicacionFactory.desdeNombre("PAUSADA"));
        assertInstanceOf(EstadoCerrada.class, EstadoPublicacionFactory.desdeNombre("CERRADA"));
        assertInstanceOf(EstadoCancelada.class, EstadoPublicacionFactory.desdeNombre("CANCELADA"));
        assertInstanceOf(EstadoBorrador.class, EstadoPublicacionFactory.desdeNombre(null));
        assertInstanceOf(EstadoBorrador.class, EstadoPublicacionFactory.desdeNombre(" "));
        assertThrows(IllegalArgumentException.class, () -> EstadoPublicacionFactory.desdeNombre("XYZ"));
    }

    @Test
    @DisplayName("capacidades de cada estado: edicion, solicitudes, visibilidad y terminalidad")
    void capacidades() {
        EstadoPublicacion borrador = new EstadoBorrador();
        assertTrue(borrador.permiteEdicion());
        assertFalse(borrador.admiteSolicitudes());
        assertFalse(borrador.esVisiblePublicamente());
        assertFalse(borrador.esTerminal());

        EstadoPublicacion publicada = new EstadoPublicada();
        assertFalse(publicada.permiteEdicion());
        assertTrue(publicada.admiteSolicitudes());
        assertTrue(publicada.esVisiblePublicamente());

        EstadoPublicacion pausada = new EstadoPausada();
        assertFalse(pausada.admiteSolicitudes());
        assertFalse(pausada.esVisiblePublicamente());
        assertFalse(pausada.esTerminal());

        assertTrue(new EstadoCerrada().esTerminal());
        assertTrue(new EstadoCancelada().esTerminal());
        assertEquals("CERRADA", new EstadoCerrada().getNombre());
    }
}
