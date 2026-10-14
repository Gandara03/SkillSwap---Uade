package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para el ciclo de vida de una contratacion. */
public interface IContratacionController {

    Resultado<ContratacionDto> solicitar(SolicitarContratacionCommand comando);

    Resultado<ContratacionDto> aceptar(Long contratacionId);

    Resultado<ContratacionDto> rechazar(Long contratacionId, String motivo);

    Resultado<ContratacionDto> pagar(Long contratacionId);

    Resultado<ContratacionDto> iniciar(Long contratacionId);

    Resultado<ContratacionDto> completar(Long contratacionId);

    Resultado<ResultadoCancelacionDto> cancelar(Long contratacionId, String motivo);

    Resultado<ContratacionDto> proponerReprogramacion(Long contratacionId, Long nuevaFranjaId);

    Resultado<ContratacionDto> responderReprogramacion(Long contratacionId, boolean acepta);

    Resultado<ContratacionDto> obtener(Long contratacionId);

    Resultado<ComprobanteDto> comprobante(Long contratacionId);

    Resultado<Pagina<HistorialDto>> historial(Long contratacionId, PaginaRequest pagina);

    Resultado<Pagina<ContratacionDto>> misContrataciones(PaginaRequest pagina);
}
