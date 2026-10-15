package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.NO_ENCONTRADO;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integracion NotificacionController -> NotificacionUseCase -> JPA/H2. Bruno pide dos sesiones a Ana, asi que
 * Ana recibe dos notificaciones internas de SOLICITUD_RECIBIDA.
 */
@DisplayName("NotificacionController (JPA/H2): bandeja paginada, marcar leida y permisos")
class NotificacionControllerTest extends ControllerJpaTestBase {

    private INotificacionController notificaciones() {
        return app.getNotificacionController();
    }

    private void dosSolicitudesParaAna() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 2, 2);
        solicitar("u-bruno", mentoria, 0);
        solicitar("u-bruno", mentoria, 1);
    }

    /** Una notificacion de Ana, todavia sin leer. */
    private NotificacionDto unaDeAna() {
        como("u-ana");
        return ok(notificaciones().listar(true, primeraPagina())).contenido().get(0);
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("listar todas o solo las no leidas, paginado; marcarLeida la saca de las no leidas")
    void listarYMarcarLeida() {
        dosSolicitudesParaAna();
        como("u-ana");

        Pagina<NotificacionDto> noLeidas = ok(notificaciones().listar(true, primeraPagina()));
        assertEquals(2L, noLeidas.totalElementos());
        NotificacionDto primera = noLeidas.contenido().get(0);
        assertEquals(TipoEventoContratacion.SOLICITUD_RECIBIDA, primera.tipoEvento());

        assertTrue(ok(notificaciones().marcarLeida(primera.id())).leida());

        assertEquals(1L, ok(notificaciones().listar(true, primeraPagina())).totalElementos());
        Pagina<NotificacionDto> segundaDeUna = ok(notificaciones().listar(false, PaginaRequest.de(1, 1)));
        assertEquals(2L, segundaDeUna.totalElementos());
        assertEquals(1, segundaDeUna.contenido().size());
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("listar sin pagina y marcarLeida con id nulo o no positivo son VALIDACION")
    void entradaInvalida() {
        dosSolicitudesParaAna();
        como("u-ana");

        falla(VALIDACION, notificaciones().listar(false, null));
        falla(VALIDACION, notificaciones().marcarLeida(null));
        falla(VALIDACION, notificaciones().marcarLeida(0L));

        assertEquals(2L, ok(notificaciones().listar(true, primeraPagina())).totalElementos());
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("Bruno no puede marcar como leida una notificacion de Ana")
    void notificacionAjena() {
        dosSolicitudesParaAna();
        NotificacionDto deAna = unaDeAna();

        como("u-bruno");
        falla(NO_AUTORIZADO, notificaciones().marcarLeida(deAna.id()));

        assertFalse(unaDeAna().leida());
        assertEquals(2L, ok(notificaciones().listar(true, primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("sin sesion no se puede listar ni marcar")
    void sinSesionNoAutorizado() {
        dosSolicitudesParaAna();
        NotificacionDto deAna = unaDeAna();

        sinSesion();
        falla(NO_AUTORIZADO, notificaciones().listar(false, primeraPagina()));
        falla(NO_AUTORIZADO, notificaciones().marcarLeida(deAna.id()));
    }

    // ---------- otros codigos ----------

    @Test
    @DisplayName("marcar una notificacion inexistente es NO_ENCONTRADO")
    void inexistente() {
        dosSolicitudesParaAna();
        como("u-ana");

        falla(NO_ENCONTRADO, notificaciones().marcarLeida(999L));
    }
}
