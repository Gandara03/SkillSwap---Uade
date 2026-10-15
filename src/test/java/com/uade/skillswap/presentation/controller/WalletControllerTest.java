package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Integracion WalletController -> WalletUseCase -> JPA/H2. Cada usuario ve solo su propia wallet. */
@DisplayName("WalletController (JPA/H2): saldo, movimientos paginados y permisos")
class WalletControllerTest extends ControllerJpaTestBase {

    private IWalletController wallet() {
        return app.getWalletController();
    }

    private static List<TipoMovimiento> tipos(Pagina<MovimientoDto> movimientos) {
        return movimientos.contenido().stream().map(MovimientoDto::tipo).toList();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("un perfil nuevo tiene el bono inicial como saldo y como unico movimiento")
    void bonoInicial() {
        profesional("u-ana", "Ana");

        assertEquals(0, sc(100).compareTo(ok(wallet().consultar()).saldo()));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tipos(ok(wallet().movimientos(primeraPagina()))));
    }

    @Test
    @DisplayName("despues de una contratacion completada, cada uno ve solo sus movimientos, paginados")
    void cadaUnoVeLosSuyos() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 2, 1);
        contratarYCompletar("u-ana", "u-bruno", mentoria, 0);

        como("u-bruno");
        Pagina<MovimientoDto> deBruno = ok(wallet().movimientos(PaginaRequest.de(0, 1)));
        assertEquals(List.of(TipoMovimiento.PAGO), tipos(deBruno), "el mas reciente primero");
        assertEquals(2L, deBruno.totalElementos());
        assertEquals(0, sc(60).compareTo(ok(wallet().consultar()).saldo()));

        como("u-ana");
        assertEquals(List.of(TipoMovimiento.COBRO, TipoMovimiento.BONO_INICIAL),
                tipos(ok(wallet().movimientos(primeraPagina()))));
        assertEquals(0, sc(140).compareTo(ok(wallet().consultar()).saldo()));
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("movimientos sin pagina es VALIDACION")
    void sinPagina() {
        profesional("u-ana", "Ana");

        falla(VALIDACION, wallet().movimientos(null));
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("sin sesion o con el perfil dado de baja no se puede consultar la wallet")
    void sinActorValido() {
        profesional("u-ana", "Ana");

        sinSesion();
        falla(NO_AUTORIZADO, wallet().consultar());
        falla(NO_AUTORIZADO, wallet().movimientos(primeraPagina()));
        como("u-ana");
        ok(app.getPerfilController().darDeBaja());
        falla(NO_AUTORIZADO, wallet().consultar());
    }
}
