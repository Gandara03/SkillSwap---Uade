package com.uade.skillswap.infrastructure.notificacion.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mock del SDK de correo (Adaptee): API propia; lo enviado queda en una lista inspeccionable. */
public class EmailSdkMock {

    public record Correo(String destino, String asunto, String cuerpo) { }

    private final List<Correo> enviados = new ArrayList<>();

    public void enviarCorreo(String destino, String asunto, String cuerpo) {
        enviados.add(new Correo(destino, asunto, cuerpo));
    }

    public List<Correo> enviados() {
        return Collections.unmodifiableList(enviados);
    }
}
