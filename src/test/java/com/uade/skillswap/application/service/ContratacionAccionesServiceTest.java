package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.ACEPTAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.CALIFICAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.CANCELAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.COMPLETAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.INICIAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.PAGAR;
import static com.uade.skillswap.domain.model.contratacion.AccionContratacion.RECHAZAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ContratacionService: el DTO trae las acciones de quien consulta y el inicio de la franja")
class ContratacionAccionesServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;   // proveedora
    private PerfilDto bruno; // cliente
    private PublicacionDto publicada; // franjas a 100 h y 130 h

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        publicada = e.publicada(ana);
    }

    @Test
    @DisplayName("la solicitud: el cliente no tiene acciones y el proveedor puede aceptar o rechazar")
    void solicitud() {
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertEquals(List.of(), solicitud.acciones(), "lo devuelto es para el cliente, que no puede retirarla");
        assertEquals(List.of(ACEPTAR, RECHAZAR), e.estado(ana, solicitud).acciones());
        assertEquals(publicada.franjas().get(0).inicio(), solicitud.franjaInicio());
    }

    @Test
    @DisplayName("cada operacion devuelve las acciones de quien la hizo, y la otra parte ve las suyas")
    void recorridoCompleto() {
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        ContratacionDto aceptada = app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        assertEquals(List.of(CANCELAR), aceptada.acciones());
        assertEquals(List.of(PAGAR, CANCELAR), e.estado(bruno, aceptada).acciones());

        ContratacionDto pagada = app.contratacionUseCase.pagar(bruno.id(), solicitud.id());
        assertEquals(List.of(CANCELAR), pagada.acciones());
        assertEquals(List.of(INICIAR, CANCELAR), e.estado(ana, pagada).acciones());

        ContratacionDto enCurso = app.contratacionUseCase.iniciar(ana.id(), solicitud.id());
        assertEquals(List.of(COMPLETAR), enCurso.acciones());
        assertEquals(List.of(), e.estado(bruno, enCurso).acciones());

        ContratacionDto completada = app.contratacionUseCase.completar(ana.id(), solicitud.id());
        assertEquals(List.of(CALIFICAR), completada.acciones());
        assertEquals(List.of(CALIFICAR), e.estado(bruno, completada).acciones());
    }

    @Test
    @DisplayName("rechazar y cancelar dejan la contratacion sin acciones")
    void terminales() {
        ContratacionDto rechazada = app.contratacionUseCase.rechazar(ana.id(), e.solicitar(bruno, publicada, 0).id(),
                "Sin disponibilidad");
        assertEquals(List.of(), rechazada.acciones());

        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 1);
        ContratacionDto cancelada = app.contratacionUseCase.cancelar(bruno.id(), aceptada.id(), "Ya no puedo")
                .contratacion();
        assertEquals(List.of(), cancelada.acciones());
        assertEquals(List.of(), e.estado(ana, cancelada).acciones());
    }

    @Test
    @DisplayName("el listado de contrataciones trae las acciones de quien lista")
    void listado() {
        e.aceptada(bruno, ana, publicada, 0);
        PaginaRequest pagina = PaginaRequest.de(0, 10);

        assertEquals(List.of(PAGAR, CANCELAR),
                app.contratacionUseCase.listarMias(bruno.id(), pagina).contenido().get(0).acciones());
        assertEquals(List.of(CANCELAR),
                app.contratacionUseCase.listarMias(ana.id(), pagina).contenido().get(0).acciones());
    }

    @Test
    @DisplayName("al aceptar una reprogramacion, el inicio informado pasa a ser el de la nueva franja")
    void franjaInicioTrasReprogramar() {
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), e.franjaId(publicada, 1));

        ContratacionDto movida = app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true);

        assertEquals(publicada.franjas().get(1).inicio(), movida.franjaInicio());
        assertEquals(publicada.franjas().get(1).inicio(), e.estado(bruno, movida).franjaInicio());
    }
}
