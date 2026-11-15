package com.uade.skillswap.bootstrap;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Corre la demo de consola completa sobre H2, con el mismo armado que {@code Main} usa con {@code --h2}. */
class DemoFlujoPrincipalTest {

    @Test
    @DisplayName("la demo sobre H2 recorre todos los pasos y termina sin error")
    void demoCompletaSobreH2() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(salida, true, StandardCharsets.UTF_8);
        IdentidadMockAdapter identidad = IdentidadMockAdapter.conUsuariosDemo();
        Composicion app = Composicion.crear(BaseDeDatos.iniciarH2(), identidad, SkillSwapConfig.porDefecto());
        try {
            DatosDemo.Semilla semilla = new DatosDemo(app, identidad).sembrar();
            boolean ok = new DemoFlujoPrincipal(app, identidad, semilla, out).ejecutar();

            String texto = salida.toString(StandardCharsets.UTF_8);
            assertTrue(ok, texto);
            assertTrue(texto.contains("Demo finalizada correctamente."), texto);
            assertFalse(texto.contains("Demo interrumpida"), texto);
        } finally {
            app.cerrar();
        }
    }
}
