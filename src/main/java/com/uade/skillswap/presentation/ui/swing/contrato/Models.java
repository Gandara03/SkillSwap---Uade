package com.uade.skillswap.presentation.ui.swing.contrato;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelos de vista que intercambian la pantalla y {@link UiControllers}. Solo usan tipos de Java: la vista no conoce
 * ni el dominio ni los DTO de la aplicacion.
 */
public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL }
    public enum TipoPublicacion { MENTORIA, SERVICIO, DESAFIO }
    public enum PoliticaCancelacion { FLEXIBLE, ESTRICTA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }

    /** Acciones del autor sobre una publicacion; el texto es la etiqueta del boton. */
    public enum AccionPublicacion {
        PUBLICAR("Publicar"), PAUSAR("Pausar"), REANUDAR("Reanudar"), CERRAR("Cerrar"), CANCELAR("Cancelar");

        private final String etiqueta;

        AccionPublicacion(String etiqueta) { this.etiqueta = etiqueta; }

        public String etiqueta() { return etiqueta; }
    }

    /** Acciones sobre una contratacion; el texto es la etiqueta del boton. */
    public enum AccionContratacion {
        ACEPTAR("Aceptar"), RECHAZAR("Rechazar"), PAGAR("Pagar"), INICIAR("Iniciar"), COMPLETAR("Completar"),
        CANCELAR("Cancelar"), CALIFICAR("Calificar");

        private final String etiqueta;

        AccionContratacion(String etiqueta) { this.etiqueta = etiqueta; }

        public String etiqueta() { return etiqueta; }
    }

    /** Criterios de orden de la busqueda; el texto es el que muestra el combo. */
    public enum OrdenBusqueda {
        RELEVANCIA("Relevancia"), MEJOR_REPUTACION("Mejor reputación"), MENOR_PRECIO("Menor precio"),
        MAS_RECIENTES("Más recientes");

        private final String etiqueta;

        OrdenBusqueda(String etiqueta) { this.etiqueta = etiqueta; }

        @Override public String toString() { return etiqueta; }
    }

    public record Resena(int puntaje, String comentario, LocalDateTime fecha) {}

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados, List<Resena> ultimasResenas) {}

    /** {@code accionesDelAutor} solo tiene sentido en "Mis publicaciones", donde quien mira es el autor. */
    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              BigDecimal precio, double reputacion, String proveedor,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada, int cupoDisponible, List<AccionPublicacion> accionesDelAutor) {}

    /** Datos del formulario de nueva publicacion, con su primera franja de disponibilidad. */
    public record NuevaPublicacion(TipoPublicacion tipo, String titulo, String descripcion, String categoria,
                                   Modalidad modalidad, int duracionMinutos, int cupo, BigDecimal precio,
                                   PoliticaCancelacion politica, List<String> etiquetas, List<String> habilidades,
                                   LocalDateTime inicioFranja, int minutosFranja) {}

    public record Franja(String id, LocalDateTime inicio, LocalDateTime fin) {}

    /** {@code acciones}: las que puede pedir el usuario activo, segun el estado y su parte en la contratacion. */
    public record Contratacion(String id, String titulo, String contraparte,
                               LocalDateTime fecha, BigDecimal precio, EstadoContratacion estado,
                               List<AccionContratacion> acciones) {}

    public record ResultadoCancelacion(Contratacion contratacion, String politica,
                                       BigDecimal reintegroCliente, BigDecimal compensacionProveedor) {}

    public record Movimiento(String concepto, LocalDateTime fecha, BigDecimal importe, BigDecimal saldo) {}

    public record Notificacion(String id, String titulo, String detalle,
                               LocalDateTime fecha, boolean leida, String tipo) {}

    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad, OrdenBusqueda orden) {}
}
