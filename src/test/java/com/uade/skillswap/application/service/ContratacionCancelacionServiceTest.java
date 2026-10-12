package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cancelacion con escrow. Escenario base: mentoria de 40 SC cuya franja arranca 100 h despues del reloj inicial,
 * cliente y proveedor con 100 SC de bono. El cliente paga 40 (saldo 60) y se cancela tras {@code avanzar} horas,
 * asi que la anticipacion es 100 - avanzar.
 */
@DisplayName("ContratacionService.cancelar: reintegro segun politica, compensacion y escrow")
class ContratacionCancelacionServiceTest {

    /** Una contratacion pagada lista para cancelar, sobre su propia aplicacion en memoria. */
    private static final class Caso {
        final Escenario e = new Escenario();
        final AplicacionEnMemoria app = e.app;
        final PerfilDto proveedor = e.profesional("u-ana", "Ana");
        final PerfilDto cliente = e.profesional("u-bruno", "Bruno");
        final PublicacionDto publicacion;
        final ContratacionDto pagada;

        Caso(TipoPoliticaCancelacion politica) {
            publicacion = e.publicada(proveedor, e.oferta().politica(politica));
            pagada = e.pagada(cliente, proveedor, publicacion, 0);
        }

        ResultadoCancelacionDto cancelarCliente(long horasTranscurridas) {
            e.avanzarHoras(horasTranscurridas);
            return app.contratacionUseCase.cancelar(cliente.id(), pagada.id(), "Cambio de planes");
        }

        ResultadoCancelacionDto cancelarProveedor(long horasTranscurridas) {
            e.avanzarHoras(horasTranscurridas);
            return app.contratacionUseCase.cancelar(proveedor.id(), pagada.id(), "Me surgio un imprevisto");
        }

        /** Verifica reintegro, compensacion, saldos finales y conservacion del dinero. */
        void verificar(String etiqueta, ResultadoCancelacionDto r, String politica, String reintegro, String compensacion) {
            assertEquals(politica, r.politica(), etiqueta);
            assertEquals("CANCELADA", r.contratacion().estado(), etiqueta);
            Escenario.assertMonto(reintegro, r.reintegroCliente());
            Escenario.assertMonto(compensacion, r.compensacionProveedor());
            Escenario.assertMonto("40", r.reintegroCliente().add(r.compensacionProveedor())); // reintegro + compensacion = precio
            Escenario.assertMonto(new java.math.BigDecimal("60").add(r.reintegroCliente()).toPlainString(), e.saldo(cliente));
            Escenario.assertMonto(new java.math.BigDecimal("100").add(r.compensacionProveedor()).toPlainString(), e.saldo(proveedor));
            Escenario.assertMonto("200", e.saldo(cliente).add(e.saldo(proveedor)));
        }
    }

    // ---------- politica FLEXIBLE: 100 % desde 24 h, 50 % desde 2 h, 0 % por debajo ----------

    @Test
    @DisplayName("FLEXIBLE: con 24 h o mas de anticipacion el reintegro es total (incluido el limite exacto de 24 h)")
    void flexibleReintegroTotal() {
        Caso conMuchaAnticipacion = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        conMuchaAnticipacion.verificar("100 h antes", conMuchaAnticipacion.cancelarCliente(0), "FLEXIBLE", "40", "0");
        Caso enElLimite = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        enElLimite.verificar("24 h antes", enElLimite.cancelarCliente(76), "FLEXIBLE", "40", "0");
    }

    @Test
    @DisplayName("FLEXIBLE: entre 2 h y 24 h el reintegro es del 50 % y el proveedor cobra la otra mitad")
    void flexibleReintegroParcial() {
        Caso casi24 = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        casi24.verificar("23 h antes", casi24.cancelarCliente(77), "FLEXIBLE", "20", "20");
        Caso enElLimite = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        enElLimite.verificar("2 h antes", enElLimite.cancelarCliente(98), "FLEXIBLE", "20", "20");
    }

