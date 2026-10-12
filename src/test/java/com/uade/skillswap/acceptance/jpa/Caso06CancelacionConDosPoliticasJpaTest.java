package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Caso de aceptacion 6 (JPA/H2): cancelar y calcular el reintegro con dos politicas diferentes.
 * Misma anticipacion (60 h) y mismo precio (80 SC): FLEXIBLE reintegra el 100 % (24 h o mas) y ESTRICTA el 50 %
 * (48 h o mas pero menos de 72 h). Lo no reintegrado compensa al proveedor. El reloj es el real: el margen de
 * horas a cada lado de los umbrales absorbe el tiempo que tarda el test.
 */
@DisplayName("Caso 6 (JPA/H2): cancelar y calcular el reintegro con dos politicas diferentes")
class Caso06CancelacionConDosPoliticasJpaTest extends AceptacionJpaBase {

    private static final Duration ANTICIPACION = Duration.ofHours(60);

    private PublicacionDto publicarCon(TipoPoliticaCancelacion politica) {
        return publicar("u-ana", mentoria("Mentoria " + politica, "programacion", 80, 1, politica, Set.of("java"),
                Set.of("java"), List.of(franja(ANTICIPACION))));
    }

    @Test
    @DisplayName("Caso 6: con 60 h de anticipacion, FLEXIBLE reintegra 80 SC y ESTRICTA solo 40 SC")
    void caso06_cancelarConPoliticaFlexibleYEstrictaDaReintegrosDistintos() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        PublicacionDto flexible = publicarCon(TipoPoliticaCancelacion.FLEXIBLE);
        PublicacionDto estricta = publicarCon(TipoPoliticaCancelacion.ESTRICTA);
        ContratacionDto deBruno = contratarYPagar("u-ana", "u-bruno", flexible, 0);
        ContratacionDto deCecilia = contratarYPagar("u-ana", "u-cecilia", estricta, 0);
        assertEquals(0, sc(20).compareTo(saldo("u-bruno")));
        assertEquals(0, sc(20).compareTo(saldo("u-cecilia")));

        como("u-bruno");
        ResultadoCancelacionDto conFlexible = ok(app.getContratacionController().cancelar(deBruno.id(), "No llego"));
        como("u-cecilia");
        ResultadoCancelacionDto conEstricta = ok(app.getContratacionController().cancelar(deCecilia.id(), "No llego"));

        assertEquals("FLEXIBLE", conFlexible.politica());
        assertEquals("ESTRICTA", conEstricta.politica());
        assertEquals(0, sc(80).compareTo(conFlexible.reintegroCliente()));
        assertEquals(0, sc(0).compareTo(conFlexible.compensacionProveedor()));
        assertEquals(0, sc(40).compareTo(conEstricta.reintegroCliente()));
        assertEquals(0, sc(40).compareTo(conEstricta.compensacionProveedor()));
        assertNotEquals(0, conFlexible.reintegroCliente().compareTo(conEstricta.reintegroCliente()));
        assertEquals("CANCELADA", conFlexible.contratacion().estado());
        assertEquals("CANCELADA", conEstricta.contratacion().estado());

        // saldos persistidos
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")));
        assertEquals(0, sc(60).compareTo(saldo("u-cecilia")));
        assertEquals(0, sc(140).compareTo(saldo("u-ana")));
        assertEquals("CANCELADA", contratacionVista("u-cecilia", deCecilia.id()).estado());
    }

    @Test
    @DisplayName("Caso 6: al cancelar se libera la franja y el cupo de la publicacion")
    void caso06_cancelarLiberaFranjaYCupo() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto estricta = publicarCon(TipoPoliticaCancelacion.ESTRICTA);
        ContratacionDto contratacion = contratarYPagar("u-ana", "u-bruno", estricta, 0);
        assertEquals(0, publicacionVista("u-ana", estricta.id()).cupoDisponible());

        como("u-bruno");
        ok(app.getContratacionController().cancelar(contratacion.id(), "Imprevisto"));

        PublicacionDto tras = publicacionVista("u-ana", estricta.id());
        assertEquals(1, tras.cupoDisponible());
        assertFalse(tras.franjas().get(0).ocupada());
    }
}
