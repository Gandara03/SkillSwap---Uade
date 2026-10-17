package com.uade.skillswap.domain.model.wallet;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Billetera de SkillCoins de un perfil. Nunca admite saldo negativo.
 * Los movimientos nuevos quedan en memoria hasta que el repositorio los persiste.
 */
public class Wallet {

    private Long id;
    private final Long perfilId;
    private SkillCoins saldo;
    private Long version;
    private final List<MovimientoCredito> movimientosNuevos = new ArrayList<>();

    private Wallet(Long id, Long perfilId, SkillCoins saldo, Long version) {
        this.id = id;
        this.perfilId = perfilId;
        this.saldo = saldo;
        this.version = version;
    }

    public static Wallet abrir(Long perfilId, SkillCoins saldoInicial, LocalDateTime ahora) {
        Wallet wallet = new Wallet(null, perfilId, SkillCoins.CERO, null);
        if (saldoInicial != null && !saldoInicial.esCero()) {
            wallet.acreditar(saldoInicial, TipoMovimiento.BONO_INICIAL, null, ahora);
        }
        return wallet;
    }

    /** Solo para rehidratar desde persistencia. */
    public static Wallet reconstruir(Long id, Long perfilId, SkillCoins saldo, Long version) {
        return new Wallet(id, perfilId, saldo, version);
    }

    public void debitar(SkillCoins monto, TipoMovimiento tipo, Long contratacionId, LocalDateTime ahora) {
        exigirMontoPositivo(monto);
        if (tipo.esCredito()) {
            throw new ReglaDeNegocioException("El tipo de movimiento " + tipo + " no es un debito");
        }
        exigirSaldoSuficiente(monto);
        saldo = saldo.restar(monto);
        movimientosNuevos.add(new MovimientoCredito(null, perfilId, tipo, monto, saldo, contratacionId, ahora));
    }

    /** Falla si el saldo no alcanza para debitar ese monto. No modifica nada. */
    public void exigirSaldoSuficiente(SkillCoins monto) {
        if (saldo.esMenorQue(monto)) {
            throw new SaldoInsuficienteException("Saldo insuficiente: disponible " + saldo + ", requerido " + monto);
        }
    }

    public void acreditar(SkillCoins monto, TipoMovimiento tipo, Long contratacionId, LocalDateTime ahora) {
        exigirMontoPositivo(monto);
        if (!tipo.esCredito()) {
            throw new ReglaDeNegocioException("El tipo de movimiento " + tipo + " no es un credito");
        }
        saldo = saldo.sumar(monto);
        movimientosNuevos.add(new MovimientoCredito(null, perfilId, tipo, monto, saldo, contratacionId, ahora));
    }

    private static void exigirMontoPositivo(SkillCoins monto) {
        if (monto == null || monto.esNegativo() || monto.esCero()) {
            throw new ReglaDeNegocioException("El monto del movimiento debe ser mayor a cero");
        }
    }

    public List<MovimientoCredito> getMovimientosNuevos() {
        return Collections.unmodifiableList(movimientosNuevos);
    }

    /** Lo llama el repositorio luego de persistir los movimientos nuevos. */
    public void limpiarMovimientosNuevos() {
        movimientosNuevos.clear();
    }

    public Long getId() {
        return id;
    }

    public Long getPerfilId() {
        return perfilId;
    }

    public SkillCoins getSaldo() {
        return saldo;
    }

    public Long getVersion() {
        return version;
    }
}
