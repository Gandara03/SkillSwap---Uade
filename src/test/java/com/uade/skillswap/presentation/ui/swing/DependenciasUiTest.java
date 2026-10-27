package com.uade.skillswap.presentation.ui.swing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La pantalla solo conoce su contrato (UiControllers y Models): los fuentes de ui.swing no pueden nombrar ninguna otra
 * clase del proyecto (dominio, aplicacion, infraestructura, controllers, adaptador...). Se revisa el texto de los
 * archivos, asi que cubre imports, imports static y nombres completos. Maven corre los tests desde la carpeta del
 * proyecto, por eso las rutas son relativas a ella.
 */
@DisplayName("La pantalla Swing no depende de las capas internas")
class DependenciasUiTest {

    private static final String PROYECTO = "com.uade.skillswap.";
    private static final String PANTALLA = "com.uade.skillswap.presentation.ui.swing";
    private static final Path FUENTES = Path.of("src/main/java/com/uade/skillswap/presentation");

    @Test
    @DisplayName("ningun fuente de ui.swing nombra clases de otro paquete del proyecto")
    void pantallaSinDependenciasInternas() throws IOException {
        List<String> usos = usosDeOtrosPaquetes(FUENTES.resolve("ui/swing"));

        assertTrue(usos.isEmpty(), () -> "La pantalla usa clases internas:\n" + String.join("\n", usos));
    }

    @Test
    @DisplayName("el control detecta usos reales: el adaptador si nombra controllers y DTO")
    void elControlDetectaUsos() throws IOException {
        assertFalse(usosDeOtrosPaquetes(FUENTES.resolve("adaptador")).isEmpty());
    }

    /** Cada linea que, quitando las menciones al propio paquete de la pantalla, sigue nombrando el proyecto. */
    private static List<String> usosDeOtrosPaquetes(Path carpeta) throws IOException {
        List<Path> fuentes;
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            fuentes = archivos.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertFalse(fuentes.isEmpty(), () -> "No hay fuentes en " + carpeta.toAbsolutePath());

        List<String> usos = new ArrayList<>();
        for (Path fuente : fuentes) {
            List<String> lineas = Files.readAllLines(fuente);
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).replace(PANTALLA, "").contains(PROYECTO)) {
                    usos.add(fuente.getFileName() + ":" + (i + 1) + "  " + lineas.get(i).trim());
                }
            }
        }
        return usos;
    }
}
