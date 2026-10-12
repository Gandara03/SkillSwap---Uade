package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.presentation.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 10 (JPA/H2): completar una contratacion, calificarla una sola vez y recalcular la reputacion.
 * La reputacion es el promedio de las calificaciones recibidas, recalculado y persistido en cada calificacion.
 * La segunda calificacion del mismo autor se rechaza (regla de negocio + unico calificacion(contratacion, autor)).
 */
@DisplayName("Caso 10 (JPA/H2): completar, calificar una sola vez y recalcular reputacion")
class Caso10CompletarCalificarYReputacionJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 10: completar acredita al proveedor, la calificacion se acepta una vez y la reputacion es el promedio")
    void caso10_completarCalificarUnaSolaVezYRecalcularReputacion() {
        PerfilDto ana = profesional("u-ana", "Ana");
        PerfilDto bruno = profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 2, 2);

        ContratacionDto deBruno = contratarYCompletar("u-ana", "u-bruno", mentoria, 0);
        ContratacionDto deCecilia = contratarYCompletar("u-ana", "u-cecilia", mentoria, 1);
        assertEquals("COMPLETADA", deBruno.estado());
        assertEquals(0, sc(180).compareTo(saldo("u-ana")), "completar libera lo retenido: 100 + 40 + 40");
        assertEquals(0.0, perfilDe("u-ana").reputacion(), 0.0001, "sin calificaciones");

        // Bruno califica con 5: reputacion 5.0
        como("u-bruno");
        CalificacionDto primera = ok(app.getCalificacionController().calificar(calificacion(deBruno.id(), 5)));
        assertEquals(ana.id(), primera.destinatarioId());
        assertEquals(bruno.id(), primera.autorId());
        assertEquals(5.0, perfilDe("u-ana").reputacion(), 0.0001);

        // el mismo autor no puede calificar dos veces la misma contratacion; la reputacion no se mueve
        como("u-bruno");
        Resultado<CalificacionDto> segunda = app.getCalificacionController().calificar(calificacion(deBruno.id(), 1));
        assertTrue(segunda.esError());
        assertEquals(ErrorCode.REGLA_DE_NEGOCIO, segunda.codigo());
        PerfilDto tras = perfilDe("u-ana");
        assertEquals(5.0, tras.reputacion(), 0.0001);
        assertEquals(1, tras.cantidadCalificaciones());

        // Cecilia califica con 3: la reputacion es el promedio de las recibidas (5 y 3) = 4.0
        como("u-cecilia");
        ok(app.getCalificacionController().calificar(calificacion(deCecilia.id(), 3)));
        PerfilDto promedio = perfilDe("u-ana");
        assertEquals(4.0, promedio.reputacion(), 0.0001);
        assertEquals(2, promedio.cantidadCalificaciones());
        como("u-ana");
        assertEquals(2L, ok(app.getCalificacionController().misRecibidas(primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("Caso 10: no se puede calificar una contratacion que todavia no esta completada")
    void caso10_noSeCalificaAntesDeCompletar() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);
        ContratacionDto pagada = contratarYPagar("u-ana", "u-bruno", mentoria, 0);

        como("u-bruno");
        Resultado<CalificacionDto> intento = app.getCalificacionController().calificar(calificacion(pagada.id(), 5));

        assertTrue(intento.esError());
        assertEquals(0.0, perfilDe("u-ana").reputacion(), 0.0001);
    }
}
