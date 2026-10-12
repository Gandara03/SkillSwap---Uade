package com.uade.skillswap.application.politica;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;

import com.uade.skillswap.domain.model.vo.SkillCoins;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Politicas de cancelacion (Strategy + Simple Factory)")
class PoliticaCancelacionTest {

    private static final SkillCoins MONTO = SkillCoins.de(100);

    // ----- flexible: 100 % >= 24h, 50 % >= 2h, 0 % debajo -----

    @Test
    @DisplayName("FLEXIBLE: con 24 horas o mas reintegra el 100 %")
    void flexibleTotal() {
        PoliticaCancelacion p = new PoliticaFlexible();
        assertEquals(MONTO, p.calcularReintegro(MONTO, Duration.ofHours(24)));
        assertEquals(MONTO, p.calcularReintegro(MONTO, Duration.ofHours(100)));
    }

    @Test
    @DisplayName("FLEXIBLE: entre 2 y menos de 24 horas reintegra el 50 %")
    void flexibleParcial() {
        PoliticaCancelacion p = new PoliticaFlexible();
        assertEquals(SkillCoins.de(50), p.calcularReintegro(MONTO, Duration.ofHours(23)));
        assertEquals(SkillCoins.de(50), p.calcularReintegro(MONTO, Duration.ofHours(23).plusMinutes(59)));
        assertEquals(SkillCoins.de(50), p.calcularReintegro(MONTO, Duration.ofHours(2)));
    }

    @Test
    @DisplayName("FLEXIBLE: con menos de 2 horas no reintegra")
    void flexibleNada() {
        PoliticaCancelacion p = new PoliticaFlexible();
        assertEquals(SkillCoins.CERO, p.calcularReintegro(MONTO, Duration.ofHours(1).plusMinutes(59)));
        assertEquals(SkillCoins.CERO, p.calcularReintegro(MONTO, Duration.ZERO));
    }

    // ----- estricta: 100 % >= 72h, 50 % >= 48h, 0 % debajo -----

    @Test
    @DisplayName("ESTRICTA: con 72 horas o mas reintegra el 100 %")
    void estrictaTotal() {
        PoliticaCancelacion p = new PoliticaEstricta();
        assertEquals(MONTO, p.calcularReintegro(MONTO, Duration.ofHours(72)));
        assertEquals(MONTO, p.calcularReintegro(MONTO, Duration.ofDays(30)));
    }

    @Test
    @DisplayName("ESTRICTA: entre 48 y menos de 72 horas reintegra el 50 %")
    void estrictaParcial() {
        PoliticaCancelacion p = new PoliticaEstricta();
        assertEquals(SkillCoins.de(50), p.calcularReintegro(MONTO, Duration.ofHours(71)));
        assertEquals(SkillCoins.de(50), p.calcularReintegro(MONTO, Duration.ofHours(48)));
    }

    @Test
    @DisplayName("ESTRICTA: con menos de 48 horas no reintegra (aun donde FLEXIBLE si lo hace)")
    void estrictaNada() {
        PoliticaCancelacion p = new PoliticaEstricta();
        assertEquals(SkillCoins.CERO, p.calcularReintegro(MONTO, Duration.ofHours(47)));
        assertEquals(SkillCoins.CERO, p.calcularReintegro(MONTO, Duration.ofHours(24)));
    }

    @Test
    @DisplayName("los dos tipos difieren para la misma anticipacion")
    void tiposDifieren() {
        Duration veinticincoHoras = Duration.ofHours(25);
        assertEquals(MONTO, new PoliticaFlexible().calcularReintegro(MONTO, veinticincoHoras));
        assertEquals(SkillCoins.CERO, new PoliticaEstricta().calcularReintegro(MONTO, veinticincoHoras));
    }

    @Test
    @DisplayName("una anticipacion negativa (la franja ya empezo) nunca reintegra")
    void anticipacionNegativa() {
        assertEquals(SkillCoins.CERO, new PoliticaFlexible().calcularReintegro(MONTO, Duration.ofHours(-1)));
        assertEquals(SkillCoins.CERO, new PoliticaEstricta().calcularReintegro(MONTO, Duration.ofMinutes(-1)));
    }

    @Test
    @DisplayName("monto cero o nulo reintegra cero")
    void montoCeroONulo() {
        assertEquals(SkillCoins.CERO, new PoliticaFlexible().calcularReintegro(SkillCoins.CERO, Duration.ofHours(100)));
        assertEquals(SkillCoins.CERO, new PoliticaFlexible().calcularReintegro(null, Duration.ofHours(100)));
    }

