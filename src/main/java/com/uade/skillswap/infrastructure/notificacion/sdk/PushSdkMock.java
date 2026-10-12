package com.uade.skillswap.infrastructure.notificacion.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mock del SDK de push (Adaptee): API propia; lo enviado queda en una lista inspeccionable. */
public class PushSdkMock {

    public record Push(String dispositivo, String titulo, String texto) { }

    private final List<Push> enviados = new ArrayList<>();

    public void pushear(String dispositivo, String titulo, String texto) {
        enviados.add(new Push(dispositivo, titulo, texto));
    }

    public List<Push> enviados() {
        return Collections.unmodifiableList(enviados);
    }
}
