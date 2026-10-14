package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.ConfiguracionCancelacion;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RecomendacionService: Strategy intercambiable por parametro sobre el mismo servicio")
class RecomendacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto cecilia; // quien pide recomendaciones; le interesa "java"
    private PerfilDto ana;
    private PerfilDto bruno;
    private PerfilDto dario;
    private PerfilDto elena;

    /**
     * Catalogo: "Java" (ana, afin a Cecilia, sin reservas), "SQL" (bruno, no afin, la mas contratada: 2 reservas),
     * "Kotlin" (bruno, no afin, sin reservas) y "Mi propia mentoria" (de Cecilia, nunca se le recomienda).
     */
    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        cecilia = e.registrar("u-cecilia", "Cecilia", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of("sql"), Set.of("java"));
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        dario = e.profesional("u-dario", "Dario");
        elena = e.profesional("u-elena", "Elena");

        e.publicada(ana, e.oferta().titulo("Java").categoria("programacion").habilidades("java").etiquetas("backend").cupo(2));
        PublicacionDto sql = e.publicada(bruno, e.oferta().titulo("SQL").categoria("datos").habilidades("sql").etiquetas("bases").cupo(3).franjasEn(100L, 130L));
        e.publicada(bruno, e.oferta().titulo("Kotlin").categoria("movil").habilidades("kotlin").etiquetas("android"));
        e.publicada(cecilia, e.oferta().titulo("Mi propia mentoria").habilidades("java"));
        e.aceptada(dario, bruno, sql, 0);
        e.aceptada(elena, bruno, sql, 1);
    }

    private List<String> titulos(PerfilDto actor, TipoEstrategiaRecomendacion estrategia) {
        return app.recomendacionUseCase.recomendar(actor.id(), estrategia).stream().map(PublicacionResumenDto::titulo).toList();
    }

    /** Califica al autor con un cliente, sobre una publicacion aparte que luego se cierra (para no alterar el catalogo). */
    private void darReputacion(PerfilDto autor, PerfilDto cliente, int puntaje) {
        PublicacionDto sesion = e.publicada(autor, e.oferta().titulo("Sesion pasada de " + autor.nombreVisible()).franjasEn(50L));
        e.calificar(cliente, e.completada(cliente, autor, sesion, 0), puntaje);
        app.publicacionUseCase.cerrar(autor.id(), sesion.id());
    }

    @Test
    @DisplayName("AFINIDAD recomienda lo que coincide con los intereses del perfil y deja afuera lo que no")
    void porAfinidad() {
        assertEquals(List.of("Java"), titulos(cecilia, TipoEstrategiaRecomendacion.AFINIDAD));
    }

    @Test
    @DisplayName("POPULARIDAD recomienda primero lo mas contratado, aunque no coincida con los intereses")
    void porPopularidad() {
        assertEquals(List.of("SQL", "Java", "Kotlin"), titulos(cecilia, TipoEstrategiaRecomendacion.POPULARIDAD));
    }

    @Test
    @DisplayName("el MISMO servicio da resultados distintos segun la estrategia elegida por parametro")
    void estrategiasDistintasMismoServicio() {
        List<String> afinidad = titulos(cecilia, TipoEstrategiaRecomendacion.AFINIDAD);
        List<String> popularidad = titulos(cecilia, TipoEstrategiaRecomendacion.POPULARIDAD);

        assertNotEquals(afinidad, popularidad);
        assertEquals("Java", afinidad.get(0));
        assertEquals("SQL", popularidad.get(0));
    }

    @Test
    @DisplayName("sin estrategia explicita se usa AFINIDAD")
    void estrategiaPorDefecto() {
        assertEquals(titulos(cecilia, TipoEstrategiaRecomendacion.AFINIDAD), titulos(cecilia, null));
    }

    @Test
    @DisplayName("nunca se recomiendan las publicaciones propias, ni las pausadas, cerradas o en borrador")
    void soloAjenasYPublicadas() {
        PublicacionDto pausada = e.publicada(ana, e.oferta().titulo("Pausada").habilidades("java"));
        app.publicacionUseCase.pausar(ana.id(), pausada.id());
        e.borrador(ana, e.oferta().titulo("Borrador").habilidades("java"));

        for (TipoEstrategiaRecomendacion estrategia : TipoEstrategiaRecomendacion.values()) {
            List<String> recomendadas = titulos(cecilia, estrategia);
            assertTrue(!recomendadas.contains("Mi propia mentoria"), estrategia + ": no se recomienda lo propio");
            assertTrue(!recomendadas.contains("Pausada") && !recomendadas.contains("Borrador"), estrategia + ": solo publicadas");
        }
    }

    @Test
    @DisplayName("a igual afinidad desempata la reputacion del autor")
    void desempataPorReputacion() {
        e.publicada(ana, e.oferta().titulo("Java de Ana").habilidades("java").etiquetas("backend"));
        e.publicada(bruno, e.oferta().titulo("Java de Bruno").habilidades("java").etiquetas("backend"));
        darReputacion(ana, dario, 2);
        darReputacion(bruno, dario, 5);

        List<String> afinidad = titulos(cecilia, TipoEstrategiaRecomendacion.AFINIDAD);

        assertEquals(List.of("Java de Bruno", "Java", "Java de Ana"), afinidad, "5.0 antes que las de Ana (2.0), y entre estas por id");
    }

    @Test
    @DisplayName("el limite de recomendaciones sale de la configuracion")
    void limiteConfigurable() {
        SkillSwapConfig config = new SkillSwapConfig(SkillCoins.de(100), 200, 1, 5, 5, ConfiguracionCancelacion.porDefecto());
        Escenario chico = new Escenario(AplicacionEnMemoria.crear(config));
        PerfilDto cliente = chico.registrar("u-cliente", "Cliente", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of("java"));
        PerfilDto autora = chico.profesional("u-autora", "Autora");
        chico.publicada(autora, chico.oferta().titulo("Uno").habilidades("java"));
        chico.publicada(autora, chico.oferta().titulo("Dos").habilidades("java"));

        assertEquals(1, chico.app.recomendacionUseCase.recomendar(cliente.id(), TipoEstrategiaRecomendacion.POPULARIDAD).size());
        assertEquals(1, chico.app.recomendacionUseCase.recomendar(cliente.id(), TipoEstrategiaRecomendacion.AFINIDAD).size());
    }

    @Test
    @DisplayName("las filas traen autor, reputacion y precio final listos para mostrar")
    void filasCompletas() {
        PublicacionResumenDto fila = app.recomendacionUseCase.recomendar(cecilia.id(), TipoEstrategiaRecomendacion.AFINIDAD).get(0);

        assertEquals("Ana", fila.autorNombre());
        assertEquals(ana.id(), fila.autorId());
        Escenario.assertMonto("40", fila.precioFinal());
        assertEquals("PUBLICADA", fila.estado());
        assertEquals(2, fila.cupoDisponible());
    }

    @Test
    @DisplayName("sin publicaciones afines AFINIDAD devuelve una lista vacia, no falla")
    void sinCoincidencias() {
        PerfilDto sinIntereses = e.registrar("u-nuevo", "Nuevo", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of("cocina"));

        assertTrue(titulos(sinIntereses, TipoEstrategiaRecomendacion.AFINIDAD).isEmpty());
        assertEquals(List.of("SQL", "Java", "Kotlin", "Mi propia mentoria"),
                titulos(sinIntereses, TipoEstrategiaRecomendacion.POPULARIDAD));
    }

    @Test
    @DisplayName("un perfil dado de baja no recibe recomendaciones; uno inexistente o nulo tampoco")
    void actorInvalido() {
        app.perfilUseCase.darDeBaja(dario.id());

        assertThrows(NoAutorizadoException.class, () -> app.recomendacionUseCase.recomendar(dario.id(), TipoEstrategiaRecomendacion.AFINIDAD));
        assertThrows(RecursoNoEncontradoException.class, () -> app.recomendacionUseCase.recomendar(999L, TipoEstrategiaRecomendacion.AFINIDAD));
        assertThrows(ValidacionException.class, () -> app.recomendacionUseCase.recomendar(null, TipoEstrategiaRecomendacion.AFINIDAD));
    }
}
