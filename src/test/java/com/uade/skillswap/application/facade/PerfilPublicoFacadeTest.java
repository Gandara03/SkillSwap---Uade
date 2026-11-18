package com.uade.skillswap.application.facade;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionCompletadaDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.domain.model.politica.PoliticaEstricta;
import com.uade.skillswap.domain.model.politica.PoliticaFlexible;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PerfilPublicoFacade: vista resumida del perfil (Facade sobre perfil, calificaciones, publicaciones y contrataciones)")
class PerfilPublicoFacadeTest {

    private Escenario e;
    private AplicacionEnMemoria app;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
    }

    @Test
    @DisplayName("el perfil publico arma reputacion, ultimas calificaciones, solo las publicaciones activas e historial")
    void perfilPublico() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana, e.oferta().titulo("Activa").cupo(2));
        e.borrador(ana, e.oferta().titulo("Solo borrador"));
        PublicacionDto pausada = e.publicada(ana, e.oferta().titulo("Pausada"));
        app.publicacionUseCase.pausar(ana.id(), pausada.id());
        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);
        e.calificar(bruno, completada, 4);

        PerfilPublicoDto publico = app.perfilPublicoUseCase.verPerfilPublico(ana.id());

        assertEquals(ana.id(), publico.id());
        assertEquals("Ana", publico.nombreVisible());
        assertEquals(Set.of("java"), publico.habilidades());
        assertEquals(4.0, publico.reputacion(), 0.0001);
        assertEquals(1, publico.cantidadCalificaciones());
        assertEquals(1, publico.ultimasCalificaciones().size());
        assertEquals(4, publico.ultimasCalificaciones().get(0).puntaje());
        assertEquals(1, publico.publicacionesActivas().size(), "solo la PUBLICADA");
        assertEquals("Activa", publico.publicacionesActivas().get(0).titulo());
        assertEquals("Ana", publico.publicacionesActivas().get(0).autorNombre());
        assertEquals(4.0, publico.publicacionesActivas().get(0).autorReputacion(), 0.0001);
        assertEquals(1, publico.contratacionesCompletadas());
        assertEquals(List.of(new ContratacionCompletadaDto("Activa", completada.completadaEn())),
                publico.ultimasCompletadas(), "titulo y fecha, nada del cliente");
    }

    @Test
    @DisplayName("el historial resumido cuenta solo las contrataciones COMPLETADAS donde el perfil fue el proveedor")
    void historialSoloCompletadasComoProveedor() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto deAna = e.publicada(ana, e.oferta().titulo("De Ana").cupo(2));
        PublicacionDto deBruno = e.publicada(bruno, e.oferta().titulo("De Bruno"));
        e.completada(bruno, ana, deAna, 0);
        e.enCurso(bruno, ana, deAna, 1);   // todavia no se completo
        e.completada(ana, bruno, deBruno, 0); // Ana fue la clienta

        PerfilPublicoDto deAnaPublico = app.perfilPublicoUseCase.verPerfilPublico(ana.id());
        PerfilPublicoDto deBrunoPublico = app.perfilPublicoUseCase.verPerfilPublico(bruno.id());

        assertEquals(1, deAnaPublico.contratacionesCompletadas());
        assertEquals(List.of("De Ana"), titulos(deAnaPublico));
        assertEquals(1, deBrunoPublico.contratacionesCompletadas());
        assertEquals(List.of("De Bruno"), titulos(deBrunoPublico));
    }

    private static List<String> titulos(PerfilPublicoDto publico) {
        return publico.ultimasCompletadas().stream().map(ContratacionCompletadaDto::tituloPublicacion).toList();
    }

    @Test
    @DisplayName("el perfil publico limita calificaciones, publicaciones y completadas segun la configuracion y lista primero las mas recientes")
    void perfilPublicoRespetaLimites() {
        SkillSwapConfig config = new SkillSwapConfig(SkillCoins.de(100), 200, 10, 30, 2, 1, 2,
                PoliticaFlexible.UMBRALES_POR_DEFECTO, PoliticaEstricta.UMBRALES_POR_DEFECTO);
        Escenario otro = new Escenario(AplicacionEnMemoria.crear(config));
        PerfilDto ana = otro.profesional("u-ana", "Ana");
        PerfilDto bruno = otro.profesional("u-bruno", "Bruno");
        PublicacionDto principal = otro.publicada(ana, otro.oferta().cupo(3).precio("10").franjasEn(100L, 110L, 120L));
        otro.publicada(ana, otro.oferta().titulo("Segunda publicada"));
        int[] puntajes = {1, 3, 5};
        List<ContratacionDto> completadas = new ArrayList<>();
        for (int i = 0; i < puntajes.length; i++) {
            ContratacionDto c = otro.completada(bruno, ana, principal, i);
            completadas.add(c);
            otro.avanzarHoras(1); // cada calificacion (y cada completada) en un instante distinto
            otro.calificar(bruno, c, puntajes[i]);
        }

        PerfilPublicoDto publico = otro.app.perfilPublicoUseCase.verPerfilPublico(ana.id());

        assertEquals(3, publico.cantidadCalificaciones());
        assertEquals(3.0, publico.reputacion(), 0.0001);
        assertEquals(2, publico.ultimasCalificaciones().size(), "tope configurado");
        assertEquals(5, publico.ultimasCalificaciones().get(0).puntaje(), "la mas reciente primero");
        assertEquals(1, publico.publicacionesActivas().size(), "tope configurado");
        assertEquals(3, publico.contratacionesCompletadas(), "el total no tiene tope");
        assertEquals(List.of(completadas.get(2).completadaEn(), completadas.get(1).completadaEn()),
                publico.ultimasCompletadas().stream().map(ContratacionCompletadaDto::completadaEn).toList(),
                "tope configurado, la mas reciente primero");
    }

    @Test
    @DisplayName("un perfil nuevo tiene perfil publico vacio: sin calificaciones, publicaciones ni historial")
    void perfilPublicoVacio() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        PerfilPublicoDto publico = app.perfilPublicoUseCase.verPerfilPublico(ana.id());

        assertEquals(0.0, publico.reputacion(), 0.0001);
        assertTrue(publico.ultimasCalificaciones().isEmpty());
        assertTrue(publico.publicacionesActivas().isEmpty());
        assertEquals(0, publico.contratacionesCompletadas());
        assertTrue(publico.ultimasCompletadas().isEmpty());
    }

    @Test
    @DisplayName("el perfil publico de un perfil inexistente es RecursoNoEncontrado")
    void perfilInexistente() {
        e.profesional("u-ana", "Ana");

        assertThrows(RecursoNoEncontradoException.class, () -> app.perfilPublicoUseCase.verPerfilPublico(999L));
    }
}
