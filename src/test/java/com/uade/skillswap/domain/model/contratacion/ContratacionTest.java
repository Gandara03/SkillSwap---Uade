package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.politica.PoliticaEstricta;
import com.uade.skillswap.domain.model.politica.PoliticaFlexible;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.CLIENTE;
import static com.uade.skillswap.support.Datos.OTRO;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contratacion (agregado + State)")
class ContratacionTest {

    // ---------- solicitud ----------

    @Test
    @DisplayName("una solicitud nace PENDIENTE, sin pagar, con historial y evento SOLICITUD_RECIBIDA")
    void solicitud() {
        Contratacion c = Datos.pendiente();
        assertEquals("PENDIENTE", c.getEstado());
        assertFalse(c.estaPagada());
        assertFalse(c.estaCompletada());
        assertNull(c.getPropuestaReprogramacion());
        assertEquals(SkillCoins.de(100), c.getPrecioFinal());
        assertEquals(1, c.getHistorial().size());
        assertNull(c.getHistorial().get(0).getEstadoAnterior());
        assertEquals("PENDIENTE", c.getHistorial().get(0).getEstadoNuevo());
        assertEquals(CLIENTE, c.getHistorial().get(0).getActorId());
        assertEquals(1, c.eventosPendientes().size());
        assertEquals(TipoEventoContratacion.SOLICITUD_RECIBIDA, c.eventosPendientes().get(0).getTipo());
    }

