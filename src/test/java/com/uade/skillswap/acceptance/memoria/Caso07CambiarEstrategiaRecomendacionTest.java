package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Caso de aceptacion 7: cambiar la estrategia de recomendacion sin modificar el caso de uso consumidor.
 * Es el MISMO RecomendacionUseCase; solo cambia el parametro TipoEstrategiaRecomendacion (Strategy + Simple Factory).
 */
@DisplayName("Caso 7 (memoria): cambiar la estrategia de recomendacion sin modificar el caso de uso")
class Caso07CambiarEstrategiaRecomendacionTest {

    /**
     * Escenario: Ana publica "Java a fondo" (afinidad alta con los intereses de Dario, sin contrataciones) y
     * "Charla de backend" (afinidad baja, pero ya contratada por Cecilia, o sea mas popular).
     */
    private record Escenario(EscenarioMemoria e, PerfilDto dario, PublicacionDto javaAFondo, PublicacionDto backend) { }

    private Escenario armar() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PerfilDto dario = e.registrar("u-dario", "Dario", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA,
                Set.of(), Set.of("java"));
        // afinidad con el interes "java": habilidad (2) + etiqueta (1) + categoria (1) = 4 puntos
        PublicacionDto javaAFondo = e.publicar(ana.id(), e.mentoria("Java a fondo", "java", 50, 5,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), e.franjasDiarias(2)));
        // afinidad con el interes "java": solo la etiqueta (1 punto)
        PublicacionDto backend = e.publicar(ana.id(), e.mentoria("Charla de backend", "arquitectura", 50, 5,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("diseno"), e.franjasDiarias(2)));
        // Cecilia contrata (y se le acepta) la charla: cupo ocupado = 1 -> mas popular
        ContratacionDto contratacion = e.solicitar(cecilia.id(), backend, 0);
        e.app.contratacionUseCase.aceptar(ana.id(), contratacion.id());
        return new Escenario(e, dario, javaAFondo, backend);
    }

    private static List<Long> ids(List<PublicacionResumenDto> resumenes) {
        return resumenes.stream().map(PublicacionResumenDto::id).toList();
    }

    @Test
    @DisplayName("Caso 7: AFINIDAD y POPULARIDAD ordenan distinto con el mismo caso de uso")
    void caso07_cambiarLaEstrategiaCambiaElOrdenSinTocarElCasoDeUso() {
        Escenario s = armar();
        RecomendacionUseCase recomendador = s.e().app.recomendacionUseCase; // un unico consumidor

        List<Long> porAfinidad = ids(recomendador.recomendar(s.dario().id(), TipoEstrategiaRecomendacion.AFINIDAD, s.e().primeraPagina()).contenido());
        List<Long> porPopularidad = ids(recomendador.recomendar(s.dario().id(), TipoEstrategiaRecomendacion.POPULARIDAD, s.e().primeraPagina()).contenido());

        assertEquals(List.of(s.javaAFondo().id(), s.backend().id()), porAfinidad, "primero la que mas coincide con sus intereses");
        assertEquals(List.of(s.backend().id(), s.javaAFondo().id()), porPopularidad, "primero la mas contratada");
        assertNotEquals(porAfinidad, porPopularidad);
    }

    @Test
    @DisplayName("Caso 7: volver a la estrategia anterior reproduce el mismo orden (no hay estado oculto)")
    void caso07_alternarEstrategiasEsRepetible() {
        Escenario s = armar();
        RecomendacionUseCase recomendador = s.e().app.recomendacionUseCase;

        List<Long> primera = ids(recomendador.recomendar(s.dario().id(), TipoEstrategiaRecomendacion.AFINIDAD, s.e().primeraPagina()).contenido());
        recomendador.recomendar(s.dario().id(), TipoEstrategiaRecomendacion.POPULARIDAD, s.e().primeraPagina()).contenido();
        List<Long> segunda = ids(recomendador.recomendar(s.dario().id(), TipoEstrategiaRecomendacion.AFINIDAD, s.e().primeraPagina()).contenido());

        assertEquals(primera, segunda);
    }

    @Test
    @DisplayName("Caso 7: el resumen recomendado trae autor, precio final y cupo para mostrar sin consultas extra")
    void caso07_elResumenTraeLosDatosParaLaVista() {
        Escenario s = armar();

        PublicacionResumenDto primera = s.e().app.recomendacionUseCase
                .recomendar(s.dario().id(), TipoEstrategiaRecomendacion.POPULARIDAD, s.e().primeraPagina()).contenido().get(0);

        assertEquals("Charla de backend", primera.titulo());
        assertEquals("Ana", primera.autorNombre());
        assertEquals(0, primera.precioFinal().compareTo(java.math.BigDecimal.valueOf(50)));
        assertEquals(4, primera.cupoDisponible(), "cupo 5 con una reserva");
    }
}
