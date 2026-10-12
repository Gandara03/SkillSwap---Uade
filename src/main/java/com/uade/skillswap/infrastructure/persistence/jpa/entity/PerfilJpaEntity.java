package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Perfil persistido. El usuario externo se referencia por user_id (unico). Reputacion denormalizada: se recalcula en el dominio. */
@Entity
@Table(name = "perfil")
public class PerfilJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 100, nullable = false, updatable = false)
    private String userId;

    @Column(name = "tipo", length = 20, nullable = false, updatable = false)
    private String tipo;

    @Column(name = "nombre_visible", length = 150, nullable = false)
    private String nombreVisible;

    @Column(name = "biografia", length = 2000)
    private String biografia;

    @Column(name = "zona_horaria", length = 64, nullable = false)
    private String zonaHoraria;

    @Column(name = "canal_preferido", length = 20, nullable = false)
    private String canalPreferido;

    @Column(name = "reputacion", nullable = false)
    private double reputacion;

    @Column(name = "cantidad_calificaciones", nullable = false)
    private int cantidadCalificaciones;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "perfil_habilidad", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "habilidad", length = 100, nullable = false)
    private Set<String> habilidades = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "perfil_interes", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "interes", length = 100, nullable = false)
    private Set<String> intereses = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "perfil_disponibilidad", joinColumns = @JoinColumn(name = "perfil_id"))
    @OrderColumn(name = "posicion")
    private List<BloqueDisponibilidadEmbebible> disponibilidad = new ArrayList<>();

    public PerfilJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public void setNombreVisible(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public void setZonaHoraria(String zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    public String getCanalPreferido() {
        return canalPreferido;
    }

    public void setCanalPreferido(String canalPreferido) {
        this.canalPreferido = canalPreferido;
    }

    public double getReputacion() {
        return reputacion;
    }

    public void setReputacion(double reputacion) {
        this.reputacion = reputacion;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public void setCantidadCalificaciones(int cantidadCalificaciones) {
        this.cantidadCalificaciones = cantidadCalificaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Set<String> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(Set<String> habilidades) {
        this.habilidades = habilidades;
    }

    public Set<String> getIntereses() {
        return intereses;
    }

    public void setIntereses(Set<String> intereses) {
        this.intereses = intereses;
    }

    public List<BloqueDisponibilidadEmbebible> getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(List<BloqueDisponibilidadEmbebible> disponibilidad) {
        this.disponibilidad = disponibilidad;
    }
}
