package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 6: cancelar y calcular el reintegro con dos politicas diferentes.
 * Misma anticipacion (60 h) y mismo precio: FLEXIBLE reintegra el 100 % (24 h o mas) y ESTRICTA el 50 %
 * (48 h o mas pero menos de 72 h). Lo no reintegrado compensa al proveedor (escrow).
 */
@DisplayName("Caso 6 (memoria): cancelar y calcular el reintegro con dos politicas diferentes")
class Caso06CancelacionConDosPoliticasTest {

    private static final Duration ANTICIPACION = Duration.ofHours(60);

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    private PublicacionDto publicarCon(EscenarioMemoria e, Long autorId, TipoPoliticaCancelacion politica) {
        return e.publicar(autorId, e.mentoria("Mentoria " + politica, "programacion", 80, 1, politica,
                Set.of("java"), Set.of("java"), List.of(e.franja(ANTICIPACION))));
    }

    @Test
    @DisplayName("Caso 6: con 60 h de anticipacion, FLEXIBLE reintegra 80 SC y ESTRICTA solo 40 SC")
    void caso06_cancelarConPoliticaFlexibleYEstrictaDaReintegrosDistintos() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PublicacionDto flexible = publicarCon(e, ana.id(), TipoPoliticaCancelacion.FLEXIBLE);
        PublicacionDto estricta = publicarCon(e, ana.id(), TipoPoliticaCancelacion.ESTRICTA);
        ContratacionDto deBruno = e.contratarYPagar(ana.id(), bruno.id(), flexible, 0);
        ContratacionDto deCecilia = e.contratarYPagar(ana.id(), cecilia.id(), estricta, 0);
        assertEquals(0, sc(20).compareTo(e.saldo(bruno.id())));
        assertEquals(0, sc(20).compareTo(e.saldo(cecilia.id())));

        ResultadoCancelacionDto conFlexible = e.app.contratacionUseCase.cancelar(bruno.id(), deBruno.id(), "No llego");
        ResultadoCancelacionDto conEstricta = e.app.contratacionUseCase.cancelar(cecilia.id(), deCecilia.id(), "No llego");

        // misma anticipacion, distinta politica -> distinto reintegro
        assertEquals("FLEXIBLE", conFlexible.politica());
        assertEquals("ESTRICTA", conEstricta.politica());
        assertEquals(0, sc(80).compareTo(conFlexible.reintegroCliente()));
        assertEquals(0, sc(0).compareTo(conFlexible.compensacionProveedor()));
        assertEquals(0, sc(40).compareTo(conEstricta.reintegroCliente()));
        assertEquals(0, sc(40).compareTo(conEstricta.compensacionProveedor()));
        assertNotEquals(0, conFlexible.reintegroCliente().compareTo(conEstricta.reintegroCliente()));
        assertEquals("CANCELADA", conFlexible.contratacion().estado());
        assertEquals("CANCELADA", conEstricta.contratacion().estado());

        // saldos finales: Bruno recupera todo; Cecilia pierde 40; Ana cobra solo la compensacion de la estricta
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())));
        assertEquals(0, sc(60).compareTo(e.saldo(cecilia.id())));
        assertEquals(0, sc(140).compareTo(e.saldo(ana.id())));
        assertEquals(TipoMovimiento.REINTEGRO,
                e.app.walletUseCase.movimientos(cecilia.id(), e.primeraPagina()).contenido().get(0).tipo());
    }

    @Test
    @DisplayName("Caso 6: al cancelar se libera la franja y el cupo de la publicacion")
    void caso06_cancelarLiberaFranjaYCupo() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto estricta = publicarCon(e, ana.id(), TipoPoliticaCancelacion.ESTRICTA);
        ContratacionDto contratacion = e.contratarYPagar(ana.id(), bruno.id(), estricta, 0);
        assertEquals(0, e.app.publicacionUseCase.obtener(ana.id(), estricta.id()).cupoDisponible());

        e.app.contratacionUseCase.cancelar(bruno.id(), contratacion.id(), "Imprevisto");

        PublicacionDto tras = e.app.publicacionUseCase.obtener(ana.id(), estricta.id());
        assertEquals(1, tras.cupoDisponible());
        assertFalse(tras.franjas().get(0).ocupada());
    }

    @Test
    @DisplayName("Caso 6: con menos anticipacion la politica FLEXIBLE tambien reduce el reintegro")
    void caso06_laAnticipacionTambienCuenta() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto flexible = e.publicar(ana.id(), e.mentoria("Mentoria flexible", "programacion", 80, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), List.of(e.franja(Duration.ofHours(30)))));
        ContratacionDto contratacion = e.contratarYPagar(ana.id(), bruno.id(), flexible, 0);

        // pasan 20 h: faltan 10 h (entre 2 h y 24 h) -> 50 %
        e.app.reloj.avanzar(Duration.ofHours(20));
        ResultadoCancelacionDto resultado = e.app.contratacionUseCase.cancelar(bruno.id(), contratacion.id(), "Tarde");

        assertEquals(0, sc(40).compareTo(resultado.reintegroCliente()));
        assertTrue(resultado.compensacionProveedor().compareTo(sc(40)) == 0);
    }
}
