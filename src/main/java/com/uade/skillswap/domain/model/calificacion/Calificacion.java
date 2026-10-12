package com.uade.skillswap.domain.model.calificacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.vo.Puntaje;

import java.time.LocalDateTime;

/** Calificacion verificada: solo participantes de una contratacion completada, y nunca a si mismo. */
public class Calificacion {

    private Long id;
    private final Long contratacionId;
    private final Long autorId;
    private final Long destinatarioId;
    private final Puntaje puntaje;
    private final String comentario;
    private final LocalDateTime fecha;

    private Calificacion(Long id, Long contratacionId, Long autorId, Long destinatarioId, Puntaje puntaje,
                         String comentario, LocalDateTime fecha) {
        this.id = id;
        this.contratacionId = contratacionId;
        this.autorId = autorId;
        this.destinatarioId = destinatarioId;
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    /** Verifica que el autor participe de una contratacion completada; el destinatario es la contraparte. */
    public static Calificacion registrar(Contratacion contratacion, Long autorId, Puntaje puntaje,
                                         String comentario, LocalDateTime ahora) {
        if (puntaje == null) {
            throw new ReglaDeNegocioException("El puntaje es obligatorio");
        }
        if (!contratacion.estaCompletada()) {
            throw new ReglaDeNegocioException("Solo se puede calificar una contratacion completada");
        }
        if (!contratacion.esParticipante(autorId)) {
            throw new ReglaDeNegocioException("Solo los participantes de la contratacion pueden calificar");
        }
        Long destinatarioId = contratacion.contraparteDe(autorId);
        if (destinatarioId.equals(autorId)) {
            throw new ReglaDeNegocioException("Una persona no puede calificarse a si misma");
        }
        return new Calificacion(null, contratacion.getId(), autorId, destinatarioId, puntaje, comentario, ahora);
    }

    /** Solo para rehidratar desde persistencia. */
    public static Calificacion reconstruir(Long id, Long contratacionId, Long autorId, Long destinatarioId,
                                           Puntaje puntaje, String comentario, LocalDateTime fecha) {
        return new Calificacion(id, contratacionId, autorId, destinatarioId, puntaje, comentario, fecha);
    }

    public Long getId() {
        return id;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public Long getAutorId() {
        return autorId;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public Puntaje getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
