package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.DatosPublicacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Constructores de objetos de dominio para tests. Los ids que normalmente asigna la persistencia se
 * asignan aca por reflexion para poder armar escenarios sin repositorio.
 */
public final class Datos {

    public static final LocalDateTime AHORA = RelojFijo.INICIO_POR_DEFECTO;
    public static final Long PROVEEDOR = 1L;
    public static final Long CLIENTE = 2L;
    public static final Long OTRO = 3L;
    public static final Long PUBLICACION_ID = 100L;
    public static final Long CONTRATACION_ID = 500L;

    private Datos() { }

    /** Simula la asignacion de id que hace la persistencia. */
    public static void asignarId(Object entidad, Long id) {
        Reflejo.asignar(entidad, "id", id);
    }

    // ---------- perfiles ----------

    public static Perfil profesional(Long id, String userId) {
        Perfil p = Perfil.crear(userId, "Profesional " + userId, TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA);
        Reflejo.asignar(p, "id", id);
        return p;
    }

    public static Perfil organizacion(Long id, String userId) {
        Perfil p = Perfil.crear(userId, "Organizacion " + userId, TipoPerfil.ORGANIZACION, ZoneId.of("UTC"), AHORA);
        Reflejo.asignar(p, "id", id);
        return p;
    }

    // ---------- publicaciones ----------

    /** Borrador completo (mentoria PROFESIONAL) con {@code cantidadFranjas} franjas libres a partir de manana. */
    public static Publicacion borradorCompleto(Long autorId, int cantidadFranjas) {
        return borradorCompleto(autorId, cantidadFranjas, 1, SkillCoins.de(100));
    }

    public static Publicacion borradorCompleto(Long autorId, int cantidadFranjas, int cupo, SkillCoins precio) {
        Publicacion p = Publicacion.crearBorrador(TipoPublicacion.MENTORIA, autorId, TipoPerfil.PROFESIONAL,
                new DatosPublicacion("Mentoria de Java", "Clean Architecture y patrones", "programacion",
                        Modalidad.REMOTA, 60, cupo, precio, List.of("java", "backend"), List.of(Habilidad.de("Java")),
                        TipoPoliticaCancelacion.FLEXIBLE), AHORA);
        for (int i = 1; i <= cantidadFranjas; i++) {
            p.agregarFranja(AHORA.plusDays(i), AHORA.plusDays(i).plusHours(1), AHORA);
        }
        asignarIds(p, PUBLICACION_ID);
        return p;
    }

    public static Publicacion publicada(Long autorId, int cantidadFranjas) {
        return publicada(autorId, cantidadFranjas, 1, SkillCoins.de(100));
    }

    public static Publicacion publicada(Long autorId, int cantidadFranjas, int cupo, SkillCoins precio) {
        Publicacion p = borradorCompleto(autorId, cantidadFranjas, cupo, precio);
        p.publicar(autorId, AHORA);
        return p;
    }

    /** Asigna el id a la publicacion y ids 1..n a sus franjas sin id. */
    public static void asignarIds(Publicacion p, Long id) {
        Reflejo.asignar(p, "id", id);
        long siguiente = 1;
        for (Franja f : p.getFranjas()) {
            if (f.getId() == null) {
                Reflejo.asignar(f, "id", siguiente);
            }
            siguiente++;
        }
    }

    // ---------- contrataciones ----------

    public static Contratacion pendiente() {
        return pendiente(SkillCoins.de(100));
    }

    public static Contratacion pendiente(SkillCoins precioFinal) {
        Contratacion c = Contratacion.solicitar(PUBLICACION_ID, CLIENTE, PROVEEDOR, 1L, "Hola, quiero la mentoria",
                "Aprender patrones", precioFinal, AHORA);
        Reflejo.asignar(c, "id", CONTRATACION_ID);
        return c;
    }

    public static Contratacion aceptada() {
        Contratacion c = pendiente();
        c.aceptar(PROVEEDOR, AHORA.plusMinutes(1));
        return c;
    }

    public static Contratacion confirmada() {
        Contratacion c = aceptada();
        c.pagar(CLIENTE, AHORA.plusMinutes(2));
        return c;
    }

    public static Contratacion enCurso() {
        Contratacion c = confirmada();
        c.iniciar(PROVEEDOR, AHORA.plusMinutes(3));
        return c;
    }

    public static Contratacion completada() {
        Contratacion c = enCurso();
        c.completar(PROVEEDOR, AHORA.plusMinutes(4));
        return c;
    }

    public static Calificacion calificacion(Contratacion completada, Long autorId, int puntaje) {
        return Calificacion.registrar(completada, autorId, Puntaje.de(puntaje), "ok", AHORA.plusHours(1));
    }

    public static Set<Habilidad> habilidades(String... nombres) {
        java.util.LinkedHashSet<Habilidad> set = new java.util.LinkedHashSet<>();
        for (String n : nombres) {
            set.add(Habilidad.de(n));
        }
        return set;
    }

    public static Duration horas(long h) {
        return Duration.ofHours(h);
    }
}
