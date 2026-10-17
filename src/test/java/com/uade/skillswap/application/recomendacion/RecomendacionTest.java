package com.uade.skillswap.application.recomendacion;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.publicacion.DatosPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Estrategias de recomendacion (Strategy + Simple Factory)")
class RecomendacionTest {

    private static final Long USUARIO = 50L;

    /** Perfil con intereses java y testing. */
    private static Perfil usuario() {
        Perfil p = Perfil.crear("u-user", "User", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA);
        Datos.asignarId(p, USUARIO);
        p.actualizarDatos("User", null, null, List.of(), List.of("Java", "testing"), List.of(), null);
        return p;
    }

    /** Publicada con id, autor, habilidades/etiquetas/categoria dados y cupo ocupado = ocupado. */
    private static Publicacion publicada(long id, long autor, String categoria, List<String> etiquetas,
                                         List<Habilidad> habilidades, int ocupado) {
        Publicacion p = Publicacion.crearBorrador(TipoPublicacion.MENTORIA, autor, TipoPerfil.PROFESIONAL,
                new DatosPublicacion("T" + id, "d", categoria, Modalidad.REMOTA, 60, 10, SkillCoins.de(10), etiquetas,
                        habilidades, TipoPoliticaCancelacion.FLEXIBLE), AHORA);
        for (int i = 0; i < Math.max(ocupado, 1); i++) {
            p.agregarFranja(AHORA.plusDays(i + 1), AHORA.plusDays(i + 1).plusHours(1), AHORA);
        }
        Datos.asignarIds(p, id);
        p.publicar(autor, AHORA);
        for (int i = 1; i <= ocupado; i++) {
            p.reservarFranja((long) i, AHORA);
        }
        return p;
    }

    private static List<Long> ids(List<Publicacion> lista) {
        return lista.stream().map(Publicacion::getId).collect(Collectors.toList());
    }

    // ---------- afinidad ----------

    @Test
    @DisplayName("AFINIDAD: ordena por coincidencia con los intereses (habilidad=2, etiqueta=1, categoria=1)")
    void afinidadOrdenaPorPuntos() {
        Publicacion poca = publicada(1, 10, "otra", List.of("testing"), List.of(), 0);                     // 1 punto
        Publicacion mucha = publicada(2, 11, "java", List.of("testing"), List.of(Habilidad.de("Java")), 0); // 2+1+1 = 4
        Publicacion media = publicada(3, 12, "otra", List.of(), List.of(Habilidad.de("java")), 0);          // 2
        List<Publicacion> r = new RecomendacionPorAfinidad().recomendar(
                new DatosRecomendacion(usuario(), Map.of()), List.of(poca, mucha, media), 10);
        assertEquals(List.of(2L, 3L, 1L), ids(r));
    }

    @Test
    @DisplayName("AFINIDAD: excluye publicaciones sin ninguna coincidencia")
    void afinidadExcluyeSinCoincidencia() {
        Publicacion sin = publicada(1, 10, "cocina", List.of("pastas"), List.of(Habilidad.de("Cocina")), 0);
        Publicacion con = publicada(2, 10, "x", List.of("java"), List.of(), 0);
        List<Publicacion> r = new RecomendacionPorAfinidad().recomendar(
                new DatosRecomendacion(usuario(), Map.of()), List.of(sin, con), 10);
        assertEquals(List.of(2L), ids(r));
    }

