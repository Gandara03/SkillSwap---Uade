package com.uade.skillswap.application.port.out;

import java.util.function.Supplier;

/** Unidad de trabajo: todo lo que corre dentro de {@code ejecutar} se confirma o se revierte en conjunto. */
public interface TransaccionPort {

    <T> T ejecutar(Supplier<T> trabajo);

    default void ejecutar(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
