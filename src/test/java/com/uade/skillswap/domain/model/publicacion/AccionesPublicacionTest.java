package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicacionFactory;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.CANCELAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.CERRAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.PAUSAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.PUBLICAR;
import static com.uade.skillswap.domain.model.publicacion.AccionPublicacion.REANUDAR;
import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Publicacion: acciones del autor segun el estado (State)")
class AccionesPublicacionTest {

    private static final List<String> ESTADOS = List.of("BORRADOR", "PUBLICADA", "PAUSADA", "CERRADA", "CANCELADA");

    @Test
    @DisplayName("cada estado declara exactamente las transiciones que sus metodos aceptan")
    void declaradasCoincidenConLasQueFuncionan() {
        for (String estado : ESTADOS) {
            Set<AccionPublicacion> permitidas = EstadoPublicacionFactory.desdeNombre(estado).accionesPermitidas();
            for (AccionPublicacion accion : AccionPublicacion.values()) {
                Publicacion p = enEstado(estado);
                String caso = estado + " / " + accion;
                if (permitidas.contains(accion)) {
                    assertDoesNotThrow(() -> ejecutar(p, accion), caso);
                } else {
                    assertThrows(TransicionInvalidaException.class, () -> ejecutar(p, accion), caso);
                }
            }
        }
    }

    @Test
    @DisplayName("las acciones del autor siguen al estado y vienen en el orden del enum")
    void accionesDelAutor() {
        assertEquals(List.of(PUBLICAR, CANCELAR), enEstado("BORRADOR").accionesDelAutor());
        assertEquals(List.of(PAUSAR, CERRAR, CANCELAR), enEstado("PUBLICADA").accionesDelAutor());
        assertEquals(List.of(REANUDAR, CERRAR, CANCELAR), enEstado("PAUSADA").accionesDelAutor());
        assertEquals(List.of(), enEstado("CERRADA").accionesDelAutor());
        assertEquals(List.of(), enEstado("CANCELADA").accionesDelAutor());
    }

    // ---------- soporte ----------

    /** Publicacion completa (con franja), llevada al estado pedido por sus propias transiciones. */
    private static Publicacion enEstado(String estado) {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        switch (estado) {
            case "BORRADOR" -> { }
            case "PUBLICADA" -> p.publicar(PROVEEDOR, AHORA);
            case "PAUSADA" -> {
                p.publicar(PROVEEDOR, AHORA);
                p.pausar(PROVEEDOR, AHORA);
            }
            case "CERRADA" -> {
                p.publicar(PROVEEDOR, AHORA);
                p.cerrar(PROVEEDOR, AHORA);
            }
            case "CANCELADA" -> p.cancelar(PROVEEDOR, "ya no la dicto", AHORA);
            default -> throw new IllegalArgumentException(estado);
        }
        return p;
    }

    private static void ejecutar(Publicacion p, AccionPublicacion accion) {
        switch (accion) {
            case PUBLICAR -> p.publicar(PROVEEDOR, AHORA);
            case PAUSAR -> p.pausar(PROVEEDOR, AHORA);
            case REANUDAR -> p.reanudar(PROVEEDOR, AHORA);
            case CERRAR -> p.cerrar(PROVEEDOR, AHORA);
            case CANCELAR -> p.cancelar(PROVEEDOR, "motivo", AHORA);
        }
    }
}
