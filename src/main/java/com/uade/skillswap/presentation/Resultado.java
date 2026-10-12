package com.uade.skillswap.presentation;

import com.uade.skillswap.application.exception.ErrorCode;

import java.util.function.Function;

/**
 * Resultado de una operacion de la UI: o trae un valor, o trae un error ya traducido para el usuario.
 * Los controllers nunca lanzan excepciones: la UI decide que mostrar mirando este objeto.
 */
public final class Resultado<T> {

    private final T valor;
    private final ErrorCode codigo;
    private final String mensaje;

    private Resultado(T valor, ErrorCode codigo, String mensaje) {
        this.valor = valor;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public static <T> Resultado<T> ok(T valor) {
        return new Resultado<>(valor, null, null);
    }

    public static <T> Resultado<T> error(ErrorCode codigo, String mensaje) {
        if (codigo == null) {
            throw new IllegalArgumentException("El codigo de error es obligatorio");
        }
        return new Resultado<>(null, codigo, mensaje);
    }

    public boolean esOk() {
        return codigo == null;
    }

    public boolean esError() {
        return !esOk();
    }

    /** Valor de una operacion exitosa. Pedirlo sobre un error es un bug del que llama: consultar antes esOk(). */
    public T valor() {
        if (!esOk()) {
            throw new IllegalStateException("El resultado es un error (" + codigo + "): " + mensaje);
        }
        return valor;
    }

    /** Codigo de error, o null si la operacion salio bien. */
    public ErrorCode codigo() {
        return codigo;
    }

    /** Mensaje para el usuario final, o null si la operacion salio bien. */
    public String mensaje() {
        return mensaje;
    }

    public <R> Resultado<R> mapear(Function<T, R> f) {
        return esOk() ? ok(f.apply(valor)) : error(codigo, mensaje);
    }

    @Override
    public String toString() {
        return esOk() ? "Resultado.ok(" + valor + ")" : "Resultado.error(" + codigo + ": " + mensaje + ")";
    }
}
