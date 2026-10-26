package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.ACEPTAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.CALIFICAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.CANCELAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.COMPLETAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.INICIAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.PAGAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.RECHAZAR;
import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.CLIENTE;
import static com.uade.skillswap.support.Datos.OTRO;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Contratacion: acciones disponibles segun el estado (State) y la parte del actor")
class AccionesContratacionTest {

    private static final List<String> ESTADOS =
            List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA", "RECHAZADA", "CANCELADA");
    private static final List<AccionContratacion> TRANSICIONES =
            List.of(ACEPTAR, RECHAZAR, PAGAR, INICIAR, COMPLETAR, CANCELAR);

    @Test
    @DisplayName("cada estado declara exactamente las transiciones que sus metodos aceptan")
    void declaradasCoincidenConLasQueFuncionan() {
        for (String estado : ESTADOS) {
            Set<AccionContratacion> permitidas = EstadoContratacionFactory.desdeNombre(estado).accionesPermitidas();
            for (AccionContratacion accion : TRANSICIONES) {
                Contratacion c = enEstado(estado);
                String caso = estado + " / " + accion;
                if (permitidas.contains(accion)) {
                    assertDoesNotThrow(() -> ejecutar(c, accion), caso);
                } else {
                    assertThrows(TransicionInvalidaException.class, () -> ejecutar(c, accion), caso);
                }
            }
        }
    }

    @Test
    @DisplayName("solo COMPLETADA declara CALIFICAR, y es el unico estado en que se puede registrar una calificacion")
    void calificarSoloEnCompletada() {
        for (String estado : ESTADOS) {
            boolean declara = EstadoContratacionFactory.desdeNombre(estado).accionesPermitidas().contains(CALIFICAR);
            assertEquals("COMPLETADA".equals(estado), declara, estado);
            Contratacion c = enEstado(estado);
            if (declara) {
                assertDoesNotThrow(() -> ejecutar(c, CALIFICAR), estado);
            } else {
                assertThrows(ReglaDeNegocioException.class, () -> ejecutar(c, CALIFICAR), estado);
            }
        }
    }

    @Test
    @DisplayName("PENDIENTE: el proveedor acepta o rechaza; el cliente no puede retirar la solicitud")
    void pendiente() {
        assertAcciones(Datos.pendiente(), List.of(ACEPTAR, RECHAZAR), List.of());
    }

    @Test
    @DisplayName("ACEPTADA: el cliente paga o cancela; el proveedor solo cancela")
    void aceptada() {
        assertAcciones(Datos.aceptada(), List.of(CANCELAR), List.of(PAGAR, CANCELAR));
    }

    @Test
    @DisplayName("CONFIRMADA: el proveedor inicia o cancela; el cliente solo cancela")
    void confirmada() {
        assertAcciones(Datos.confirmada(), List.of(INICIAR, CANCELAR), List.of(CANCELAR));
    }

    @Test
    @DisplayName("EN_CURSO: solo el proveedor completa")
    void enCurso() {
        assertAcciones(Datos.enCurso(), List.of(COMPLETAR), List.of());
    }

    @Test
    @DisplayName("COMPLETADA: las dos partes pueden calificar")
    void completada() {
        assertAcciones(Datos.completada(), List.of(CALIFICAR), List.of(CALIFICAR));
    }

    @Test
    @DisplayName("RECHAZADA y CANCELADA no tienen acciones para nadie")
    void terminalesSinAcciones() {
        assertAcciones(enEstado("RECHAZADA"), List.of(), List.of());
        assertAcciones(enEstado("CANCELADA"), List.of(), List.of());
    }

    @Test
    @DisplayName("las acciones de cada parte salen del enum: los mensajes de permiso no cambiaron")
    void mensajesDePermiso() {
        Contratacion pendiente = Datos.pendiente();
        Contratacion aceptada = Datos.aceptada();
        Contratacion confirmada = Datos.confirmada();

        assertEquals("Solo el proveedor puede realizar esta accion", assertThrows(ReglaDeNegocioException.class,
                () -> pendiente.aceptar(CLIENTE, AHORA)).getMessage());
        assertEquals("Solo el cliente puede realizar esta accion", assertThrows(ReglaDeNegocioException.class,
                () -> aceptada.pagar(PROVEEDOR, AHORA)).getMessage());
        assertEquals("Solo los participantes de la contratacion pueden realizar esta accion",
                assertThrows(ReglaDeNegocioException.class, () -> confirmada.cancelar(OTRO, "x", AHORA)).getMessage());
        assertEquals("Solo los participantes de la contratacion pueden realizar esta accion",
                assertThrows(ReglaDeNegocioException.class,
                        () -> confirmada.proponerReprogramacion(OTRO, 2L, AHORA)).getMessage());
    }

    // ---------- soporte ----------

    /** Revisa las acciones de proveedor y cliente; un tercero nunca tiene ninguna. */
    private static void assertAcciones(Contratacion c, List<AccionContratacion> proveedor,
                                       List<AccionContratacion> cliente) {
        assertEquals(proveedor, c.accionesDisponiblesPara(PROVEEDOR), "proveedor en " + c.getEstado());
        assertEquals(cliente, c.accionesDisponiblesPara(CLIENTE), "cliente en " + c.getEstado());
        assertEquals(List.of(), c.accionesDisponiblesPara(OTRO), "tercero en " + c.getEstado());
    }

    private static Contratacion enEstado(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> Datos.pendiente();
            case "ACEPTADA" -> Datos.aceptada();
            case "CONFIRMADA" -> Datos.confirmada();
            case "EN_CURSO" -> Datos.enCurso();
            case "COMPLETADA" -> Datos.completada();
            case "RECHAZADA" -> rechazada();
            case "CANCELADA" -> cancelada();
            default -> throw new IllegalArgumentException(estado);
        };
    }

    private static Contratacion rechazada() {
        Contratacion c = Datos.pendiente();
        c.rechazar(PROVEEDOR, "sin disponibilidad", AHORA);
        return c;
    }

    private static Contratacion cancelada() {
        Contratacion c = Datos.aceptada();
        c.cancelar(CLIENTE, "no puedo", AHORA);
        return c;
    }

    /** Ejecuta la accion con el actor que le corresponde por su parte, asi solo puede fallar por el estado. */
    private static void ejecutar(Contratacion c, AccionContratacion accion) {
        Long actor = accion.getParte() == ParteContratacion.CLIENTE ? CLIENTE : PROVEEDOR;
        switch (accion) {
            case ACEPTAR -> c.aceptar(actor, AHORA);
            case RECHAZAR -> c.rechazar(actor, "motivo", AHORA);
            case PAGAR -> c.pagar(actor, AHORA);
            case INICIAR -> c.iniciar(actor, AHORA);
            case COMPLETAR -> c.completar(actor, AHORA);
            case CANCELAR -> c.cancelar(actor, "motivo", AHORA);
            case CALIFICAR -> Calificacion.registrar(c, actor, Puntaje.de(5), "ok", AHORA);
        }
    }
}
