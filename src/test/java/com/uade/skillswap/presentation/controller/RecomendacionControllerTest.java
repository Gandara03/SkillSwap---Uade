package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integracion RecomendacionController -> RecomendacionUseCase (Strategy) -> JPA/H2, con el ranking paginado. */
@DisplayName("RecomendacionController (JPA/H2): ranking paginado, estrategia por defecto y permisos")
class RecomendacionControllerTest extends ControllerJpaTestBase {

    private final List<Long> deAna = new ArrayList<>();

    private IRecomendacionController recomendaciones() {
        return app.getRecomendacionController();
    }

    /** Ana publica tres mentorias y deja un borrador; Bruno (le interesa "java") publica una propia. */
    private void escenario() {
        profesional("u-ana", "Ana");
        registrar("u-bruno", "Bruno", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of("java"));
        for (String titulo : List.of("Java a fondo", "Patrones en Java", "Charla de backend")) {
            deAna.add(publicar("u-ana", mentoria(titulo, "programacion", 40, 2, TipoPoliticaCancelacion.FLEXIBLE,
                    Set.of("java"), Set.of("java"), franjasDiarias(1))).id());
        }
        como("u-ana");
        ok(app.getPublicacionController().crearBorrador(mentoria("Borrador de Java", "programacion", 40, 2,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), franjasDiarias(1))));
        publicar("u-bruno", mentoria("Mi propia mentoria", "programacion", 40, 2, TipoPoliticaCancelacion.FLEXIBLE,
                Set.of("java"), Set.of("java"), franjasDiarias(1)));
    }

    private static List<Long> ids(Pagina<PublicacionResumenDto> pagina) {
        return pagina.contenido().stream().map(PublicacionResumenDto::id).toList();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("el ranking se pagina: entre las dos paginas estan las publicadas de otros, sin repetir")
    void rankingPaginado() {
        escenario();
        como("u-bruno");

        Pagina<PublicacionResumenDto> primera = ok(recomendaciones()
                .recomendar(TipoEstrategiaRecomendacion.POPULARIDAD, PaginaRequest.de(0, 2)));
        Pagina<PublicacionResumenDto> segunda = ok(recomendaciones()
                .recomendar(TipoEstrategiaRecomendacion.POPULARIDAD, PaginaRequest.de(1, 2)));

        assertEquals(3L, primera.totalElementos(), "ni la propia de Bruno ni el borrador de Ana");
        assertEquals(2, primera.contenido().size());
        assertTrue(primera.tieneSiguiente());
        assertEquals(1, segunda.contenido().size());
        assertFalse(segunda.tieneSiguiente());
        List<Long> todas = new ArrayList<>(ids(primera));
        todas.addAll(ids(segunda));
        assertEquals(Set.copyOf(deAna), Set.copyOf(todas));
        assertEquals(3, todas.size());
    }

    @Test
    @DisplayName("sin estrategia se usa AFINIDAD, la que elige la factory por defecto")
    void estrategiaPorDefecto() {
        escenario();
        como("u-bruno");

        assertEquals(ids(ok(recomendaciones().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, primeraPagina()))),
                ids(ok(recomendaciones().recomendar(null, primeraPagina()))));
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("recomendar sin pagina es VALIDACION")
    void sinPagina() {
        escenario();
        como("u-bruno");

        falla(VALIDACION, recomendaciones().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, null));
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("sin sesion, sin perfil o con el perfil dado de baja no hay recomendaciones: NO_AUTORIZADO")
    void sinActorValido() {
        escenario();

        sinSesion();
        falla(NO_AUTORIZADO, recomendaciones().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, primeraPagina()));
        como("u-cecilia");
        falla(NO_AUTORIZADO, recomendaciones().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, primeraPagina()));
        como("u-bruno");
        ok(app.getPerfilController().darDeBaja());
        falla(NO_AUTORIZADO, recomendaciones().recomendar(TipoEstrategiaRecomendacion.AFINIDAD, primeraPagina()));
    }
}