    @Test
    @DisplayName("AFINIDAD: desempata por reputacion del autor y luego por id")
    void afinidadDesempate() {
        Publicacion a = publicada(1, 10, "x", List.of("java"), List.of(), 0);
        Publicacion b = publicada(2, 11, "x", List.of("java"), List.of(), 0);
        Publicacion c = publicada(3, 12, "x", List.of("java"), List.of(), 0);
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of(10L, 3.0, 11L, 5.0, 12L, 5.0));
        assertEquals(List.of(2L, 3L, 1L), ids(new RecomendacionPorAfinidad().recomendar(datos, List.of(a, b, c), 10)));
    }

    @Test
    @DisplayName("las estrategias nunca recomiendan al usuario sus propias publicaciones")
    void excluyePropias() {
        Publicacion propia = publicada(1, USUARIO, "java", List.of("java"), List.of(Habilidad.de("java")), 3);
        Publicacion ajena = publicada(2, 10, "java", List.of("java"), List.of(), 0);
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of());
        assertEquals(List.of(2L), ids(new RecomendacionPorAfinidad().recomendar(datos, List.of(propia, ajena), 10)));
        assertEquals(List.of(2L), ids(new RecomendacionPorPopularidad().recomendar(datos, List.of(propia, ajena), 10)));
    }

    @Test
    @DisplayName("no recomiendan publicaciones no visibles (pausadas)")
    void excluyeNoVisibles() {
        Publicacion pausada = publicada(1, 10, "java", List.of("java"), List.of(), 0);
        pausada.pausar(10L, AHORA);
        Publicacion visible = publicada(2, 10, "java", List.of("java"), List.of(), 0);
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of());
        assertEquals(List.of(2L), ids(new RecomendacionPorAfinidad().recomendar(datos, List.of(pausada, visible), 10)));
        assertEquals(List.of(2L), ids(new RecomendacionPorPopularidad().recomendar(datos, List.of(pausada, visible), 10)));
    }

    // ---------- popularidad ----------

    @Test
    @DisplayName("POPULARIDAD: ordena por cupo ocupado descendente y luego por reputacion del autor")
    void popularidadOrdena() {
        Publicacion poco = publicada(1, 10, "x", List.of(), List.of(), 1);
        Publicacion mucho = publicada(2, 11, "x", List.of(), List.of(), 3);
        Publicacion empateMejorRep = publicada(3, 12, "x", List.of(), List.of(), 1);
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of(10L, 2.0, 12L, 4.5));
        List<Publicacion> r = new RecomendacionPorPopularidad().recomendar(datos, List.of(poco, mucho, empateMejorRep), 10);
        assertEquals(List.of(2L, 3L, 1L), ids(r));
    }

    @Test
    @DisplayName("POPULARIDAD no exige coincidencia con intereses")
    void popularidadSinIntereses() {
        Publicacion p = publicada(1, 10, "cocina", List.of(), List.of(), 0);
        List<Publicacion> r = new RecomendacionPorPopularidad().recomendar(
                new DatosRecomendacion(usuario(), Map.of()), List.of(p), 10);
        assertEquals(List.of(1L), ids(r));
    }

    // ---------- diferencias y limites ----------

    @Test
    @DisplayName("las dos estrategias producen ordenes distintos sobre las mismas candidatas")
    void ordenesDistintos() {
        // afin: coincide con intereses pero nadie la contrato; popular: muy contratada pero con 1 punto de afinidad
        Publicacion afin = publicada(1, 10, "java", List.of("testing"), List.of(Habilidad.de("Java")), 0);
        Publicacion popular = publicada(2, 11, "otra", List.of("java"), List.of(), 4);
        List<Publicacion> candidatas = List.of(afin, popular);
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of());
        List<Long> porAfinidad = ids(new RecomendacionPorAfinidad().recomendar(datos, candidatas, 10));
        List<Long> porPopularidad = ids(new RecomendacionPorPopularidad().recomendar(datos, candidatas, 10));
        assertEquals(List.of(1L, 2L), porAfinidad);
        assertEquals(List.of(2L, 1L), porPopularidad);
        assertNotEquals(porAfinidad, porPopularidad);
    }

    @Test
    @DisplayName("el limite acota la cantidad devuelta; limite 0 o negativo devuelve vacio")
    void limite() {
        List<Publicacion> candidatas = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            candidatas.add(publicada(i, 10, "x", List.of("java"), List.of(), 0));
        }
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of());
        assertEquals(2, new RecomendacionPorAfinidad().recomendar(datos, candidatas, 2).size());
        assertEquals(3, new RecomendacionPorPopularidad().recomendar(datos, candidatas, 3).size());
        assertTrue(new RecomendacionPorAfinidad().recomendar(datos, candidatas, 0).isEmpty());
        assertTrue(new RecomendacionPorPopularidad().recomendar(datos, candidatas, -4).isEmpty());
    }

    @Test
    @DisplayName("sin candidatas devuelven una lista vacia")
    void sinCandidatas() {
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of());
        assertTrue(new RecomendacionPorAfinidad().recomendar(datos, List.of(), 5).isEmpty());
        assertTrue(new RecomendacionPorPopularidad().recomendar(datos, List.of(), 5).isEmpty());
    }

    @Test
    @DisplayName("los intereses se comparan sin distinguir mayusculas ni espacios")
    void interesesNormalizados() {
        Perfil p = Perfil.crear("u-q", "Q", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA);
        Datos.asignarId(p, USUARIO);
        p.actualizarDatos("Q", null, null, List.of(), List.of("  JAVA  "), List.of(), null);
        Publicacion pub = publicada(1, 10, "x", List.of("java"), List.of(), 0);
        assertEquals(List.of(1L), ids(new RecomendacionPorAfinidad().recomendar(
                new DatosRecomendacion(p, Map.of()), List.of(pub), 5)));
    }

    // ---------- datos y fabrica ----------

    @Test
    @DisplayName("DatosRecomendacion devuelve 0 para autores sin reputacion y tolera mapa nulo")
    void datos() {
        DatosRecomendacion datos = new DatosRecomendacion(usuario(), Map.of(1L, 4.0));
        assertEquals(4.0, datos.reputacionDe(1L), 0.0001);
        assertEquals(0.0, datos.reputacionDe(2L), 0.0001);
        assertEquals(0.0, new DatosRecomendacion(usuario(), null).reputacionDe(1L), 0.0001);
    }

    @Test
    @DisplayName("la fabrica devuelve la estrategia por tipo y AFINIDAD por defecto")
    void fabrica() {
        assertInstanceOf(RecomendacionPorAfinidad.class, EstrategiaRecomendacionFactory.crear(TipoEstrategiaRecomendacion.AFINIDAD));
        assertInstanceOf(RecomendacionPorPopularidad.class, EstrategiaRecomendacionFactory.crear(TipoEstrategiaRecomendacion.POPULARIDAD));
        assertInstanceOf(RecomendacionPorAfinidad.class, EstrategiaRecomendacionFactory.crear(null));
        assertEquals(TipoEstrategiaRecomendacion.POPULARIDAD,
                EstrategiaRecomendacionFactory.crear(TipoEstrategiaRecomendacion.POPULARIDAD).getTipo());
    }
}
