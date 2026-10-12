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

import java.util.List;

/** Adaptador delgado: cada metodo delega en ContratacionUseCase con el actor de la sesion. */
public class ContratacionController implements IContratacionController {

    private final ContratacionUseCase contrataciones;
    private final SesionUseCase sesion;

    public ContratacionController(ContratacionUseCase contrataciones, SesionUseCase sesion) {
        this.contrataciones = contrataciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<ContratacionDto> solicitar(SolicitarContratacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.solicitar(actor, comando));
    }

    @Override
    public Resultado<ContratacionDto> aceptar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.aceptar(actor, contratacionId));
    }

    @Override
    public Resultado<ContratacionDto> rechazar(Long contratacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.rechazar(actor, contratacionId, motivo));
    }

    @Override
    public Resultado<ContratacionDto> pagar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.pagar(actor, contratacionId));
    }

    @Override
    public Resultado<ContratacionDto> iniciar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.iniciar(actor, contratacionId));
    }

    @Override
    public Resultado<ContratacionDto> completar(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.completar(actor, contratacionId));
    }

    @Override
    public Resultado<ResultadoCancelacionDto> cancelar(Long contratacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.cancelar(actor, contratacionId, motivo));
    }

    @Override
    public Resultado<ContratacionDto> proponerReprogramacion(Long contratacionId, Long nuevaFranjaId) {
        return Ejecucion.conActor(sesion,
                actor -> contrataciones.proponerReprogramacion(actor, contratacionId, nuevaFranjaId));
    }

    @Override
    public Resultado<ContratacionDto> responderReprogramacion(Long contratacionId, boolean acepta) {
        return Ejecucion.conActor(sesion,
                actor -> contrataciones.responderReprogramacion(actor, contratacionId, acepta));
    }

    @Override
    public Resultado<ContratacionDto> obtener(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.obtener(actor, contratacionId));
    }

    @Override
    public Resultado<ComprobanteDto> comprobante(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.comprobante(actor, contratacionId));
    }

    @Override
    public Resultado<List<HistorialDto>> historial(Long contratacionId) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.historial(actor, contratacionId));
    }

    @Override
    public Resultado<Pagina<ContratacionDto>> misContrataciones(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> contrataciones.listarMias(actor, pagina));
    }
}
