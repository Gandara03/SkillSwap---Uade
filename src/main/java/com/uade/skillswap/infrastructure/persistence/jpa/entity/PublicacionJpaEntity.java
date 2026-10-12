package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Publicacion persistida. 'revision' se incrementa cuando cambian las franjas para que el @Version del agregado detecte tambien reservas que no tocan otras columnas (p. ej. reprogramar). */
@Entity
@Table(name = "publicacion")
public class PublicacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo", length = 20, nullable = false, updatable = false)
    private String tipo;

    @Column(name = "autor_id", nullable = false, updatable = false)
    private Long autorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", insertable = false, updatable = false)
    private PerfilJpaEntity autor;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "descripcion", length = 4000)
    private String descripcion;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "modalidad", length = 20)
    private String modalidad;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(name = "cupo", nullable = false)
    private int cupo;

    @Column(name = "cupo_ocupado", nullable = false)
    private int cupoOcupado;

    @Column(name = "precio_base", precision = 19, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "politica_cancelacion", length = 20, nullable = false)
    private String politicaCancelacion;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "publicada_en")
    private LocalDateTime publicadaEn;

    @Column(name = "revision", nullable = false)
    private int revision;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "publicacion_etiqueta", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta", length = 100, nullable = false)
    private Set<String> etiquetas = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "publicacion_habilidad", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "habilidad", length = 100, nullable = false)
    private Set<String> habilidades = new LinkedHashSet<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<FranjaJpaEntity> franjas = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<HistorialPublicacionJpaEntity> historial = new ArrayList<>();

    public PublicacionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public PerfilJpaEntity getAutor() {
        return autor;
    }

    public void setAutor(PerfilJpaEntity autor) {
        this.autor = autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public int getCupo() {
        return cupo;
    }

    public void setCupo(int cupo) {
        this.cupo = cupo;
    }

    public int getCupoOcupado() {
        return cupoOcupado;
    }

    public void setCupoOcupado(int cupoOcupado) {
        this.cupoOcupado = cupoOcupado;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public String getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public void setPoliticaCancelacion(String politicaCancelacion) {
        this.politicaCancelacion = politicaCancelacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDateTime getPublicadaEn() {
        return publicadaEn;
    }

    public void setPublicadaEn(LocalDateTime publicadaEn) {
        this.publicadaEn = publicadaEn;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Set<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(Set<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public Set<String> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(Set<String> habilidades) {
        this.habilidades = habilidades;
    }

    public List<FranjaJpaEntity> getFranjas() {
        return franjas;
    }

    public void setFranjas(List<FranjaJpaEntity> franjas) {
        this.franjas = franjas;
    }

    public List<HistorialPublicacionJpaEntity> getHistorial() {
        return historial;
    }

    public void setHistorial(List<HistorialPublicacionJpaEntity> historial) {
        this.historial = historial;
    }
}
