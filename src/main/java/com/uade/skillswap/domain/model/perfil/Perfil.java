package com.uade.skillswap.domain.model.perfil;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Perfil profesional u organizacion. Referencia al usuario del servicio de identidad externo
 * mediante userId. La reputacion es de solo lectura: se recalcula desde calificaciones verificadas.
 */
public class Perfil {

    private Long id;
    private final String userId;
    private final TipoPerfil tipo;
    private String nombreVisible;
    private String biografia;
    private ZoneId zonaHoraria;
    private final Set<Habilidad> habilidades = new LinkedHashSet<>();
    private final Set<String> intereses = new LinkedHashSet<>();
    private final List<BloqueDisponibilidad> disponibilidad = new ArrayList<>();
    private CanalNotificacion canalPreferido = CanalNotificacion.INTERNA;
    private double reputacion;
    private int cantidadCalificaciones;
    private boolean activo = true;
    private final LocalDateTime creadoEn;
    private Long version;

    private Perfil(Long id, String userId, TipoPerfil tipo, LocalDateTime creadoEn) {
        this.id = id;
        this.userId = userId;
        this.tipo = tipo;
        this.creadoEn = creadoEn;
    }

    public static Perfil crear(String userId, String nombreVisible, TipoPerfil tipo, ZoneId zonaHoraria,
                               LocalDateTime ahora) {
        if (userId == null || userId.isBlank()) {
            throw new ReglaDeNegocioException("El userId del servicio de identidad es obligatorio");
        }
        if (tipo == null) {
            throw new ReglaDeNegocioException("El tipo de perfil es obligatorio");
        }
        Perfil perfil = new Perfil(null, userId.trim(), tipo, ahora);
        perfil.nombreVisible = exigirNombre(nombreVisible);
        perfil.zonaHoraria = zonaHoraria != null ? zonaHoraria : ZoneId.of("UTC");
        return perfil;
    }

    /** Solo para rehidratar desde persistencia. */
    public static Perfil reconstruir(Long id, String userId, TipoPerfil tipo, String nombreVisible, String biografia,
                                     ZoneId zonaHoraria, Collection<Habilidad> habilidades, Collection<String> intereses,
                                     Collection<BloqueDisponibilidad> disponibilidad, CanalNotificacion canalPreferido,
                                     double reputacion, int cantidadCalificaciones, boolean activo,
                                     LocalDateTime creadoEn, Long version) {
        Perfil p = new Perfil(id, userId, tipo, creadoEn);
        p.nombreVisible = nombreVisible;
        p.biografia = biografia;
        p.zonaHoraria = zonaHoraria;
        p.habilidades.addAll(habilidades);
        p.intereses.addAll(intereses);
        p.disponibilidad.addAll(disponibilidad);
        p.canalPreferido = canalPreferido;
        p.reputacion = reputacion;
        p.cantidadCalificaciones = cantidadCalificaciones;
        p.activo = activo;
        p.version = version;
        return p;
    }

    public void actualizarDatos(String nombreVisible, String biografia, ZoneId zonaHoraria,
                                Collection<Habilidad> habilidades, Collection<String> intereses,
                                Collection<BloqueDisponibilidad> disponibilidad, CanalNotificacion canalPreferido) {
        exigirActivo();
        this.nombreVisible = exigirNombre(nombreVisible);
        this.biografia = biografia;
        if (zonaHoraria != null) {
            this.zonaHoraria = zonaHoraria;
        }
        reemplazar(this.habilidades, habilidades);
        reemplazar(this.intereses, intereses);
        reemplazar(this.disponibilidad, disponibilidad);
        if (canalPreferido != null) {
            this.canalPreferido = canalPreferido;
        }
    }

    /** Recalcula la reputacion como promedio de las calificaciones verificadas recibidas. */
    public void recalcularReputacion(List<Calificacion> calificacionesRecibidas) {
        this.cantidadCalificaciones = calificacionesRecibidas.size();
        this.reputacion = calificacionesRecibidas.isEmpty() ? 0.0
                : calificacionesRecibidas.stream().mapToInt(c -> c.getPuntaje().getValor()).average().orElse(0.0);
    }

    /** Baja logica: el perfil queda inactivo pero conserva su historial. */
    public void darDeBaja() {
        this.activo = false;
    }

    private void exigirActivo() {
        if (!activo) {
            throw new ReglaDeNegocioException("El perfil esta dado de baja");
        }
    }

    private static String exigirNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre visible es obligatorio");
        }
        return nombre.trim();
    }

    private static <T> void reemplazar(Collection<T> destino, Collection<T> origen) {
        destino.clear();
        if (origen != null) {
            destino.addAll(origen);
        }
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public TipoPerfil getTipo() {
        return tipo;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public Set<Habilidad> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public Set<String> getIntereses() {
        return Collections.unmodifiableSet(intereses);
    }

    public List<BloqueDisponibilidad> getDisponibilidad() {
        return Collections.unmodifiableList(disponibilidad);
    }

    public CanalNotificacion getCanalPreferido() {
        return canalPreferido;
    }

    public double getReputacion() {
        return reputacion;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public boolean estaActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public Long getVersion() {
        return version;
    }
}
