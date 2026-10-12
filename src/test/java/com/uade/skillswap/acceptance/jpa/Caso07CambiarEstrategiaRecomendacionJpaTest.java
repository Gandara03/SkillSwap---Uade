package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
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
 * Caso de aceptacion 7 (JPA/H2): cambiar la estrategia de recomendacion sin modificar el caso de uso consumidor.
 * El controller y el caso de uso son los mismos; solo cambia el TipoEstrategiaRecomendacion (Strategy).
 *
 * <p>Escenario: Ana publica "Java a fondo" (afinidad alta con los intereses de Bruno, sin contrataciones) y
 * "Charla de backend" (afinidad baja, pero ya contratada por Cecilia, o sea mas popular). Bruno es quien recibe
 * las recomendaciones.
 */
@DisplayName("Caso 7 (JPA/H2): cambiar la estrategia de recomendacion sin modificar el caso de uso")
class Caso07CambiarEstrategiaRecomendacionJpaTest extends AceptacionJpaBase {

    private PublicacionDto javaAFondo;
    private PublicacionDto backend;

    private void armarEscenario() {
        profesional("u-ana", "Ana");
        profesional("u-cecilia", "Cecilia");
        registrar("u-bruno", "Bruno", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of("java"));
        // afinidad con "java": habilidad (2) + etiqueta (1) + categoria (1)
        javaAFondo = publicar("u-ana", mentoria("Java a fondo", "java", 50, 5, TipoPoliticaCancelacion.FLEXIBLE,
                Set.of("java"), Set.of("java"), franjasDiarias(2)));
        // afinidad con "java": solo la etiqueta (1)
        backend = publicar("u-ana", mentoria("Charla de backend", "arquitectura", 50, 5,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("diseno"), franjasDiarias(2)));
        // Cecilia contrata (y Ana acepta) la charla: cupo ocupado = 1 -> mas popular
        ContratacionDto contratacion = solicitar("u-cecilia", backend, 0);
        aceptar("u-ana", contratacion.id());
    }

    private List<Long> recomendar(TipoEstrategiaRecomendacion estrategia) {
        como("u-bruno");
        return ok(app.getRecomendacionController().recomendar(estrategia)).stream()
                .map(PublicacionResumenDto::id).toList();
    }

    @Test
    @DisplayName("Caso 7: AFINIDAD y POPULARIDAD ordenan distinto con el mismo caso de uso")
    void caso07_cambiarLaEstrategiaCambiaElOrdenSinTocarElCasoDeUso() {
        armarEscenario();

        List<Long> porAfinidad = recomendar(TipoEstrategiaRecomendacion.AFINIDAD);
        List<Long> porPopularidad = recomendar(TipoEstrategiaRecomendacion.POPULARIDAD);

        assertEquals(List.of(javaAFondo.id(), backend.id()), porAfinidad, "primero la que mas coincide con sus intereses");
        assertEquals(List.of(backend.id(), javaAFondo.id()), porPopularidad, "primero la mas contratada");
        assertNotEquals(porAfinidad, porPopularidad);
    }

    @Test
    @DisplayName("Caso 7: volver a la estrategia anterior reproduce el mismo orden (no hay estado oculto)")
    void caso07_alternarEstrategiasEsRepetible() {
        armarEscenario();

        List<Long> primera = recomendar(TipoEstrategiaRecomendacion.AFINIDAD);
        recomendar(TipoEstrategiaRecomendacion.POPULARIDAD);
        List<Long> segunda = recomendar(TipoEstrategiaRecomendacion.AFINIDAD);

        assertEquals(primera, segunda);
    }
}
