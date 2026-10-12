package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.LocalDateTime;

/** Comprobante de una contratacion completada (vista derivada, no se persiste por separado). */
public final class Comprobante {

    private final String numero;
    private final Long contratacionId;
    private final Long publicacionId;
    private final Long clienteId;
    private final Long proveedorId;
    private final SkillCoins monto;
    private final LocalDateTime pagadoEn;
    private final LocalDateTime completadoEn;

    public Comprobante(String numero, Long contratacionId, Long publicacionId, Long clienteId, Long proveedorId,
                       SkillCoins monto, LocalDateTime pagadoEn, LocalDateTime completadoEn) {
        this.numero = numero;
        this.contratacionId = contratacionId;
        this.publicacionId = publicacionId;
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.monto = monto;
        this.pagadoEn = pagadoEn;
        this.completadoEn = completadoEn;
    }

    public String getNumero() {
        return numero;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public SkillCoins getMonto() {
        return monto;
    }

    public LocalDateTime getPagadoEn() {
        return pagadoEn;
    }

    public LocalDateTime getCompletadoEn() {
        return completadoEn;
    }
}
