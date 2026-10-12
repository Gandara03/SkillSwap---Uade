package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.presentation.ManejadorErrores;
import com.uade.skillswap.presentation.Resultado;

import java.util.function.Function;
import java.util.function.Supplier;

/** Ejecuta un caso de uso y lo convierte en Resultado: es lo unico que los controllers necesitan para "no lanzar". */
final class Ejecucion {

    private Ejecucion() {
    }

    static <T> Resultado<T> sinActor(Supplier<T> accion) {
        try {
            return Resultado.ok(accion.get());
        } catch (RuntimeException e) {
            return ManejadorErrores.traducir(e);
        }
    }

    /** El actor (perfil de la sesion actual) se resuelve dentro del try: sin sesion es un error controlado. */
    static <T> Resultado<T> conActor(SesionUseCase sesion, Function<Long, T> accion) {
        try {
            return Resultado.ok(accion.apply(sesion.perfilIdActual()));
        } catch (RuntimeException e) {
            return ManejadorErrores.traducir(e);
        }
    }

    static Resultado<Void> conActorSinValor(SesionUseCase sesion, java.util.function.Consumer<Long> accion) {
        return conActor(sesion, actor -> {
            accion.accept(actor);
            return null;
        });
    }
}
