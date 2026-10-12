package com.uade.skillswap;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.bootstrap.DatosDemo;
import com.uade.skillswap.bootstrap.DemoFlujoPrincipal;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Punto de entrada. Opciones:
 * {@code --h2} (base en memoria, por defecto si no hay SKILLSWAP_DB_URL), {@code --mysql} (o definir SKILLSWAP_DB_URL),
 * {@code --sin-demo} (solo carga los datos de ejemplo) y {@code --menu} (abre el menu de consola al terminar).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        List<String> opciones = List.of(args);
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        System.setOut(out);

        boolean usarMySql = opciones.contains("--mysql")
                || (!opciones.contains("--h2") && System.getenv("SKILLSWAP_DB_URL") != null);
        boolean conDemo = !opciones.contains("--sin-demo");
        boolean conMenu = opciones.contains("--menu") || !conDemo;

        out.println("SkillSwap - base de datos: " + (usarMySql ? "MySQL" : "H2 en memoria"));
        JpaContexto contexto = usarMySql ? BaseDeDatos.iniciarMySql() : BaseDeDatos.iniciarH2();
        Composicion app = null;
        int codigoSalida = 0;
        try {
            IdentidadMockAdapter identidad = IdentidadMockAdapter.conUsuariosDemo();
            app = Composicion.crear(contexto, identidad, SkillSwapConfig.porDefecto());

            out.println("Cargando datos de demostración (idempotente)...");
            DatosDemo.Semilla semilla = new DatosDemo(app, identidad).sembrar();
            out.println("Listo: perfiles u-ana, u-bruno, u-cecilia y u-techcorp con sus publicaciones.");

            if (conDemo && !new DemoFlujoPrincipal(app, identidad, semilla, out).ejecutar()) {
                codigoSalida = 1;
            }
            if (conMenu) {
                app.crearMenu(identidad, System.in, out).ejecutar();
            }
        } catch (RuntimeException e) {
            out.println("No se pudo ejecutar SkillSwap: " + e.getMessage());
            codigoSalida = 1;
        } finally {
            if (app != null) {
                app.cerrar();
            } else {
                contexto.cerrar();
            }
        }
        if (codigoSalida != 0) {
            System.exit(codigoSalida);
        }
    }
}