    @Test
    @DisplayName("la solicitud recorta mensaje y objetivo")
    void solicitudRecorta() {
        Contratacion c = Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, "  hola ", " meta ", SkillCoins.CERO, AHORA);
        assertEquals("hola", c.getMensaje());
        assertEquals("meta", c.getObjetivo());
    }

    @Test
    @DisplayName("la solicitud exige publicacion, franja, cliente y proveedor")
    void solicitudObligatorios() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(null, CLIENTE, PROVEEDOR, 1L, "m", "o", SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, null, PROVEEDOR, 1L, "m", "o", SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, null, 1L, "m", "o", SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, null, "m", "o", SkillCoins.CERO, AHORA));
    }

    @Test
    @DisplayName("el autor no puede solicitar su propia publicacion")
    void solicitudAutopropia() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, PROVEEDOR, PROVEEDOR, 1L, "m", "o", SkillCoins.CERO, AHORA));
    }

    @Test
    @DisplayName("mensaje y objetivo son obligatorios")
    void solicitudMensajeObjetivo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, null, "o", SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, " ", "o", SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, "m", null, SkillCoins.CERO, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, "m", "  ", SkillCoins.CERO, AHORA));
    }

    @Test
    @DisplayName("el precio final no puede ser nulo ni negativo")
    void solicitudPrecio() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, "m", "o", null, AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> Contratacion.solicitar(1L, CLIENTE, PROVEEDOR, 1L, "m", "o", SkillCoins.de(-1), AHORA));
    }

    // ---------- flujo feliz ----------

    @Test
    @DisplayName("flujo feliz: PENDIENTE -> ACEPTADA -> CONFIRMADA -> EN_CURSO -> COMPLETADA")
    void flujoFeliz() {
        Contratacion c = Datos.pendiente();
        c.aceptar(PROVEEDOR, AHORA.plusMinutes(1));
        assertEquals("ACEPTADA", c.getEstado());
        c.pagar(CLIENTE, AHORA.plusMinutes(2));
        assertEquals("CONFIRMADA", c.getEstado());
        assertTrue(c.estaPagada());
        assertEquals(AHORA.plusMinutes(2), c.getPagadaEn());
        c.iniciar(PROVEEDOR, AHORA.plusMinutes(3));
        assertEquals("EN_CURSO", c.getEstado());
        c.completar(PROVEEDOR, AHORA.plusMinutes(4));
        assertEquals("COMPLETADA", c.getEstado());
        assertTrue(c.estaCompletada());
        assertEquals(AHORA.plusMinutes(4), c.getCompletadaEn());

        List<HistorialEstado> h = c.getHistorial();
        assertEquals(5, h.size());
        assertEquals("PENDIENTE", h.get(1).getEstadoAnterior());
        assertEquals("ACEPTADA", h.get(1).getEstadoNuevo());
        assertEquals(PROVEEDOR, h.get(1).getActorId());
        assertEquals("EN_CURSO", h.get(4).getEstadoAnterior());
        assertEquals("COMPLETADA", h.get(4).getEstadoNuevo());
    }

    @Test
    @DisplayName("el rechazo con motivo deja RECHAZADA y registra el motivo")
    void rechazar() {
        Contratacion c = Datos.pendiente();
        c.rechazar(PROVEEDOR, "no tengo disponibilidad", AHORA);
        assertEquals("RECHAZADA", c.getEstado());
        assertEquals("no tengo disponibilidad", c.getHistorial().get(1).getMotivo());
    }

    @Test
    @DisplayName("el rechazo exige motivo y no cambia el estado ni registra evento si falla")
    void rechazarSinMotivo() {
        Contratacion c = Datos.pendiente();
        assertThrows(ReglaDeNegocioException.class, () -> c.rechazar(PROVEEDOR, null, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> c.rechazar(PROVEEDOR, "  ", AHORA));
        assertEquals("PENDIENTE", c.getEstado());
        assertEquals(1, c.eventosPendientes().size());
    }

    // ---------- permisos por rol ----------

    @Test
    @DisplayName("solo el proveedor acepta, rechaza, inicia y completa")
    void soloProveedor() {
        Contratacion pendiente = Datos.pendiente();
        assertThrows(ReglaDeNegocioException.class, () -> pendiente.aceptar(CLIENTE, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> pendiente.aceptar(OTRO, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> pendiente.rechazar(CLIENTE, "x", AHORA));
        assertEquals("PENDIENTE", pendiente.getEstado());

        Contratacion confirmada = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> confirmada.iniciar(CLIENTE, AHORA));
        Contratacion enCurso = Datos.enCurso();
        assertThrows(ReglaDeNegocioException.class, () -> enCurso.completar(CLIENTE, AHORA));
        assertEquals("EN_CURSO", enCurso.getEstado());
    }

    @Test
    @DisplayName("solo el cliente paga")
    void soloClientePaga() {
        Contratacion c = Datos.aceptada();
        assertThrows(ReglaDeNegocioException.class, () -> c.pagar(PROVEEDOR, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> c.pagar(OTRO, AHORA));
        assertEquals("ACEPTADA", c.getEstado());
        assertFalse(c.estaPagada());
    }

    @Test
    @DisplayName("un tercero no puede cancelar ni proponer reprogramaciones")
    void terceroNoParticipa() {
        Contratacion c = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> c.cancelar(OTRO, "x", AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> c.proponerReprogramacion(OTRO, 2L, AHORA));
        assertEquals("CONFIRMADA", c.getEstado());
    }

    // ---------- transiciones invalidas ----------

    @Test
    @DisplayName("PENDIENTE no puede pagarse, iniciarse, completarse ni cancelarse")
    void invalidasDesdePendiente() {
        Contratacion c = Datos.pendiente();
        assertThrows(TransicionInvalidaException.class, () -> c.pagar(CLIENTE, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.iniciar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.completar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.cancelar(CLIENTE, "x", AHORA));
        assertEquals("PENDIENTE", c.getEstado());
        assertFalse(c.estaPagada());
    }

    @Test
    @DisplayName("ACEPTADA no puede aceptarse de nuevo, rechazarse, iniciarse ni completarse")
    void invalidasDesdeAceptada() {
        Contratacion c = Datos.aceptada();
        assertThrows(TransicionInvalidaException.class, () -> c.aceptar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.rechazar(PROVEEDOR, "x", AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.iniciar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.completar(PROVEEDOR, AHORA));
    }

    @Test
    @DisplayName("doble pago: pagar una contratacion CONFIRMADA falla y conserva la fecha del primer pago")
    void dobleDePago() {
        Contratacion c = Datos.confirmada();
        assertThrows(TransicionInvalidaException.class, () -> c.pagar(CLIENTE, AHORA.plusHours(1)));
        assertEquals(AHORA.plusMinutes(2), c.getPagadaEn());
        long eventosPago = c.eventosPendientes().stream()
                .filter(e -> e.getTipo() == TipoEventoContratacion.PAGO_CONFIRMADO).count();
        assertEquals(1L, eventosPago);
    }

    @Test
    @DisplayName("CONFIRMADA no puede aceptarse, rechazarse ni completarse sin iniciar")
    void invalidasDesdeConfirmada() {
        Contratacion c = Datos.confirmada();
        assertThrows(TransicionInvalidaException.class, () -> c.aceptar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.rechazar(PROVEEDOR, "x", AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.completar(PROVEEDOR, AHORA));
    }

    @Test
    @DisplayName("EN_CURSO solo admite completar: no se cancela, ni se paga, ni se reprograma")
    void invalidasDesdeEnCurso() {
        Contratacion c = Datos.enCurso();
        assertThrows(TransicionInvalidaException.class, () -> c.cancelar(CLIENTE, "x", AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.cancelar(PROVEEDOR, "x", AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.pagar(CLIENTE, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.iniciar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> c.proponerReprogramacion(CLIENTE, 2L, AHORA));
    }

    @Test
    @DisplayName("los estados terminales no admiten ninguna accion")
    void terminales() {
        Contratacion rechazada = Datos.pendiente();
        rechazada.rechazar(PROVEEDOR, "no", AHORA);
        Contratacion cancelada = Datos.aceptada();
        cancelada.cancelar(CLIENTE, "no puedo", AHORA);
        Contratacion completada = Datos.completada();
        for (Contratacion c : List.of(rechazada, cancelada, completada)) {
            assertThrows(TransicionInvalidaException.class, () -> c.aceptar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.rechazar(PROVEEDOR, "x", AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.pagar(CLIENTE, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.iniciar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.completar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.cancelar(CLIENTE, "x", AHORA));
            assertThrows(TransicionInvalidaException.class, () -> c.proponerReprogramacion(CLIENTE, 2L, AHORA));
        }
    }

    @Test
    @DisplayName("una transicion invalida no registra historial ni eventos")
    void invalidaNoDejaRastro() {
        Contratacion c = Datos.pendiente();
        int historial = c.getHistorial().size();
        int eventos = c.eventosPendientes().size();
        assertThrows(TransicionInvalidaException.class, () -> c.completar(PROVEEDOR, AHORA));
        assertEquals(historial, c.getHistorial().size());
        assertEquals(eventos, c.eventosPendientes().size());
    }

    // ---------- cancelacion ----------

    @Test
    @DisplayName("se puede cancelar en ACEPTADA y en CONFIRMADA, por cliente o proveedor, con motivo")
    void cancelar() {
        Contratacion a = Datos.aceptada();
        a.cancelar(CLIENTE, "me surgio algo", AHORA);
        assertEquals("CANCELADA", a.getEstado());
        assertEquals(CLIENTE, a.getCanceladaPorId());

        Contratacion b = Datos.confirmada();
        b.cancelar(PROVEEDOR, "estoy enfermo", AHORA);
        assertEquals("CANCELADA", b.getEstado());
        assertEquals(PROVEEDOR, b.getCanceladaPorId());
        assertTrue(b.estaPagada(), "el pago sigue registrado: el reintegro lo resuelve el servicio");
    }

    @Test
    @DisplayName("cancelar exige motivo")
    void cancelarSinMotivo() {
        Contratacion c = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> c.cancelar(CLIENTE, null, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> c.cancelar(CLIENTE, " ", AHORA));
        assertEquals("CONFIRMADA", c.getEstado());
        assertNull(c.getCanceladaPorId());
    }

    // ---------- reprogramacion ----------

    @Test
    @DisplayName("proponer reprogramacion deja la propuesta pendiente sin cambiar la franja ni el estado")
    void proponer() {
        Contratacion c = Datos.confirmada();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        assertNotNull(c.getPropuestaReprogramacion());
        assertEquals(2L, (long) c.getPropuestaReprogramacion().getNuevaFranjaId());
        assertEquals(CLIENTE, c.getPropuestaReprogramacion().getPropuestaPorId());
        assertEquals(1L, (long) c.getFranjaId());
        assertEquals("CONFIRMADA", c.getEstado());
        assertEquals(TipoEventoContratacion.REPROGRAMACION_PROPUESTA, ultimoEvento(c));
    }

    @Test
    @DisplayName("la contraparte acepta: la reserva pasa a la nueva franja y no queda propuesta")
    void aceptarReprogramacion() {
        Contratacion c = Datos.aceptada();
        c.proponerReprogramacion(PROVEEDOR, 2L, AHORA);
        c.responderReprogramacion(CLIENTE, true, AHORA);
        assertEquals(2L, (long) c.getFranjaId());
        assertNull(c.getPropuestaReprogramacion());
        assertEquals("ACEPTADA", c.getEstado());
        assertEquals(TipoEventoContratacion.REPROGRAMACION_ACEPTADA, ultimoEvento(c));
    }

    @Test
    @DisplayName("la contraparte rechaza: la franja no cambia y se puede volver a proponer")
    void rechazarReprogramacion() {
        Contratacion c = Datos.confirmada();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        c.responderReprogramacion(PROVEEDOR, false, AHORA);
        assertEquals(1L, (long) c.getFranjaId());
        assertNull(c.getPropuestaReprogramacion());
        assertEquals(TipoEventoContratacion.REPROGRAMACION_RECHAZADA, ultimoEvento(c));
        assertDoesNotThrow(() -> c.proponerReprogramacion(CLIENTE, 3L, AHORA));
    }

    @Test
    @DisplayName("quien propone no puede responder su propia propuesta")
    void propuestaPropia() {
        Contratacion c = Datos.confirmada();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> c.responderReprogramacion(CLIENTE, true, AHORA));
        assertEquals(1L, (long) c.getFranjaId());
        assertNotNull(c.getPropuestaReprogramacion());
    }

    @Test
    @DisplayName("no se puede responder si no hay propuesta pendiente")
    void respuestaSinPropuesta() {
        Contratacion c = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> c.responderReprogramacion(PROVEEDOR, true, AHORA));
    }

    @Test
    @DisplayName("no se acumulan propuestas: una nueva con otra pendiente falla")
    void propuestaDuplicada() {
        Contratacion c = Datos.confirmada();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> c.proponerReprogramacion(PROVEEDOR, 3L, AHORA));
        assertEquals(2L, (long) c.getPropuestaReprogramacion().getNuevaFranjaId());
    }

    @Test
    @DisplayName("la nueva franja debe ser distinta a la actual y no nula")
    void propuestaMismaFranja() {
        Contratacion c = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> c.proponerReprogramacion(CLIENTE, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> c.proponerReprogramacion(CLIENTE, null, AHORA));
        assertNull(c.getPropuestaReprogramacion());
    }

    @Test
    @DisplayName("no se reprograma una solicitud PENDIENTE")
    void reprogramarPendiente() {
        Contratacion c = Datos.pendiente();
        assertThrows(TransicionInvalidaException.class, () -> c.proponerReprogramacion(CLIENTE, 2L, AHORA));
    }

    @Test
    @DisplayName("cancelar descarta la propuesta de reprogramacion pendiente")
    void cancelarDescartaPropuesta() {
        Contratacion c = Datos.confirmada();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        c.cancelar(CLIENTE, "mejor cancelo", AHORA);
        assertNull(c.getPropuestaReprogramacion());
    }

    @Test
    @DisplayName("la reprogramacion deja constancia en el historial sin cambiar de estado")
    void reprogramacionEnHistorial() {
        Contratacion c = Datos.confirmada();
        int antes = c.getHistorial().size();
        c.proponerReprogramacion(CLIENTE, 2L, AHORA);
        c.responderReprogramacion(PROVEEDOR, true, AHORA);
        assertEquals(antes + 2, c.getHistorial().size());
        HistorialEstado nota = c.getHistorial().get(antes);
        assertEquals("CONFIRMADA", nota.getEstadoAnterior());
        assertEquals("CONFIRMADA", nota.getEstadoNuevo());
    }

    private static TipoEventoContratacion ultimoEvento(Contratacion c) {
        return c.eventosPendientes().get(c.eventosPendientes().size() - 1).getTipo();
    }

    // ---------- participantes ----------

    @Test
    @DisplayName("contraparteDe devuelve al otro participante y falla para un tercero")
    void contraparte() {
        Contratacion c = Datos.pendiente();
        assertEquals(PROVEEDOR, c.contraparteDe(CLIENTE));
        assertEquals(CLIENTE, c.contraparteDe(PROVEEDOR));
        assertThrows(ReglaDeNegocioException.class, () -> c.contraparteDe(OTRO));
        assertTrue(c.esParticipante(CLIENTE));
        assertTrue(c.esParticipante(PROVEEDOR));
        assertFalse(c.esParticipante(OTRO));
    }

    // ---------- reparto de lo retenido al cancelar ----------

    @Test
    @DisplayName("si cancela el cliente, la politica decide el reintegro y el resto compensa al proveedor")
    void repartoCancelaCliente() {
        Contratacion conPoco = Datos.confirmada();
        conPoco.cancelar(CLIENTE, "No puedo", AHORA);
        RepartoCancelacion parcial = conPoco.repartirRetenido(new PoliticaFlexible(), Datos.horas(10));
        assertEquals(SkillCoins.de(50), parcial.getReintegro());
        assertEquals(SkillCoins.de(50), parcial.getCompensacion());

        Contratacion conTiempo = Datos.confirmada();
        conTiempo.cancelar(CLIENTE, "No puedo", AHORA);
        RepartoCancelacion total = conTiempo.repartirRetenido(new PoliticaFlexible(), Datos.horas(30));
        assertEquals(SkillCoins.de(100), total.getReintegro());
        assertEquals(SkillCoins.CERO, total.getCompensacion());
    }

    @Test
    @DisplayName("si cancela el proveedor, el cliente recupera el 100 % aunque la politica no devolveria nada")
    void repartoCancelaProveedor() {
        Contratacion c = Datos.confirmada();
        c.cancelar(PROVEEDOR, "Me surgio un imprevisto", AHORA);
        RepartoCancelacion reparto = c.repartirRetenido(new PoliticaEstricta(), Datos.horas(1));
        assertEquals(SkillCoins.de(100), reparto.getReintegro());
        assertEquals(SkillCoins.CERO, reparto.getCompensacion());
    }

    @Test
    @DisplayName("si no se habia pagado, no hay nada que repartir")
    void repartoSinPago() {
        Contratacion c = Datos.aceptada();
        c.cancelar(CLIENTE, "Cambie de idea", AHORA);
        RepartoCancelacion reparto = c.repartirRetenido(new PoliticaFlexible(), Datos.horas(30));
        assertEquals(SkillCoins.CERO, reparto.getReintegro());
        assertEquals(SkillCoins.CERO, reparto.getCompensacion());
    }

    @Test
    @DisplayName("solo se reparte lo retenido de una contratacion cancelada")
    void repartoSinCancelar() {
        Contratacion c = Datos.confirmada();
        assertThrows(ReglaDeNegocioException.class, () -> c.repartirRetenido(new PoliticaFlexible(), Datos.horas(30)));
    }

    // ---------- comprobante ----------

    @Test
    @DisplayName("el comprobante solo existe para contrataciones COMPLETADAS y contiene los datos del pago")
    void comprobante() {
        Comprobante comp = Datos.completada().consultarComprobante();
        assertEquals("SS-00000500", comp.getNumero());
        assertEquals(Datos.CONTRATACION_ID, comp.getContratacionId());
        assertEquals(Datos.PUBLICACION_ID, comp.getPublicacionId());
        assertEquals(CLIENTE, comp.getClienteId());
        assertEquals(PROVEEDOR, comp.getProveedorId());
        assertEquals(SkillCoins.de(100), comp.getMonto());
        assertEquals(AHORA.plusMinutes(2), comp.getPagadoEn());
        assertEquals(AHORA.plusMinutes(4), comp.getCompletadoEn());
    }

    @Test
    @DisplayName("el comprobante de una contratacion no completada falla")
    void comprobanteNoCompletada() {
        assertThrows(ReglaDeNegocioException.class, () -> Datos.pendiente().consultarComprobante());
        assertThrows(ReglaDeNegocioException.class, () -> Datos.confirmada().consultarComprobante());
        assertThrows(ReglaDeNegocioException.class, () -> Datos.enCurso().consultarComprobante());
    }

    // ---------- historial ----------

    @Test
    @DisplayName("el historial es inmutable desde afuera")
    void historialInmutable() {
        Contratacion c = Datos.pendiente();
        assertThrows(UnsupportedOperationException.class, () -> c.getHistorial().clear());
        assertThrows(UnsupportedOperationException.class, () -> c.eventosPendientes().clear());
    }

    @Test
    @DisplayName("reconstruir rehidrata el estado y sigue operando segun ese estado")
    void reconstruir() {
        Contratacion c = Contratacion.reconstruir(7L, 1L, CLIENTE, PROVEEDOR, 1L, "m", "o", SkillCoins.de(10),
                "CONFIRMADA", null, List.of(), AHORA, AHORA, null, null, 2L);
        assertEquals("CONFIRMADA", c.getEstado());
        assertTrue(c.estaPagada());
        assertEquals(2L, (long) c.getVersion());
        c.iniciar(PROVEEDOR, AHORA);
        assertEquals("EN_CURSO", c.getEstado());
        assertThrows(IllegalArgumentException.class, () -> Contratacion.reconstruir(7L, 1L, CLIENTE, PROVEEDOR, 1L,
                "m", "o", SkillCoins.de(10), "RARA", null, List.of(), AHORA, null, null, null, 0L));
    }
}
