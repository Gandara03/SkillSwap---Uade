package com.uade.skillswap.support;

import com.uade.skillswap.application.port.out.TransaccionPort;

import java.util.function.Supplier;

/** Fake de {@link TransaccionPort}: ejecuta el trabajo directamente y cuenta cuantas veces se uso. */
public class TransaccionSimulada implements TransaccionPort {

    private int ejecuciones;
    private int profundidad;

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        ejecuciones++;
        profundidad++;
        try {
            return trabajo.get();
        } finally {
            profundidad--;
        }
    }

    public int ejecuciones() {
        return ejecuciones;
    }

    public boolean enTransaccion() {
        return profundidad > 0;
    }

    public void reiniciarContador() {
        ejecuciones = 0;
    }
}
