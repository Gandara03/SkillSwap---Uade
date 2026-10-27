package com.uade.skillswap.presentation.ui.swing.contrato;

/**
 * Una operacion de {@link UiControllers} no se pudo completar. Lleva el codigo del error (por ejemplo
 * "SALDO_INSUFICIENTE") como texto, para que la vista no dependa de las capas internas, y el mensaje para el usuario.
 */
public class OperacionFallidaException extends RuntimeException {

    private final String codigo;

    public OperacionFallidaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
