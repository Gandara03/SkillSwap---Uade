package com.uade.skillswap.domain.model.calificacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.CLIENTE;
import static com.uade.skillswap.support.Datos.OTRO;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Calificacion")
class CalificacionTest {

    @Test
    @DisplayName("el cliente califica al proveedor de una contratacion completada")
    void clienteCalificaProveedor() {
        Calificacion c = Calificacion.registrar(Datos.completada(), CLIENTE, Puntaje.de(5), "excelente", AHORA);
        assertEquals(CLIENTE, c.getAutorId());
        assertEquals(PROVEEDOR, c.getDestinatarioId());
        assertEquals(Puntaje.de(5), c.getPuntaje());
        assertEquals("excelente", c.getComentario());
        assertEquals(Datos.CONTRATACION_ID, c.getContratacionId());
        assertEquals(AHORA, c.getFecha());
        assertNull(c.getId());
    }

    @Test
    @DisplayName("el proveedor tambien puede calificar al cliente (la contraparte es el destinatario)")
    void proveedorCalificaCliente() {
        Calificacion c = Calificacion.registrar(Datos.completada(), PROVEEDOR, Puntaje.de(4), null, AHORA);
        assertEquals(CLIENTE, c.getDestinatarioId());
    }

    @Test
    @DisplayName("solo se califica una contratacion COMPLETADA")
    void soloCompletada() {
        Contratacion[] noCompletadas = {Datos.pendiente(), Datos.aceptada(), Datos.confirmada(), Datos.enCurso()};
        for (Contratacion c : noCompletadas) {
            assertThrows(ReglaDeNegocioException.class,
                    () -> Calificacion.registrar(c, CLIENTE, Puntaje.de(3), "x", AHORA), c.getEstado());
        }
        Contratacion cancelada = Datos.aceptada();
        cancelada.cancelar(CLIENTE, "x", AHORA);
        assertThrows(ReglaDeNegocioException.class,
                () -> Calificacion.registrar(cancelada, CLIENTE, Puntaje.de(3), "x", AHORA));
        Contratacion rechazada = Datos.pendiente();
        rechazada.rechazar(PROVEEDOR, "no", AHORA);
        assertThrows(ReglaDeNegocioException.class,
                () -> Calificacion.registrar(rechazada, CLIENTE, Puntaje.de(3), "x", AHORA));
    }

    @Test
    @DisplayName("un tercero que no participo no puede calificar")
    void soloParticipantes() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Calificacion.registrar(Datos.completada(), OTRO, Puntaje.de(1), "x", AHORA));
    }

    @Test
    @DisplayName("el destinatario nunca es el propio autor")
    void noAutocalificacion() {
        Contratacion c = Datos.completada();
        for (Long autor : new Long[]{CLIENTE, PROVEEDOR}) {
            Calificacion cal = Calificacion.registrar(c, autor, Puntaje.de(3), "x", AHORA);
            assertNotEquals(cal.getAutorId(), cal.getDestinatarioId());
        }
    }

    @Test
    @DisplayName("reconstruir rehidrata sin validar contra la contratacion")
    void reconstruir() {
        Calificacion c = Calificacion.reconstruir(3L, 10L, 1L, 2L, Puntaje.de(2), "meh", AHORA);
        assertEquals(3L, (long) c.getId());
        assertEquals(2L, (long) c.getDestinatarioId());
    }
}
