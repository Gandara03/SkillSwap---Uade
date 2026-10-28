package com.uade.skillswap;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.bootstrap.DatosDemo;
import com.uade.skillswap.bootstrap.DemoFlujoPrincipal;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.presentation.ui.swing.SkillSwapFrame;
import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;

import javax.swing.SwingUtilities;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Punto de entrada. Opciones:
 * {@code --h2} (base en memoria, por defecto si no hay SKILLSWAP_DB_URL), {@code --mysql} (o definir SKILLSWAP_DB_URL),
 * {@code --sin-demo} (solo carga los datos de ejemplo), {@code --menu} (abre el menu de consola al terminar) y
 * {@code --ui} (solo carga los datos de ejemplo y abre la pantalla Swing; tiene prioridad sobre {@code --menu}).
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
        boolean conUi = opciones.contains("--ui");
        boolean conDemo = !opciones.contains("--sin-demo") && !conUi;
        boolean conMenu = (opciones.contains("--menu") || opciones.contains("--sin-demo")) && !conUi;

        out.println("SkillSwap - base de datos: " + (usarMySql ? "MySQL" : "H2 en memoria"));
        JpaContexto contexto = null;
        Composicion app = null;
        int codigoSalida = 0;
        boolean ventanaAbierta = false;
        try {
            // Dentro del try: si falla el arranque (credenciales, servidor caido, migracion) se informa el error real
            contexto = usarMySql ? BaseDeDatos.iniciarMySql() : BaseDeDatos.iniciarH2();
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
            if (conUi) {
                abrirVentana(app, identidad, out);
                ventanaAbierta = true;
            }
        } catch (RuntimeException e) {
            out.println("No se pudo ejecutar SkillSwap: " + e.getMessage());
            codigoSalida = 1;
        } finally {
            // Con la pantalla abierta, la base se cierra cuando se cierra la ventana
            if (!ventanaAbierta) {
                cerrarBase(app, contexto);
            }
        }
        if (codigoSalida != 0) {
            System.exit(codigoSalida);
        }
    }

    /** Cierra la base a traves de la composicion si llego a armarse, o directamente si el arranque fallo antes. */
    private static void cerrarBase(Composicion app, JpaContexto contexto) {
        if (app != null) {
            app.cerrar();
        } else if (contexto != null) {
            contexto.cerrar();
        }
    }

    /**
     * Arma la ventana en el hilo de eventos de Swing. Desde ahi la base queda a cargo de la ventana: se cierra al
     * cerrarla, o enseguida si la pantalla no llega a abrirse.
     */
    private static void abrirVentana(Composicion app, IdentidadMockAdapter identidad, PrintStream out) {
        UiControllers controllers = app.crearControllersUi(identidad);
        List<String> usuarios = List.of(DatosDemo.ANA, DatosDemo.BRUNO, DatosDemo.CECILIA, DatosDemo.TECHCORP);
        out.println("Abriendo la pantalla (con --ui no corren la demo ni el menú de consola).");
        out.println("Al cerrar la ventana se cierra la base y termina el programa.");
        SwingUtilities.invokeLater(() -> {
            try {
                new SkillSwapFrame(controllers, usuarios, app::cerrar).setVisible(true);
            } catch (RuntimeException e) {
                out.println("No se pudo abrir la pantalla: " + e.getMessage());
                app.cerrar();
                System.exit(1);
            }
        });
    }
}