    @Test
    @DisplayName("el reintegro nunca supera el monto pagado ni es negativo")
    void acotado() {
        long[] horas = {-5, 0, 1, 2, 23, 24, 47, 48, 71, 72, 500};
        for (PoliticaCancelacion p : new PoliticaCancelacion[]{new PoliticaFlexible(), new PoliticaEstricta()}) {
            for (long h : horas) {
                SkillCoins r = p.calcularReintegro(MONTO, Duration.ofHours(h));
                assertFalse(r.esNegativo());
                assertFalse(r.esMayorQue(MONTO));
            }
        }
    }

    @Test
    @DisplayName("el porcentaje parcial se aplica con decimales")
    void parcialConDecimales() {
        assertEquals(SkillCoins.de("12.50"), new PoliticaFlexible().calcularReintegro(SkillCoins.de(25), Duration.ofHours(5)));
    }

    // ----- umbrales configurables -----

    @Test
    @DisplayName("los umbrales son configurables")
    void umbralesCustom() {
        PoliticaCancelacion p = new PoliticaFlexible(new UmbralesCancelacion(10, 5, 30));
        assertEquals(MONTO, p.calcularReintegro(MONTO, Duration.ofHours(10)));
        assertEquals(SkillCoins.de(30), p.calcularReintegro(MONTO, Duration.ofHours(7)));
        assertEquals(SkillCoins.CERO, p.calcularReintegro(MONTO, Duration.ofHours(4)));
    }

    @Test
    @DisplayName("UmbralesCancelacion valida coherencia: total >= parcial y porcentaje 0..100")
    void umbralesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new UmbralesCancelacion(2, 24, 50));
        assertThrows(IllegalArgumentException.class, () -> new UmbralesCancelacion(24, 2, -1));
        assertThrows(IllegalArgumentException.class, () -> new UmbralesCancelacion(24, 2, 101));
        assertDoesNotThrow(() -> new UmbralesCancelacion(24, 24, 100));
    }

    @Test
    @DisplayName("valores por defecto: flexible 24/2/50 y estricta 72/48/50")
    void valoresPorDefecto() {
        assertEquals(24, new PoliticaFlexible().getUmbrales().horasReintegroTotal());
        assertEquals(2, new PoliticaFlexible().getUmbrales().horasReintegroParcial());
        assertEquals(72, new PoliticaEstricta().getUmbrales().horasReintegroTotal());
        assertEquals(48, new PoliticaEstricta().getUmbrales().horasReintegroParcial());
        ConfiguracionCancelacion c = ConfiguracionCancelacion.porDefecto();
        assertEquals(50, c.flexible().porcentajeParcial());
        assertEquals(50, c.estricta().porcentajeParcial());
    }

    // ----- fabrica -----

    @Test
    @DisplayName("la fabrica crea la politica segun el tipo; nulo equivale a FLEXIBLE")
    void fabrica() {
        PoliticaCancelacionFactory f = new PoliticaCancelacionFactory();
        assertInstanceOf(PoliticaFlexible.class, f.crear(TipoPoliticaCancelacion.FLEXIBLE));
        assertInstanceOf(PoliticaEstricta.class, f.crear(TipoPoliticaCancelacion.ESTRICTA));
        assertInstanceOf(PoliticaFlexible.class, f.crear((TipoPoliticaCancelacion) null));
        assertEquals(TipoPoliticaCancelacion.ESTRICTA, f.crear(TipoPoliticaCancelacion.ESTRICTA).getTipo());
        assertEquals(TipoPoliticaCancelacion.FLEXIBLE, f.crear(TipoPoliticaCancelacion.FLEXIBLE).getTipo());
    }

    @Test
    @DisplayName("la fabrica tambien acepta el nombre como texto (sin importar mayusculas ni espacios)")
    void fabricaPorTexto() {
        PoliticaCancelacionFactory f = new PoliticaCancelacionFactory();
        assertInstanceOf(PoliticaEstricta.class, f.crear(" estricta "));
        assertInstanceOf(PoliticaFlexible.class, f.crear("FLEXIBLE"));
        assertThrows(IllegalArgumentException.class, () -> f.crear("moderada"));
    }

    @Test
    @DisplayName("la fabrica respeta una configuracion propia de umbrales")
    void fabricaConConfiguracion() {
        ConfiguracionCancelacion cfg = new ConfiguracionCancelacion(new UmbralesCancelacion(1, 0, 10),
                new UmbralesCancelacion(100, 90, 90));
        PoliticaCancelacionFactory f = new PoliticaCancelacionFactory(cfg);
        assertEquals(MONTO, f.crear(TipoPoliticaCancelacion.FLEXIBLE).calcularReintegro(MONTO, Duration.ofHours(1)));
        assertEquals(SkillCoins.de(90), f.crear(TipoPoliticaCancelacion.ESTRICTA).calcularReintegro(MONTO, Duration.ofHours(95)));
    }
}
