package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Contratacion persistida. La propuesta de reprogramacion vive en columnas propuesta_* (todas null si no hay propuesta). */
@Entity
@Table(name = "contratacion")
public class ContratacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "publicacion_id", nullable = false, updatable = false)
    private Long publicacionId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private Long proveedorId;

    @Column(name = "franja_id", nullable = false)
    private Long franjaId;

    @Column(name = "mensaje", length = 2000, nullable = false, updatable = false)
    private String mensaje;

    @Column(name = "objetivo", length = 2000, nullable = false, updatable = false)
    private String objetivo;

    @Column(name = "precio_final", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal precioFinal;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "propuesta_franja_id")
    private Long propuestaFranjaId;

    @Column(name = "propuesta_por_id")
    private Long propuestaPorId;

    @Column(name = "propuesta_fecha")
    private LocalDateTime propuestaFecha;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "pagada_en")
    private LocalDateTime pagadaEn;

    @Column(name = "completada_en")
    private LocalDateTime completadaEn;

    @Column(name = "cancelada_por_id")
    private Long canceladaPorId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "contratacion", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<HistorialContratacionJpaEntity> historial = new ArrayList<>();

    public ContratacionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(Long publicacionId) {
        this.publicacionId = publicacionId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public Long getFranjaId() {
        return franjaId;
    }

    public void setFranjaId(Long franjaId) {
        this.franjaId = franjaId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(String objetivo) {
        this.objetivo = objetivo;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public void setPrecioFinal(BigDecimal precioFinal) {
        this.precioFinal = precioFinal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getPropuestaFranjaId() {
        return propuestaFranjaId;
    }

    public void setPropuestaFranjaId(Long propuestaFranjaId) {
        this.propuestaFranjaId = propuestaFranjaId;
    }

    public Long getPropuestaPorId() {
        return propuestaPorId;
    }

    public void setPropuestaPorId(Long propuestaPorId) {
        this.propuestaPorId = propuestaPorId;
    }

    public LocalDateTime getPropuestaFecha() {
        return propuestaFecha;
    }

    public void setPropuestaFecha(LocalDateTime propuestaFecha) {
        this.propuestaFecha = propuestaFecha;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDateTime getPagadaEn() {
        return pagadaEn;
    }

    public void setPagadaEn(LocalDateTime pagadaEn) {
        this.pagadaEn = pagadaEn;
    }

    public LocalDateTime getCompletadaEn() {
        return completadaEn;
    }

    public void setCompletadaEn(LocalDateTime completadaEn) {
        this.completadaEn = completadaEn;
    }

    public Long getCanceladaPorId() {
        return canceladaPorId;
    }

    public void setCanceladaPorId(Long canceladaPorId) {
        this.canceladaPorId = canceladaPorId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<HistorialContratacionJpaEntity> getHistorial() {
        return historial;
    }

    public void setHistorial(List<HistorialContratacionJpaEntity> historial) {
        this.historial = historial;
    }
}
