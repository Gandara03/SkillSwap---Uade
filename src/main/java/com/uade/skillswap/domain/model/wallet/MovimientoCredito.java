package com.uade.skillswap.domain.model.wallet;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.LocalDateTime;

/** Movimiento de la wallet. Inmutable: el monto es siempre positivo y el tipo define si suma o resta. */
public final class MovimientoCredito {

    private final Long id;
    private final Long perfilId;
    private final TipoMovimiento tipo;
    private final SkillCoins monto;
    private final SkillCoins saldoPosterior;
    private final Long contratacionId;
    private final LocalDateTime fecha;

    public MovimientoCredito(Long id, Long perfilId, TipoMovimiento tipo, SkillCoins monto, SkillCoins saldoPosterior,
                             Long contratacionId, LocalDateTime fecha) {
        this.id = id;
        this.perfilId = perfilId;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoPosterior = saldoPosterior;
        this.contratacionId = contratacionId;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getPerfilId() {
        return perfilId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public SkillCoins getMonto() {
        return monto;
    }

    public SkillCoins getSaldoPosterior() {
        return saldoPosterior;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
