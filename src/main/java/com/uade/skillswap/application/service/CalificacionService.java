package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.CalificacionRepository;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;

/** Calificaciones verificadas; recalcula la reputacion del destinatario en la misma transaccion. */
public class CalificacionService implements CalificacionUseCase {

    private final CalificacionRepository calificacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final PerfilRepository perfilRepository;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;

    public CalificacionService(CalificacionRepository calificacionRepository,
                               ContratacionRepository contratacionRepository, PerfilRepository perfilRepository,
                               TransaccionPort transaccion, RelojPort reloj) {
        this.calificacionRepository = calificacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.perfilRepository = perfilRepository;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public CalificacionDto calificar(Long actorId, CalificarCommand comando) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(comando, "El comando");
        Entradas.exigir(comando.contratacionId(), "La contratacion");

        return transaccion.ejecutar(() -> {
            Contratacion contratacion = Cargas.contratacion(contratacionRepository, comando.contratacionId());
            if (!contratacion.esParticipante(actorId)) {
                throw new NoAutorizadoException("Solo los participantes pueden calificar la contratacion");
            }
            // El agregado verifica que este COMPLETADA y define al destinatario (la contraparte).
            Calificacion nueva = Calificacion.registrar(contratacion, actorId, Puntaje.de(comando.puntaje()),
                    comando.comentario(), reloj.ahora());
            if (calificacionRepository.existe(contratacion.getId(), actorId)) {
                throw new ReglaDeNegocioException("Ya calificaste esta contratacion");
            }
            Calificacion guardada = calificacionRepository.guardar(nueva);

            // La reputacion solo se modifica por recalculo desde las calificaciones verificadas (incluye la nueva).
            Perfil destinatario = Cargas.perfil(perfilRepository, guardada.getDestinatarioId());
            destinatario.recalcularReputacion(calificacionRepository.buscarPorDestinatario(destinatario.getId()));
            perfilRepository.guardar(destinatario);
            return Mapeador.calificacion(guardada);
        });
    }

    @Override
    public Pagina<CalificacionDto> listarRecibidas(Long perfilId, PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        Cargas.perfil(perfilRepository, perfilId);
        return calificacionRepository.paginarPorDestinatario(perfilId, pagina).mapear(Mapeador::calificacion);
    }
}
