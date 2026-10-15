package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.NO_ENCONTRADO;
import static com.uade.skillswap.application.exception.ErrorCode.REGLA_DE_NEGOCIO;
import static com.uade.skillswap.application.exception.ErrorCode.TRANSICION_INVALIDA;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integracion ContratacionController -> ContratacionUseCase -> JPA/H2. Ana publica (proveedora), Bruno contrata
 * (cliente) y Cecilia es un tercero que no participa.
 */
@DisplayName("ContratacionController (JPA/H2): ciclo de vida, reprogramacion, historial paginado y permisos")
class ContratacionControllerTest extends ControllerJpaTestBase {

    private PublicacionDto mentoria;

    private IContratacionController contrataciones() {
        return app.getContratacionController();
    }

    /** Ana publica una mentoria de 40 SC con cupo 2 y 3 franjas; Bruno y Cecilia tienen perfil. */
    private void escenario() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        mentoria = publicarMentoria("u-ana", 40, 2, 3);
    }

    private Long franjaId(int indice) {
        return mentoria.franjas().get(indice).id();
    }

    private SolicitarContratacionCommand solicitud(Long franjaId, String mensaje, String objetivo) {
        return new SolicitarContratacionCommand(mentoria.id(), franjaId, mensaje, objetivo);
    }

    private static List<String> estados(Pagina<HistorialDto> historial) {
        return historial.contenido().stream().map(HistorialDto::estadoNuevo).toList();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("solicitar, aceptar, pagar, iniciar y completar; despues el cliente consulta el comprobante")
    void flujoCompletoYComprobante() {
        escenario();

        ContratacionDto completada = contratarYCompletar("u-ana", "u-bruno", mentoria, 0);
        como("u-bruno");
        ComprobanteDto comprobante = ok(contrataciones().comprobante(completada.id()));

        assertEquals("COMPLETADA", completada.estado());
        assertEquals(completada.id(), comprobante.contratacionId());
        assertEquals(0, sc(40).compareTo(comprobante.monto()));
        assertEquals(1L, ok(contrataciones().misContrataciones(primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("el historial se pagina por el controller, en orden cronologico")
    void historialPaginado() {
        escenario();
        ContratacionDto completada = contratarYCompletar("u-ana", "u-bruno", mentoria, 0);
        como("u-ana");

        Pagina<HistorialDto> primera = ok(contrataciones().historial(completada.id(), PaginaRequest.de(0, 2)));
        Pagina<HistorialDto> ultima = ok(contrataciones().historial(completada.id(), PaginaRequest.de(2, 2)));

        assertEquals(List.of("PENDIENTE", "ACEPTADA"), estados(primera));
        assertEquals(5L, primera.totalElementos());
        assertEquals(List.of("COMPLETADA"), estados(ultima));
    }

    @Test
    @DisplayName("la proveedora rechaza con motivo y el cliente cancela una contratacion pagada con reintegro")
    void rechazarYCancelar() {
        escenario();
        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);
        como("u-ana");
        ContratacionDto rechazada = ok(contrataciones().rechazar(pendiente.id(), "No tengo disponibilidad"));

        ContratacionDto pagada = contratarYPagar("u-ana", "u-bruno", mentoria, 1);
        como("u-bruno");
        ResultadoCancelacionDto cancelacion = ok(contrataciones().cancelar(pagada.id(), "Surgio un imprevisto"));

        assertEquals("RECHAZADA", rechazada.estado());
        assertEquals("CANCELADA", cancelacion.contratacion().estado());
        assertTrue(cancelacion.reintegroCliente().signum() > 0, "se reintegra segun la politica");
    }

    @Test
    @DisplayName("reprogramacion: el cliente propone otra franja y la proveedora la acepta")
    void reprogramar() {
        escenario();
        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);
        aceptar("u-ana", pendiente.id());
        como("u-bruno");

        ContratacionDto propuesta = ok(contrataciones().proponerReprogramacion(pendiente.id(), franjaId(2)));
        como("u-ana");
        ContratacionDto reprogramada = ok(contrataciones().responderReprogramacion(pendiente.id(), true));

        assertEquals(franjaId(2), propuesta.propuestaReprogramacion().nuevaFranjaId());
        assertEquals(franjaId(2), reprogramada.franjaId());
        assertNull(reprogramada.propuestaReprogramacion());
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("solicitar sin datos obligatorios o con textos demasiado largos es VALIDACION y no crea nada")
    void solicitarInvalido() {
        escenario();
        como("u-bruno");

        falla(VALIDACION, contrataciones().solicitar(null));
        falla(VALIDACION, contrataciones().solicitar(
                new SolicitarContratacionCommand(null, franjaId(0), "Hola", "Aprender")));
        falla(VALIDACION, contrataciones().solicitar(solicitud(null, "Hola", "Aprender")));
        falla(VALIDACION, contrataciones().solicitar(solicitud(franjaId(0), "  ", "Aprender")));
        falla(VALIDACION, contrataciones().solicitar(solicitud(franjaId(0), "Hola", null)));
        falla(VALIDACION, contrataciones().solicitar(solicitud(franjaId(0), texto(2001), "Aprender")));

        assertEquals(0L, ok(contrataciones().misContrataciones(primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("ids, motivos y paginas invalidos son VALIDACION y la contratacion no cambia")
    void operacionesInvalidas() {
        escenario();
        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);
        como("u-ana");

        falla(VALIDACION, contrataciones().aceptar(null));
        falla(VALIDACION, contrataciones().aceptar(0L));
        falla(VALIDACION, contrataciones().obtener(-3L));
        falla(VALIDACION, contrataciones().rechazar(pendiente.id(), null));
        falla(VALIDACION, contrataciones().rechazar(pendiente.id(), texto(1001)));
        falla(VALIDACION, contrataciones().cancelar(pendiente.id(), " "));
        falla(VALIDACION, contrataciones().proponerReprogramacion(pendiente.id(), null));
        falla(VALIDACION, contrataciones().historial(pendiente.id(), null));
        falla(VALIDACION, contrataciones().misContrataciones(null));

        assertEquals("PENDIENTE", contratacionVista("u-ana", pendiente.id()).estado());
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("un tercero no puede ver, aceptar, rechazar ni consultar el historial de una contratacion ajena")
    void terceroNoAutorizado() {
        escenario();
        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);
        como("u-cecilia");

        falla(NO_AUTORIZADO, contrataciones().obtener(pendiente.id()));
        falla(NO_AUTORIZADO, contrataciones().aceptar(pendiente.id()));
        falla(NO_AUTORIZADO, contrataciones().rechazar(pendiente.id(), "No me gusta"));
        falla(NO_AUTORIZADO, contrataciones().historial(pendiente.id(), primeraPagina()));
        falla(NO_AUTORIZADO, contrataciones().comprobante(pendiente.id()));

        assertEquals(0L, ok(contrataciones().misContrataciones(primeraPagina())).totalElementos());
        assertEquals("PENDIENTE", contratacionVista("u-ana", pendiente.id()).estado());
    }

    @Test
    @DisplayName("cada parte hace lo suyo: el cliente no acepta, la proveedora no paga, quien propone no responde")
    void cadaParteSoloLoSuyo() {
        escenario();
        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);

        como("u-bruno");
        falla(NO_AUTORIZADO, contrataciones().aceptar(pendiente.id()));
        aceptar("u-ana", pendiente.id());
        como("u-ana");
        falla(NO_AUTORIZADO, contrataciones().pagar(pendiente.id()));
        como("u-bruno");
        ok(contrataciones().proponerReprogramacion(pendiente.id(), franjaId(2)));
        falla(NO_AUTORIZADO, contrataciones().responderReprogramacion(pendiente.id(), true));

        ContratacionDto actual = contratacionVista("u-bruno", pendiente.id());
        assertEquals("ACEPTADA", actual.estado());
        assertEquals(franjaId(0), actual.franjaId());
    }

    @Test
    @DisplayName("sin sesion no se puede operar (la sesion se mira antes que la entrada)")
    void sinSesionNoAutorizado() {
        sinSesion();

        falla(NO_AUTORIZADO, contrataciones().solicitar(new SolicitarContratacionCommand(1L, 1L, "Hola", "Aprender")));
        falla(NO_AUTORIZADO, contrataciones().misContrataciones(primeraPagina()));
        falla(NO_AUTORIZADO, contrataciones().aceptar(null));
    }

    // ---------- otros codigos ----------

    @Test
    @DisplayName("pagar dos veces es TRANSICION_INVALIDA y el saldo se debita una sola vez")
    void pagarDosVeces() {
        escenario();
        ContratacionDto pagada = contratarYPagar("u-ana", "u-bruno", mentoria, 0);
        como("u-bruno");

        falla(TRANSICION_INVALIDA, contrataciones().pagar(pagada.id()));

        assertEquals(0, sc(60).compareTo(saldo("u-bruno")));
    }

    @Test
    @DisplayName("solicitar lo propio o el comprobante antes de tiempo: REGLA_DE_NEGOCIO; inexistente: NO_ENCONTRADO")
    void reglasDeNegocioYNoEncontrada() {
        escenario();
        como("u-ana");
        falla(REGLA_DE_NEGOCIO, contrataciones().solicitar(solicitud(franjaId(0), "Hola", "Aprender")));
        falla(NO_ENCONTRADO, contrataciones().obtener(999L));

        ContratacionDto pendiente = solicitar("u-bruno", mentoria, 0);
        falla(REGLA_DE_NEGOCIO, contrataciones().comprobante(pendiente.id()));
    }
}
