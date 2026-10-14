package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.politica.ConfiguracionCancelacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WalletService: saldo y movimientos de la billetera propia")
class WalletServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;
    private PerfilDto bruno;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
    }

    @Test
    @DisplayName("un perfil nuevo tiene 100 SC de bono inicial, con un unico movimiento BONO_INICIAL")
    void bonoInicial() {
        WalletDto wallet = app.walletUseCase.consultar(ana.id());

        assertEquals(ana.id(), wallet.perfilId());
        Escenario.assertMonto("100", wallet.saldo());
        Pagina<MovimientoDto> movimientos = app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10));
        assertEquals(1L, movimientos.totalElementos());
        MovimientoDto bono = movimientos.contenido().get(0);
        assertEquals(TipoMovimiento.BONO_INICIAL, bono.tipo());
        Escenario.assertMonto("100", bono.monto());
        Escenario.assertMonto("100", bono.saldoResultante());
        assertEquals(null, bono.contratacionId());
        assertNotNull(bono.fecha());
    }

    @Test
    @DisplayName("sin bono configurado (0 SC) la wallet arranca vacia y sin movimientos")
    void sinBono() {
        SkillSwapConfig config = new SkillSwapConfig(SkillCoins.CERO, 200, 10, 5, 5, ConfiguracionCancelacion.porDefecto());
        Escenario otro = new Escenario(AplicacionEnMemoria.crear(config));
        PerfilDto carla = otro.profesional("u-carla", "Carla");

        Escenario.assertMonto("0", otro.saldo(carla));
        assertEquals(0L, otro.app.walletUseCase.movimientos(carla.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("tras un servicio completo los movimientos quedan del mas reciente al mas antiguo, con saldo resultante exacto")
    void movimientosTrasUnServicio() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);

        List<MovimientoDto> deBruno = app.walletUseCase.movimientos(bruno.id(), PaginaRequest.de(0, 10)).contenido();
        List<MovimientoDto> deAna = app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10)).contenido();

        assertEquals(List.of(TipoMovimiento.PAGO, TipoMovimiento.BONO_INICIAL), deBruno.stream().map(MovimientoDto::tipo).toList());
        Escenario.assertMonto("40", deBruno.get(0).monto());
        Escenario.assertMonto("60", deBruno.get(0).saldoResultante());
        assertEquals(completada.id(), deBruno.get(0).contratacionId());
        assertEquals(List.of(TipoMovimiento.COBRO, TipoMovimiento.BONO_INICIAL), deAna.stream().map(MovimientoDto::tipo).toList());
        Escenario.assertMonto("40", deAna.get(0).monto());
        Escenario.assertMonto("140", deAna.get(0).saldoResultante());
        Escenario.assertMonto("60", e.saldo(bruno));
        Escenario.assertMonto("140", e.saldo(ana));
    }

    @Test
    @DisplayName("los movimientos se paginan")
    void movimientosPaginados() {
        PublicacionDto publicada = e.publicada(ana);
        e.completada(bruno, ana, publicada, 0);

        Pagina<MovimientoDto> primera = app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 1));
        Pagina<MovimientoDto> segunda = app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(1, 1));

        assertEquals(2L, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertTrue(primera.tieneSiguiente());
        assertEquals(TipoMovimiento.COBRO, primera.contenido().get(0).tipo());
        assertEquals(TipoMovimiento.BONO_INICIAL, segunda.contenido().get(0).tipo());
    }

    @Test
    @DisplayName("cada perfil consulta solo su billetera: lo de uno no afecta lo de otro")
    void billeterasIndependientes() {
        PublicacionDto publicada = e.publicada(ana);
        e.pagada(bruno, ana, publicada, 0);

        Escenario.assertMonto("60", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana)); // el pago queda retenido hasta completar
        assertEquals(1L, app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("wallet de un perfil inexistente es RecursoNoEncontrado; actor o pagina nulos son ValidacionException")
    void casosInvalidos() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.walletUseCase.consultar(999L));
        assertThrows(RecursoNoEncontradoException.class, () -> app.walletUseCase.movimientos(999L, PaginaRequest.de(0, 10)));
        assertThrows(ValidacionException.class, () -> app.walletUseCase.consultar(null));
        assertThrows(ValidacionException.class, () -> app.walletUseCase.movimientos(null, PaginaRequest.de(0, 10)));
        assertThrows(ValidacionException.class, () -> app.walletUseCase.movimientos(ana.id(), null));
    }
}
