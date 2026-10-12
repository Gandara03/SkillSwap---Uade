package com.uade.skillswap.presentation;

import com.uade.skillswap.application.exception.AplicacionException;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.domain.exception.CupoAgotadoException;
import com.uade.skillswap.domain.exception.DominioException;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unico lugar donde las excepciones se convierten en errores para el usuario.
 * Los errores esperados (aplicacion y dominio) muestran su mensaje; cualquier otra cosa es un error interno:
 * al usuario le llega un texto generico y el detalle tecnico va solo al log.
 */
public final class ManejadorErrores {

    static final String MENSAJE_INTERNO = "Ocurrió un error inesperado. Probá de nuevo más tarde.";
    static final String MENSAJE_CONFLICTO =
            "Otra persona modificó este recurso al mismo tiempo. Volvé a intentar la operación.";
    static final String MENSAJE_SIN_PERMISO = "No tenés permiso para realizar esta operación.";

    private static final Logger LOG = Logger.getLogger(ManejadorErrores.class.getName());

    private ManejadorErrores() {
    }

    public static <T> Resultado<T> traducir(Throwable error) {
        if (error instanceof AplicacionException app) {
            return Resultado.error(app.getCodigo(), mensajeDe(app));
        }
        if (error instanceof DominioException dominio) {
            return Resultado.error(codigoDe(dominio), dominio.getMessage());
        }
        LOG.log(Level.WARNING, "Error interno no controlado", error);
        return Resultado.error(ErrorCode.ERROR_INTERNO, MENSAJE_INTERNO);
    }

    static ErrorCode codigoDe(DominioException e) {
        if (e instanceof SaldoInsuficienteException) {
            return ErrorCode.SALDO_INSUFICIENTE;
        }
        if (e instanceof TransicionInvalidaException) {
            return ErrorCode.TRANSICION_INVALIDA;
        }
        if (e instanceof CupoAgotadoException) {
            return ErrorCode.CUPO_AGOTADO;
        }
        if (e instanceof FranjaNoDisponibleException) {
            return ErrorCode.FRANJA_NO_DISPONIBLE;
        }
        return ErrorCode.REGLA_DE_NEGOCIO;
    }

    private static String mensajeDe(AplicacionException e) {
        boolean sinTexto = e.getMessage() == null || e.getMessage().isBlank();
        return switch (e.getCodigo()) {
            case CONFLICTO_CONCURRENCIA -> MENSAJE_CONFLICTO;
            case ERROR_INTERNO -> MENSAJE_INTERNO;
            case NO_AUTORIZADO -> sinTexto ? MENSAJE_SIN_PERMISO : e.getMessage();
            default -> sinTexto ? "No se pudo completar la operación." : e.getMessage();
        };
    }
}
