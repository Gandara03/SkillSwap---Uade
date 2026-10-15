package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.NO_ENCONTRADO;
import static com.uade.skillswap.application.exception.ErrorCode.REGLA_DE_NEGOCIO;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integracion CalificacionController -> CalificacionUseCase -> JPA/H2. Bruno contrato y completo una mentoria
 * de Ana; Cecilia es un tercero.
 */
@DisplayName("CalificacionController (JPA/H2): calificar, calificaciones recibidas, validacion y permisos")
class CalificacionControllerTest extends ControllerJpaTestBase {

    private PerfilDto ana;
    private PublicacionDto mentoria;

    private ICalificacionController calificaciones() {
        return app.getCalificacionController();
    }

    private ContratacionDto completadaEntreAnaYBruno() {
        ana = profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        mentoria = publicarMentoria("u-ana", 40, 2, 2);
        return contratarYCompletar("u-ana", "u-bruno", mentoria, 0);
    }

    private long recibidasPorAna() {
        return ok(calificaciones().recibidasDe(ana.id(), primeraPagina())).totalElementos();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("el cliente califica; recibidasDe (sin sesion) y misRecibidas lo muestran")
    void calificarYListar() {
        ContratacionDto completada = completadaEntreAnaYBruno();
        como("u-bruno");

        CalificacionDto calificacion =
                ok(calificaciones().calificar(new CalificarCommand(completada.id(), 5, "Excelente")));
        sinSesion();
        List<Integer> publicas = ok(calificaciones().recibidasDe(ana.id(), primeraPagina())).contenido().stream()
                .map(CalificacionDto::puntaje).toList();
        como("u-ana");
        List<Integer> mias = ok(calificaciones().misRecibidas(primeraPagina())).contenido().stream()
                .map(CalificacionDto::puntaje).toList();

        assertEquals(ana.id(), calificacion.destinatarioId());
        assertEquals(List.of(5), publicas);
        assertEquals(List.of(5), mias);
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("calificar sin datos, con puntaje fuera de 1 a 5 o comentario largo: VALIDACION, sin gastar el voto")
    void calificarInvalido() {
        ContratacionDto completada = completadaEntreAnaYBruno();
        como("u-bruno");

        falla(VALIDACION, calificaciones().calificar(null));
        falla(VALIDACION, calificaciones().calificar(new CalificarCommand(null, 5, "Bien")));
        falla(VALIDACION, calificaciones().calificar(new CalificarCommand(completada.id(), 0, "Mal")));
        falla(VALIDACION, calificaciones().calificar(new CalificarCommand(completada.id(), 6, "Genial")));
        falla(VALIDACION, calificaciones().calificar(new CalificarCommand(completada.id(), 5, texto(1001))));

        assertEquals(0L, recibidasPorAna());
        ok(calificaciones().calificar(new CalificarCommand(completada.id(), 4, "Muy buena")));
        assertEquals(1L, recibidasPorAna());
    }

    @Test
    @DisplayName("recibidasDe y misRecibidas sin perfil o sin pagina son VALIDACION")
    void listadosInvalidos() {
        completadaEntreAnaYBruno();
        como("u-ana");

        falla(VALIDACION, calificaciones().recibidasDe(null, primeraPagina()));
        falla(VALIDACION, calificaciones().recibidasDe(ana.id(), null));
        falla(VALIDACION, calificaciones().misRecibidas(null));
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("un tercero no puede calificar una contratacion ajena, ni nadie sin sesion")
    void terceroYSinSesion() {
        ContratacionDto completada = completadaEntreAnaYBruno();

        como("u-cecilia");
        falla(NO_AUTORIZADO, calificaciones().calificar(calificacion(completada.id(), 1)));
        sinSesion();
        falla(NO_AUTORIZADO, calificaciones().calificar(calificacion(completada.id(), 1)));

        assertEquals(0L, recibidasPorAna());
    }

    // ---------- otros codigos ----------

    @Test
    @DisplayName("calificar dos veces o antes de completar es REGLA_DE_NEGOCIO; lo inexistente, NO_ENCONTRADO")
    void reglasYNoEncontrados() {
        ContratacionDto completada = completadaEntreAnaYBruno();
        ContratacionDto pagada = contratarYPagar("u-ana", "u-bruno", mentoria, 1);
        como("u-bruno");

        ok(calificaciones().calificar(calificacion(completada.id(), 5)));
        falla(REGLA_DE_NEGOCIO, calificaciones().calificar(calificacion(completada.id(), 3)));
        falla(REGLA_DE_NEGOCIO, calificaciones().calificar(calificacion(pagada.id(), 5)));
        falla(NO_ENCONTRADO, calificaciones().calificar(calificacion(999L, 5)));
        falla(NO_ENCONTRADO, calificaciones().recibidasDe(999L, primeraPagina()));
        assertEquals(1L, recibidasPorAna());
    }
}
