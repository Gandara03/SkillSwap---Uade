package com.uade.skillswap.domain.model.promocion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Promocion aplicable a una publicacion. Las promociones vigentes se acumulan (Decorator de precio)
 * y el total nunca queda negativo.
 */
public class Promocion {

    private Long id;
    private final Long publicacionId;
    private final String descripcion;
    private final TipoPromocion tipo;
    private final BigDecimal valor;
    private final LocalDateTime vigenteDesde;
    private final LocalDateTime vigenteHasta;
    private boolean activa;

    private Promocion(Long id, Long publicacionId, String descripcion, TipoPromocion tipo, BigDecimal valor,
                      LocalDateTime vigenteDesde, LocalDateTime vigenteHasta, boolean activa) {
        this.id = id;
        this.publicacionId = publicacionId;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.valor = valor;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.activa = activa;
    }

    public static Promocion crear(Long publicacionId, String descripcion, TipoPromocion tipo, BigDecimal valor,
                                  LocalDateTime vigenteDesde, LocalDateTime vigenteHasta) {
        if (publicacionId == null) {
            throw new ReglaDeNegocioException("La promocion debe pertenecer a una publicacion");
        }
        if (tipo == null || valor == null || valor.signum() <= 0) {
            throw new ReglaDeNegocioException("La promocion requiere un tipo y un valor positivo");
        }
        if (tipo == TipoPromocion.PORCENTAJE && valor.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ReglaDeNegocioException("El porcentaje de la promocion no puede superar 100");
        }
        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            throw new ReglaDeNegocioException("La vigencia de la promocion es invalida");
        }
        return new Promocion(null, publicacionId, descripcion, tipo, valor, vigenteDesde, vigenteHasta, true);
    }

    public static Promocion reconstruir(Long id, Long publicacionId, String descripcion, TipoPromocion tipo,
                                        BigDecimal valor, LocalDateTime vigenteDesde, LocalDateTime vigenteHasta,
                                        boolean activa) {
        return new Promocion(id, publicacionId, descripcion, tipo, valor, vigenteDesde, vigenteHasta, activa);
    }

    public boolean estaVigente(LocalDateTime ahora) {
        if (!activa) {
            return false;
        }
        boolean desdeOk = vigenteDesde == null || !ahora.isBefore(vigenteDesde);
        boolean hastaOk = vigenteHasta == null || !ahora.isAfter(vigenteHasta);
        return desdeOk && hastaOk;
    }

    public void desactivar() {
        this.activa = false;
    }

    public Long getId() { return id; }
    public Long getPublicacionId() { return publicacionId; }
    public String getDescripcion() { return descripcion; }
    public TipoPromocion getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getVigenteDesde() { return vigenteDesde; }
    public LocalDateTime getVigenteHasta() { return vigenteHasta; }
    public boolean isActiva() { return activa; }
}
