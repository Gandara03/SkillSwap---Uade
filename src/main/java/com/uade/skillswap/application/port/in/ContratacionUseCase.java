package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

/**
 * Cada operacion que cambia estado corre en UNA transaccion (cambio de estado + franja/cupo + wallet).
 * Las notificaciones (Observer) se despachan despues del commit.
 */
public interface ContratacionUseCase {

    ContratacionDto solicitar(Long actorId, SolicitarContratacionCommand comando);

    /** Solo el proveedor. Reserva la franja y el cupo en la misma transaccion. */
    ContratacionDto aceptar(Long actorId, Long contratacionId);

    ContratacionDto rechazar(Long actorId, Long contratacionId, String motivo);

    /** Solo el cliente. Debita al cliente y retiene el monto en la contratacion (escrow). */
    ContratacionDto pagar(Long actorId, Long contratacionId);

    ContratacionDto iniciar(Long actorId, Long contratacionId);

    /** Acredita al proveedor lo retenido. */
    ContratacionDto completar(Long actorId, Long contratacionId);

    /** Reintegra al cliente segun la politica de la publicacion y libera franja/cupo. */
    ResultadoCancelacionDto cancelar(Long actorId, Long contratacionId, String motivo);

    ContratacionDto proponerReprogramacion(Long actorId, Long contratacionId, Long nuevaFranjaId);

    /** Solo la contraparte de quien propuso. Si acepta, mueve la reserva de franja. */
    ContratacionDto responderReprogramacion(Long actorId, Long contratacionId, boolean acepta);

    ContratacionDto obtener(Long actorId, Long contratacionId);

    ComprobanteDto comprobante(Long actorId, Long contratacionId);

    /** Transiciones en orden cronologico, paginadas. Solo los participantes. */
    Pagina<HistorialDto> historial(Long actorId, Long contratacionId, PaginaRequest pagina);

    Pagina<ContratacionDto> listarMias(Long actorId, PaginaRequest pagina);
}