    @Test
    @DisplayName("FLEXIBLE: con menos de 2 h no hay reintegro y todo el precio compensa al proveedor")
    void flexibleSinReintegro() {
        Caso unaHora = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        unaHora.verificar("1 h antes", unaHora.cancelarCliente(99), "FLEXIBLE", "0", "40");
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.COBRO), tipos(unaHora, unaHora.proveedor));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.PAGO), tipos(unaHora, unaHora.cliente));
    }

    // ---------- politica ESTRICTA: 100 % desde 72 h, 50 % desde 48 h, 0 % por debajo ----------

    @Test
    @DisplayName("ESTRICTA: con 72 h o mas el reintegro es total (limite exacto incluido)")
    void estrictaReintegroTotal() {
        Caso lejos = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        lejos.verificar("100 h antes", lejos.cancelarCliente(0), "ESTRICTA", "40", "0");
        Caso enElLimite = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        enElLimite.verificar("72 h antes", enElLimite.cancelarCliente(28), "ESTRICTA", "40", "0");
    }

    @Test
    @DisplayName("ESTRICTA: entre 48 h y 72 h el reintegro es del 50 %")
    void estrictaReintegroParcial() {
        Caso casi72 = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        casi72.verificar("71 h antes", casi72.cancelarCliente(29), "ESTRICTA", "20", "20");
        Caso enElLimite = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        enElLimite.verificar("48 h antes", enElLimite.cancelarCliente(52), "ESTRICTA", "20", "20");
    }

    @Test
    @DisplayName("ESTRICTA: con menos de 48 h no hay reintegro")
    void estrictaSinReintegro() {
        Caso casi48 = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        casi48.verificar("47 h antes", casi48.cancelarCliente(53), "ESTRICTA", "0", "40");
        Caso muyCerca = new Caso(TipoPoliticaCancelacion.ESTRICTA);
        muyCerca.verificar("1 h antes", muyCerca.cancelarCliente(99), "ESTRICTA", "0", "40");
    }

    @Test
    @DisplayName("misma cancelacion 30 h antes: la politica FLEXIBLE reintegra todo y la ESTRICTA nada")
    void mismaAnticipacionDistintaPolitica() {
        Caso flexible = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        Caso estricta = new Caso(TipoPoliticaCancelacion.ESTRICTA);

        ResultadoCancelacionDto deFlexible = flexible.cancelarCliente(70);
        ResultadoCancelacionDto deEstricta = estricta.cancelarCliente(70);

        flexible.verificar("flexible 30 h", deFlexible, "FLEXIBLE", "40", "0");
        estricta.verificar("estricta 30 h", deEstricta, "ESTRICTA", "0", "40");
    }

    @Test
    @DisplayName("la anticipacion se mide contra la hora de la franja reprogramada, no contra la original")
    void anticipacionUsaLaFranjaActual() {
        Caso caso = new Caso(TipoPoliticaCancelacion.ESTRICTA); // franjas a 100 h y 130 h
        Long franjaNueva = caso.e.franjaId(caso.publicacion, 1);
        caso.app.contratacionUseCase.proponerReprogramacion(caso.cliente.id(), caso.pagada.id(), franjaNueva);
        caso.app.contratacionUseCase.responderReprogramacion(caso.proveedor.id(), caso.pagada.id(), true);

        // 60 h despues: faltan 70 h para la franja nueva (130 h) -> 50 %; contra la original (100 h) faltarian 40 h -> 0 %
        ResultadoCancelacionDto r = caso.cancelarCliente(60);

        caso.verificar("franja reprogramada", r, "ESTRICTA", "20", "20");
    }

    // ---------- cancelacion del proveedor ----------

    @Test
    @DisplayName("si cancela el PROVEEDOR el cliente recupera el 100 % aunque falte 1 h, y el proveedor no cobra nada")
    void cancelaElProveedor() {
        Caso caso = new Caso(TipoPoliticaCancelacion.ESTRICTA);

        ResultadoCancelacionDto r = caso.cancelarProveedor(99); // 1 h antes: para el cliente seria 0 %

        caso.verificar("proveedor cancela", r, "ESTRICTA", "40", "0");
        Escenario.assertMonto("100", caso.e.saldo(caso.cliente));
        Escenario.assertMonto("100", caso.e.saldo(caso.proveedor));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.PAGO, TipoMovimiento.REINTEGRO),
                tipos(caso, caso.cliente));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tipos(caso, caso.proveedor));
    }

    // ---------- movimientos, franja y notificaciones ----------

    @Test
    @DisplayName("el reintegro y la compensacion quedan como movimientos REINTEGRO y COBRO con el monto exacto")
    void movimientosDeLaCancelacion() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);

        caso.cancelarCliente(90); // 10 h antes -> 50 %

        MovimientoDto reintegro = caso.app.walletUseCase.movimientos(caso.cliente.id(), PaginaRequest.de(0, 10)).contenido().get(0);
        assertEquals(TipoMovimiento.REINTEGRO, reintegro.tipo());
        Escenario.assertMonto("20", reintegro.monto());
        Escenario.assertMonto("80", reintegro.saldoResultante());
        assertEquals(caso.pagada.id(), reintegro.contratacionId());
        MovimientoDto compensacion = caso.app.walletUseCase.movimientos(caso.proveedor.id(), PaginaRequest.de(0, 10)).contenido().get(0);
        assertEquals(TipoMovimiento.COBRO, compensacion.tipo());
        Escenario.assertMonto("20", compensacion.monto());
        Escenario.assertMonto("120", compensacion.saldoResultante());
    }

    @Test
    @DisplayName("cancelar libera la franja y el cupo, y otro cliente puede reservarla")
    void cancelarLiberaLaReserva() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        PerfilDto cecilia = caso.e.profesional("u-cecilia", "Cecilia");
        assertEquals(0, caso.e.publicacion(caso.proveedor, caso.publicacion).cupoDisponible());

        caso.cancelarCliente(1);

        PublicacionDto tras = caso.e.publicacion(caso.proveedor, caso.publicacion);
        assertFalse(tras.franjas().get(0).ocupada());
        assertEquals(1, tras.cupoDisponible());
        ContratacionDto nueva = caso.e.aceptada(cecilia, caso.proveedor, caso.publicacion, 0);
        assertEquals("ACEPTADA", nueva.estado());
    }

    @Test
    @DisplayName("cancelar notifica a la contraparte de quien cancela (CANCELACION) y no a quien cancela")
    void cancelarNotificaALaContraparte() {
        Caso delCliente = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        delCliente.cancelarCliente(1);
        assertTrue(eventos(delCliente, delCliente.proveedor).contains(TipoEventoContratacion.CANCELACION));
        assertFalse(eventos(delCliente, delCliente.cliente).contains(TipoEventoContratacion.CANCELACION));

        Caso delProveedor = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        delProveedor.cancelarProveedor(1);
        assertTrue(eventos(delProveedor, delProveedor.cliente).contains(TipoEventoContratacion.CANCELACION));
        assertFalse(eventos(delProveedor, delProveedor.proveedor).contains(TipoEventoContratacion.CANCELACION));
    }

    @Test
    @DisplayName("el motivo de la cancelacion queda en el historial junto con quien cancelo")
    void motivoEnElHistorial() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);

        ResultadoCancelacionDto r = caso.cancelarCliente(1);

        var ultimo = r.contratacion().historial().get(r.contratacion().historial().size() - 1);
        assertEquals("CANCELADA", ultimo.estadoNuevo());
        assertEquals("Cambio de planes", ultimo.motivo());
        assertEquals(caso.cliente.id(), ultimo.actorId());
    }

    // ---------- cancelar sin pago ----------

    @Test
    @DisplayName("cancelar una contratacion ACEPTADA (sin pagar) no mueve dinero, libera la franja y el reintegro es 0")
    void cancelarSinPagar() {
        Escenario e = new Escenario();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        ResultadoCancelacionDto r = e.app.contratacionUseCase.cancelar(ana.id(), aceptada.id(), "Ya no puedo");

        assertEquals("CANCELADA", r.contratacion().estado());
        Escenario.assertMonto("0", r.reintegroCliente());
        Escenario.assertMonto("0", r.compensacionProveedor());
        Escenario.assertMonto("100", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana));
        assertEquals(1L, e.app.walletUseCase.movimientos(bruno.id(), PaginaRequest.de(0, 10)).totalElementos());
        assertEquals(1L, e.app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
        assertFalse(e.publicacion(ana, publicada).franjas().get(0).ocupada());
        assertEquals(1, e.publicacion(ana, publicada).cupoDisponible());
    }

    // ---------- negativos ----------

    @Test
    @DisplayName("un tercero NO puede cancelar: NoAutorizado y no se mueve dinero ni cambia el estado")
    void terceroNoCancela() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        PerfilDto dario = caso.e.profesional("u-dario", "Dario");

        assertThrows(NoAutorizadoException.class, () -> caso.app.contratacionUseCase.cancelar(dario.id(), caso.pagada.id(), "x"));

        assertEquals("CONFIRMADA", caso.e.estado(caso.cliente, caso.pagada).estado());
        Escenario.assertMonto("60", caso.e.saldo(caso.cliente));
        Escenario.assertMonto("100", caso.e.saldo(caso.proveedor));
    }

    @Test
    @DisplayName("cancelar sin motivo es ReglaDeNegocio y la contratacion queda intacta")
    void cancelarSinMotivo() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);

        assertThrows(ReglaDeNegocioException.class, () -> caso.app.contratacionUseCase.cancelar(caso.cliente.id(), caso.pagada.id(), " "));
        assertThrows(ReglaDeNegocioException.class, () -> caso.app.contratacionUseCase.cancelar(caso.cliente.id(), caso.pagada.id(), null));

        assertEquals("CONFIRMADA", caso.e.estado(caso.cliente, caso.pagada).estado());
        Escenario.assertMonto("60", caso.e.saldo(caso.cliente));
        assertTrue(caso.e.publicacion(caso.proveedor, caso.publicacion).franjas().get(0).ocupada());
    }

    @Test
    @DisplayName("no se cancela dos veces: el segundo intento no devuelve dinero otra vez")
    void noSeCancelaDosVeces() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        caso.cancelarCliente(1);

        assertThrows(TransicionInvalidaException.class, () -> caso.app.contratacionUseCase.cancelar(caso.cliente.id(), caso.pagada.id(), "otra vez"));

        Escenario.assertMonto("100", caso.e.saldo(caso.cliente));
        Escenario.assertMonto("100", caso.e.saldo(caso.proveedor));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.PAGO, TipoMovimiento.REINTEGRO), tipos(caso, caso.cliente));
    }

    @Test
    @DisplayName("una solicitud PENDIENTE (se rechaza, no se cancela), una sesion EN_CURSO y una COMPLETADA no se pueden cancelar")
    void estadosQueNoPermitenCancelar() {
        Escenario e = new Escenario();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(3).franjasEn(100L, 110L, 120L));
        ContratacionDto pendiente = e.solicitar(bruno, publicada, 0);
        ContratacionDto enCurso = e.enCurso(bruno, ana, publicada, 1);
        ContratacionDto completada = e.completada(bruno, ana, publicada, 2);

        assertThrows(TransicionInvalidaException.class, () -> e.app.contratacionUseCase.cancelar(bruno.id(), pendiente.id(), "x"));
        assertThrows(TransicionInvalidaException.class, () -> e.app.contratacionUseCase.cancelar(bruno.id(), enCurso.id(), "x"));
        assertThrows(TransicionInvalidaException.class, () -> e.app.contratacionUseCase.cancelar(ana.id(), completada.id(), "x"));

        // dos pagos de 40 a Bruno (-80) y dos cobros de 40 a Ana (+40 completada; la en curso aun no cobra)
        Escenario.assertMonto("20", e.saldo(bruno));
        Escenario.assertMonto("140", e.saldo(ana));
    }

    @Test
    @DisplayName("cancelar una contratacion inexistente es RecursoNoEncontrado")
    void cancelarInexistente() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);

        assertThrows(RecursoNoEncontradoException.class, () -> caso.app.contratacionUseCase.cancelar(caso.cliente.id(), 999L, "x"));
    }

    @Test
    @DisplayName("cancelar con una propuesta de reprogramacion pendiente la descarta")
    void cancelarDescartaLaPropuesta() {
        Caso caso = new Caso(TipoPoliticaCancelacion.FLEXIBLE);
        caso.app.contratacionUseCase.proponerReprogramacion(caso.cliente.id(), caso.pagada.id(), caso.e.franjaId(caso.publicacion, 1));

        ResultadoCancelacionDto r = caso.cancelarProveedor(1);

        assertNull(r.contratacion().propuestaReprogramacion());
    }

    // ---------- auxiliares ----------

    private static List<TipoMovimiento> tipos(Caso caso, PerfilDto perfil) {
        List<MovimientoDto> masNuevoPrimero = caso.app.walletUseCase.movimientos(perfil.id(), PaginaRequest.de(0, 50)).contenido();
        java.util.ArrayList<TipoMovimiento> cronologico = new java.util.ArrayList<>();
        for (int i = masNuevoPrimero.size() - 1; i >= 0; i--) {
            cronologico.add(masNuevoPrimero.get(i).tipo());
        }
        return cronologico;
    }

    private static List<TipoEventoContratacion> eventos(Caso caso, PerfilDto perfil) {
        return caso.e.bandeja(perfil).stream().map(n -> n.tipoEvento()).toList();
    }
}
