package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.MovimientoCreditoJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.WalletJpaEntity;

/** Wallet y movimientos (dominio) <-> entidades. Los movimientos nuevos del agregado se insertan aparte. */
public final class WalletMapper {

    private WalletMapper() {
    }

    public static Wallet aDominio(WalletJpaEntity e) {
        return Wallet.reconstruir(e.getId(), e.getPerfilId(), SkillCoins.de(e.getSaldo()), e.getVersion());
    }

    public static void aplicar(Wallet d, WalletJpaEntity e) {
        e.setPerfilId(d.getPerfilId());
        e.setSaldo(d.getSaldo().getMonto());
    }

    public static MovimientoCredito movimientoADominio(MovimientoCreditoJpaEntity e) {
        return new MovimientoCredito(e.getId(), e.getPerfilId(), TipoMovimiento.valueOf(e.getTipo()),
                SkillCoins.de(e.getMonto()), SkillCoins.de(e.getSaldoPosterior()), e.getContratacionId(), e.getFecha());
    }

    public static MovimientoCreditoJpaEntity movimientoAEntidad(MovimientoCredito m) {
        MovimientoCreditoJpaEntity e = new MovimientoCreditoJpaEntity();
        e.setPerfilId(m.getPerfilId());
        e.setTipo(m.getTipo().name());
        e.setMonto(m.getMonto().getMonto());
        e.setSaldoPosterior(m.getSaldoPosterior().getMonto());
        e.setContratacionId(m.getContratacionId());
        e.setFecha(Fechas.enMicros(m.getFecha()));
        return e;
    }

    public static void confirmar(Wallet d, WalletJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
        CamposDominio.asignar(d, "version", e.getVersion());
    }
}
