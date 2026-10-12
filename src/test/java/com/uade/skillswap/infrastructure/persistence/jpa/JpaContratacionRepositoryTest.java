package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.contratacion.PropuestaReprogramacion;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaContratacionRepositoryTest extends JpaTestBase {

    private Perfil proveedor;
    private Perfil cliente;
    private Publicacion publicacion;

    @BeforeEach
    void crearEscenario() {
        proveedor = profesional("u-ana");
        cliente = profesional("u-bruno");
        publicacion = publicacion(proveedor).precio(100)
                .franjasEn(AHORA.plusDays(1), AHORA.plusDays(2)).guardar();
    }

    private Contratacion recargar(Contratacion c) {
        return contrataciones.buscarPorId(c.getId()).orElseThrow();
    }

    @Test
    @DisplayName("una solicitud se guarda y se recupera con sus datos, estado PENDIENTE e historial")
    void guardarSolicitudYRecuperar() {
        Contratacion guardada = solicitud(publicacion, cliente);

        assertNotNull(guardada.getId());
        Contratacion leida = recargar(guardada);
        assertEquals(publicacion.getId(), leida.getPublicacionId());
        assertEquals(cliente.getId(), leida.getClienteId());
        assertEquals(proveedor.getId(), leida.getProveedorId());
        assertEquals(publicacion.getFranjas().get(0).getId(), leida.getFranjaId());
        assertEquals("Quiero aprender", leida.getMensaje());
        assertEquals("Mejorar mi nivel", leida.getObjetivo());
        assertEquals(SkillCoins.de(100), leida.getPrecioFinal());
        assertEquals("PENDIENTE", leida.getEstado());
        assertEquals(AHORA, leida.getCreadaEn());
        assertNull(leida.getPagadaEn());
        assertNull(leida.getCompletadaEn());
        assertNull(leida.getCanceladaPorId());
        assertNull(leida.getPropuestaReprogramacion());
        assertEquals(Long.valueOf(0), leida.getVersion());
        assertEquals(1, leida.getHistorial().size());
        assertNull(leida.getHistorial().get(0).getEstadoAnterior());
        assertEquals("PENDIENTE", leida.getHistorial().get(0).getEstadoNuevo());
        assertEquals(cliente.getId(), leida.getHistorial().get(0).getActorId());
    }

    @Test
    @DisplayName("el flujo completo persiste estado, fechas e historial en cada paso y el estado rehidratado sigue operando")
    void flujoCompleto() {
        Contratacion c = solicitud(publicacion, cliente);

        Contratacion paso = recargar(c);
        paso.aceptar(proveedor.getId(), AHORA.plusMinutes(1));
        contrataciones.guardar(paso);
        assertEquals("ACEPTADA", recargar(c).getEstado());

        paso = recargar(c);
        paso.pagar(cliente.getId(), AHORA.plusMinutes(2));
        contrataciones.guardar(paso);
        Contratacion pagada = recargar(c);
        assertEquals("CONFIRMADA", pagada.getEstado());
        assertEquals(AHORA.plusMinutes(2), pagada.getPagadaEn());
        assertTrue(pagada.estaPagada());

        paso = recargar(c);
        paso.iniciar(proveedor.getId(), AHORA.plusMinutes(3));
        contrataciones.guardar(paso);
        assertEquals("EN_CURSO", recargar(c).getEstado());

        paso = recargar(c);
        paso.completar(proveedor.getId(), AHORA.plusMinutes(4));
        contrataciones.guardar(paso);

        Contratacion fin = recargar(c);
        assertEquals("COMPLETADA", fin.getEstado());
        assertTrue(fin.estaCompletada());
        assertEquals(AHORA.plusMinutes(4), fin.getCompletadaEn());
        assertEquals(Long.valueOf(4), fin.getVersion());
        assertIterableEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA"),
                fin.getHistorial().stream().map(HistorialEstado::getEstadoNuevo).toList());
        assertEquals("SS-" + String.format("%08d", c.getId()), fin.consultarComprobante().getNumero());
    }

    @Test
    @DisplayName("rechazo y cancelacion guardan el motivo en el historial y quien cancelo")
    void rechazoYCancelacion() {
        Contratacion rechazada = solicitud(publicacion, cliente);
        rechazada.rechazar(proveedor.getId(), "No tengo cupo", AHORA.plusMinutes(1));
        contrataciones.guardar(rechazada);

        Contratacion leidaRechazada = recargar(rechazada);
        assertEquals("RECHAZADA", leidaRechazada.getEstado());
        HistorialEstado ultimo = leidaRechazada.getHistorial().get(leidaRechazada.getHistorial().size() - 1);
        assertEquals("No tengo cupo", ultimo.getMotivo());
        assertEquals(proveedor.getId(), ultimo.getActorId());

        Contratacion cancelada = solicitud(publicacion, cliente);
        cancelada.aceptar(proveedor.getId(), AHORA.plusMinutes(1));
        cancelada.cancelar(cliente.getId(), "Imprevisto", AHORA.plusMinutes(2));
        contrataciones.guardar(cancelada);

        Contratacion leidaCancelada = recargar(cancelada);
        assertEquals("CANCELADA", leidaCancelada.getEstado());
        assertEquals(cliente.getId(), leidaCancelada.getCanceladaPorId());
    }

    @Test
    @DisplayName("la propuesta de reprogramacion se persiste, y al responderla se mueve la franja y se limpia la propuesta")
    void reprogramacion() {
        Long franjaActual = publicacion.getFranjas().get(0).getId();
        Long franjaNueva = publicacion.getFranjas().get(1).getId();
        Contratacion c = solicitud(publicacion, cliente);
        c.aceptar(proveedor.getId(), AHORA.plusMinutes(1));
        c.proponerReprogramacion(cliente.getId(), franjaNueva, AHORA.plusMinutes(2));
        contrataciones.guardar(c);

        Contratacion conPropuesta = recargar(c);
        PropuestaReprogramacion propuesta = conPropuesta.getPropuestaReprogramacion();
        assertNotNull(propuesta);
        assertEquals(franjaNueva, propuesta.getNuevaFranjaId());
        assertEquals(cliente.getId(), propuesta.getPropuestaPorId());
        assertEquals(AHORA.plusMinutes(2), propuesta.getFecha());
        assertEquals(franjaActual, conPropuesta.getFranjaId());

        conPropuesta.responderReprogramacion(proveedor.getId(), true, AHORA.plusMinutes(3));
        contrataciones.guardar(conPropuesta);

        Contratacion movida = recargar(c);
        assertNull(movida.getPropuestaReprogramacion());
        assertEquals(franjaNueva, movida.getFranjaId());
        // creacion, aceptacion, nota de propuesta y nota de aceptacion de la reprogramacion
        assertEquals(4, movida.getHistorial().size());
    }

    @Test
    @DisplayName("guardar devuelve la misma instancia con id asignado y conserva los eventos pendientes para notificar despues del commit")
    void conservaEventosPendientes() {
        Contratacion nueva = Contratacion.solicitar(publicacion.getId(), cliente.getId(), proveedor.getId(),
                publicacion.getFranjas().get(0).getId(), "Hola", "Aprender", SkillCoins.de(100), AHORA);
        assertEquals(1, nueva.eventosPendientes().size());
        assertNull(nueva.getId());

        Contratacion guardada = contrataciones.guardar(nueva);

        assertSame(nueva, guardada);
        assertNotNull(nueva.getId());
        assertEquals(1, guardada.eventosPendientes().size());

        List<ContratacionEvent> recibidos = new ArrayList<>();
        guardada.agregarObserver(recibidos::add);
        guardada.notificarEventosPendientes();
        assertEquals(1, recibidos.size());
        assertEquals(TipoEventoContratacion.SOLICITUD_RECIBIDA, recibidos.get(0).getTipo());
        assertEquals(nueva.getId(), recibidos.get(0).getContratacionId());
    }

    @Test
    @DisplayName("guardar dos veces la misma instancia no duplica el historial")
    void guardarDosVecesNoDuplica() {
        Contratacion c = solicitud(publicacion, cliente);
        c.aceptar(proveedor.getId(), AHORA.plusMinutes(1));

        contrataciones.guardar(c);
        contrataciones.guardar(c);

        Contratacion leida = recargar(c);
        assertEquals(2, leida.getHistorial().size());
        assertEquals(Long.valueOf(1), leida.getVersion());
    }

    @Test
    @DisplayName("dos lectores de la misma contratacion: el segundo guardado falla con ConflictoConcurrenciaException")
    void conflictoDeVersion() {
        Contratacion c = solicitud(publicacion, cliente);
        Contratacion lectorA = recargar(c);
        Contratacion lectorB = recargar(c);
        lectorA.aceptar(proveedor.getId(), AHORA.plusMinutes(1));
        lectorB.rechazar(proveedor.getId(), "Sin cupo", AHORA.plusMinutes(1));

        contrataciones.guardar(lectorA);

        assertThrows(ConflictoConcurrenciaException.class, () -> contrataciones.guardar(lectorB));
        assertEquals("ACEPTADA", recargar(c).getEstado());
    }

    @Test
    @DisplayName("buscarPorParticipante: como cliente o como proveedor, las mas recientes primero y paginado")
    void buscarPorParticipante() {
        Perfil cecilia = profesional("u-cecilia");
        Contratacion c1 = contrataciones.guardar(nuevaSolicitud(cliente, 1));
        Contratacion c2 = contrataciones.guardar(nuevaSolicitud(cliente, 2));
        Contratacion c3 = contrataciones.guardar(nuevaSolicitud(cliente, 3));
        contrataciones.guardar(nuevaSolicitud(cecilia, 4));

        Pagina<Contratacion> comoCliente = contrataciones.buscarPorParticipante(cliente.getId(), PaginaRequest.de(0, 2));
        Pagina<Contratacion> siguiente = contrataciones.buscarPorParticipante(cliente.getId(), PaginaRequest.de(1, 2));
        Pagina<Contratacion> comoProveedor = contrataciones.buscarPorParticipante(proveedor.getId(), PaginaRequest.de(0, 10));

        assertEquals(3, comoCliente.totalElementos());
        assertIterableEquals(List.of(c3.getId(), c2.getId()), comoCliente.contenido().stream().map(Contratacion::getId).toList());
        assertIterableEquals(List.of(c1.getId()), siguiente.contenido().stream().map(Contratacion::getId).toList());
        assertEquals(4, comoProveedor.totalElementos());
        assertEquals(1, comoCliente.contenido().get(0).getHistorial().size());
        assertEquals(0, contrataciones.buscarPorParticipante(9999L, PaginaRequest.de(0, 5)).totalElementos());
    }

    @Test
    @DisplayName("contarPorPublicacion cuenta todas las contrataciones de esa publicacion")
    void contarPorPublicacion() {
        Publicacion otra = publicacion(proveedor).titulo("Otra").guardar();
        solicitud(publicacion, cliente);
        solicitud(publicacion, cliente);
        solicitud(otra, cliente);

        assertEquals(2, contrataciones.contarPorPublicacion(publicacion.getId()));
        assertEquals(1, contrataciones.contarPorPublicacion(otra.getId()));
        assertEquals(0, contrataciones.contarPorPublicacion(9999L));
    }

    @Test
    @DisplayName("buscarPorId devuelve vacio si no existe o si el id es null")
    void buscarPorIdInexistente() {
        assertTrue(contrataciones.buscarPorId(9999L).isEmpty());
        assertTrue(contrataciones.buscarPorId(null).isEmpty());
        assertFalse(contrataciones.buscarPorId(solicitud(publicacion, cliente).getId()).isEmpty());
    }

    /** Solicitud de {@code solicitante} a la publicacion del escenario, creada {@code minutos} despues de AHORA. */
    private Contratacion nuevaSolicitud(Perfil solicitante, int minutos) {
        return Contratacion.solicitar(publicacion.getId(), solicitante.getId(), proveedor.getId(),
                publicacion.getFranjas().get(0).getId(), "Mensaje " + minutos, "Objetivo", SkillCoins.de(100),
                AHORA.plusMinutes(minutos));
    }
}
