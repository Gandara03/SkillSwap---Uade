package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.application.port.in.ContratacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: cada metodo valida la entrada y delega en ContratacionUseCase con el actor de la sesion. */
public class ContratacionController implements IContratacionController {

    private static final String CONTRATACION = "la contratacion";

    private final ContratacionUseCase contrataciones;
    private final SesionUseCase sesion;

    public ContratacionController(ContratacionUseCase contrataciones, SesionUseCase sesion) {
        this.contrataciones = contrataciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<ContratacionDto> solicitar(SolicitarContratacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            validar(comando);
            return contrataciones.solicitar(actor, comando);
        });
    }

    @Override
    public Resultado<ContratacionDto> aceptar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.aceptar(actor, contratacionId);
        });
    }

    @Override
    public Resultado<ContratacionDto> rechazar(Long contratacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            ValidadorEntrada.exigirTexto(motivo, ValidadorEntrada.LARGO_MOTIVO, "el motivo del rechazo");
            return contrataciones.rechazar(actor, contratacionId, motivo);
        });
    }

    @Override
    public Resultado<ContratacionDto> pagar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.pagar(actor, contratacionId);
        });
    }

    @Override
    public Resultado<ContratacionDto> iniciar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.iniciar(actor, contratacionId);
        });
    }

    @Override
    public Resultado<ContratacionDto> completar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.completar(actor, contratacionId);
        });
    }

    @Override
    public Resultado<ResultadoCancelacionDto> cancelar(Long contratacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            ValidadorEntrada.exigirTexto(motivo, ValidadorEntrada.LARGO_MOTIVO, "el motivo de la cancelacion");
            return contrataciones.cancelar(actor, contratacionId, motivo);
        });
    }

    @Override
    public Resultado<ContratacionDto> proponerReprogramacion(Long contratacionId, Long nuevaFranjaId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            ValidadorEntrada.exigirId(nuevaFranjaId, "la nueva franja");
            return contrataciones.proponerReprogramacion(actor, contratacionId, nuevaFranjaId);
        });
    }

    @Override
    public Resultado<ContratacionDto> responderReprogramacion(Long contratacionId, boolean acepta) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.responderReprogramacion(actor, contratacionId, acepta);
        });
    }

    @Override
    public Resultado<ContratacionDto> obtener(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.obtener(actor, contratacionId);
        });
    }

    @Override
    public Resultado<ComprobanteDto> comprobante(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            return contrataciones.comprobante(actor, contratacionId);
        });
    }

    @Override
    public Resultado<Pagina<HistorialDto>> historial(Long contratacionId, PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(contratacionId, CONTRATACION);
            ValidadorEntrada.exigirPagina(pagina);
            return contrataciones.historial(actor, contratacionId, pagina);
        });
    }

    @Override
    public Resultado<Pagina<ContratacionDto>> misContrataciones(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirPagina(pagina);
            return contrataciones.listarMias(actor, pagina);
        });
    }

    // ---------- validacion de entrada ----------

    private static void validar(SolicitarContratacionCommand c) {
        ValidadorEntrada.exigir(c, "los datos de la solicitud");
        ValidadorEntrada.exigirId(c.publicacionId(), "la publicacion");
        ValidadorEntrada.exigirId(c.franjaId(), "la franja");
        ValidadorEntrada.exigirTexto(c.mensaje(), ValidadorEntrada.LARGO_MENSAJE, "el mensaje");
        ValidadorEntrada.exigirTexto(c.objetivo(), ValidadorEntrada.LARGO_MENSAJE, "el objetivo");
    }
}
