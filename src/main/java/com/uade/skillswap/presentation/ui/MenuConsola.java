package com.uade.skillswap.presentation.ui;

import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;
import com.uade.skillswap.presentation.controller.IBusquedaController;
import com.uade.skillswap.presentation.controller.IContratacionController;
import com.uade.skillswap.presentation.controller.INotificacionController;
import com.uade.skillswap.presentation.controller.IPerfilController;
import com.uade.skillswap.presentation.controller.IRecomendacionController;
import com.uade.skillswap.presentation.controller.IWalletController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Menu de consola minimo: solo habla con los controllers (y con Autenticador para el login). */
public class MenuConsola {

    private static final PaginaRequest PAGINA = PaginaRequest.de(0, 10);

    private final Autenticador autenticador;
    private final IPerfilController perfiles;
    private final IBusquedaController busqueda;
    private final IRecomendacionController recomendaciones;
    private final IContratacionController contrataciones;
    private final IWalletController wallet;
    private final INotificacionController notificaciones;
    private final BufferedReader entrada;
    private final PrintStream salida;

    public MenuConsola(Autenticador autenticador, IPerfilController perfiles, IBusquedaController busqueda,
                       IRecomendacionController recomendaciones, IContratacionController contrataciones,
                       IWalletController wallet, INotificacionController notificaciones,
                       InputStream entrada, PrintStream salida) {
        this.autenticador = autenticador;
        this.perfiles = perfiles;
        this.busqueda = busqueda;
        this.recomendaciones = recomendaciones;
        this.contrataciones = contrataciones;
        this.wallet = wallet;
        this.notificaciones = notificaciones;
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.salida = salida;
    }

    public void ejecutar() {
        salida.println("\n=== SkillSwap - menú de consola ===");
        boolean seguir = true;
        while (seguir) {
            mostrarOpciones();
            String opcion = leer("Opción: ");
            if (opcion == null) {
                break;
            }
            seguir = procesar(opcion.trim());
        }
        salida.println("Hasta luego.");
    }

    private void mostrarOpciones() {
        Resultado<PerfilDto> yo = perfiles.miPerfil();
        salida.println("\nSesión: " + (yo.esOk() ? yo.valor().nombreVisible() + " (" + yo.valor().userId() + ")"
                : "sin iniciar"));
        salida.println("""
                1) Iniciar sesión      4) Mis contrataciones
                2) Buscar              5) Mi wallet
                3) Recomendaciones     6) Notificaciones
                7) Cerrar sesión       0) Salir""");
    }

    private boolean procesar(String opcion) {
        switch (opcion) {
            case "1" -> iniciarSesion();
            case "2" -> buscar();
            case "3" -> recomendar();
            case "4" -> misContrataciones();
            case "5" -> miWallet();
            case "6" -> misNotificaciones();
            case "7" -> {
                autenticador.cerrarSesion();
                salida.println("Sesión cerrada.");
            }
            case "0" -> {
                return false;
            }
            default -> salida.println("Opción inválida.");
        }
        return true;
    }

    private void iniciarSesion() {
        String userId = leer("userId (u-ana, u-bruno, u-cecilia, u-techcorp): ");
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            autenticador.iniciarSesion(userId.trim());
        } catch (IllegalArgumentException e) {
            salida.println("No existe el usuario '" + userId.trim() + "'.");
        }
    }

    private void buscar() {
        String texto = leer("Texto a buscar (enter = todo): ");
        FiltroBusqueda filtro = FiltroBusqueda.vacio();
        if (texto != null && !texto.isBlank()) {
            filtro = new FiltroBusqueda(texto.trim(), null, null, null, null, null, null, null, null, null,
                    filtro.orden());
        }
        mostrarLista(busqueda.buscar(filtro, PAGINA).mapear(p -> p.contenido()), "Sin resultados.");
    }

    private void recomendar() {
        String tipo = leer("1) Por afinidad  2) Por popularidad: ");
        if (tipo == null) {
            return;
        }
        TipoEstrategiaRecomendacion estrategia = "2".equals(tipo.trim())
                ? TipoEstrategiaRecomendacion.POPULARIDAD : TipoEstrategiaRecomendacion.AFINIDAD;
        mostrarLista(recomendaciones.recomendar(estrategia, PAGINA).mapear(p -> p.contenido()),
                "No hay recomendaciones por ahora.");
    }

    private void misContrataciones() {
        var r = contrataciones.misContrataciones(PAGINA);
        if (r.esError()) {
            salida.println("No se pudo completar: " + r.mensaje());
        } else if (r.valor().contenido().isEmpty()) {
            salida.println("Todavía no tenés contrataciones.");
        } else {
            r.valor().contenido().forEach(c -> salida.println("  " + Formato.contratacion(c)));
        }
    }

    private void miWallet() {
        var saldo = wallet.consultar();
        if (saldo.esError()) {
            salida.println("No se pudo completar: " + saldo.mensaje());
            return;
        }
        salida.println("Saldo: " + Formato.monto(saldo.valor().saldo()));
        var movimientos = wallet.movimientos(PaginaRequest.de(0, 5));
        if (movimientos.esOk()) {
            movimientos.valor().contenido().forEach(m -> salida.println("  " + Formato.movimiento(m)));
        }
    }

    private void misNotificaciones() {
        var r = notificaciones.listar(false, PAGINA);
        if (r.esError()) {
            salida.println("No se pudo completar: " + r.mensaje());
            return;
        }
        if (r.valor().contenido().isEmpty()) {
            salida.println("No tenés notificaciones.");
            return;
        }
        r.valor().contenido().forEach(n -> salida.println("  [" + n.id() + "] " + Formato.notificacion(n)));
        String id = leer("Id a marcar como leída (enter = volver): ");
        if (id == null || id.isBlank()) {
            return;
        }
        try {
            var leida = notificaciones.marcarLeida(Long.parseLong(id.trim()));
            salida.println(leida.esOk() ? "Marcada como leída." : "No se pudo completar: " + leida.mensaje());
        } catch (NumberFormatException e) {
            salida.println("Id inválido.");
        }
    }

    private void mostrarLista(Resultado<List<PublicacionResumenDto>> r, String vacio) {
        if (r.esError()) {
            salida.println("No se pudo completar: " + r.mensaje());
        } else if (r.valor().isEmpty()) {
            salida.println(vacio);
        } else {
            r.valor().forEach(p -> salida.println("  " + Formato.publicacion(p)));
        }
    }

    /** Devuelve null si se cerro la entrada (fin de archivo). */
    private String leer(String prompt) {
        salida.print(prompt);
        salida.flush();
        try {
            return entrada.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
