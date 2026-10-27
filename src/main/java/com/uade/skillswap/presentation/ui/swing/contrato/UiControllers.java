package com.uade.skillswap.presentation.ui.swing.contrato;

import com.uade.skillswap.presentation.ui.swing.contrato.Models.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo unico que la pantalla conoce del sistema. Cada operacion recibe el userId del usuario activo (login simulado).
 * Si la operacion no se puede completar, se lanza {@link OperacionFallidaException} con el mensaje para el usuario.
 */
public interface UiControllers {
    interface BusquedaController {
        /** Categoria que la pantalla usa para "sin filtrar". */
        String TODAS_LAS_CATEGORIAS = "Todas";

        List<Publicacion> buscar(FiltrosBusqueda filtros);
        List<Publicacion> recomendar(String userId);
        List<String> categorias();
    }

    interface PublicacionController {
        List<Publicacion> listarPropias(String userId);
        Publicacion crearBorrador(String userId, NuevaPublicacion datos);
        /** El motivo solo se usa para CANCELAR. */
        Publicacion cambiarEstado(String userId, String publicacionId, AccionPublicacion accion, String motivo);
        List<Franja> franjasDisponibles(String userId, String publicacionId);
    }

    interface ContratacionController {
        List<Contratacion> listarContrataciones(String userId);
        Contratacion solicitar(String userId, String publicacionId, String franjaId, String mensaje, String objetivo);
        Contratacion aceptar(String userId, String contratacionId);
        Contratacion rechazar(String userId, String contratacionId, String motivo);
        Contratacion pagar(String userId, String contratacionId);
        Contratacion iniciar(String userId, String contratacionId);
        Contratacion completar(String userId, String contratacionId);
        ResultadoCancelacion cancelar(String userId, String contratacionId, String motivo);
    }

    interface CalificacionController {
        void calificar(String userId, String contratacionId, int puntaje, String comentario);
    }

    interface WalletController {
        BigDecimal consultarSaldo(String userId);
        List<Movimiento> listarMovimientos(String userId);
    }

    interface NotificacionController {
        List<Notificacion> listarNotificaciones(String userId);
        void marcarLeida(String userId, String notificacionId);
        void marcarTodasLeidas(String userId);
    }

    interface PerfilController {
        Perfil verPerfil(String userId);
    }

    BusquedaController busqueda();
    PublicacionController publicaciones();
    ContratacionController contrataciones();
    CalificacionController calificaciones();
    WalletController wallet();
    NotificacionController notificaciones();
    PerfilController perfiles();
}
