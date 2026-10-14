package com.uade.skillswap.application.service;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.application.recomendacion.ContextoRecomendacion;
import com.uade.skillswap.application.recomendacion.EstrategiaRecomendacion;
import com.uade.skillswap.application.recomendacion.EstrategiaRecomendacionFactory;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomendaciones (Strategy): el servicio solo arma el contexto y delega; la estrategia se elige por parametro
 * mediante la Simple Factory del dominio, asi que agregar una nueva no modifica este caso de uso.
 * El puntaje se calcula en memoria sobre todas las candidatas, asi que la pagina se recorta del ranking ya ordenado.
 */
public class RecomendacionService implements RecomendacionUseCase {

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final SkillSwapConfig config;
    private final ArmadorResumenes armador;

    public RecomendacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                                RelojPort reloj, PromocionRepository promocionRepository, SkillSwapConfig config) {
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.config = config;
        this.armador = new ArmadorResumenes(perfilRepository, new CalculadorPrecios(promocionRepository, reloj));
    }

    @Override
    public Pagina<PublicacionResumenDto> recomendar(Long actorId, TipoEstrategiaRecomendacion tipoEstrategia,
                                                    PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        Perfil perfil = Cargas.perfilActivo(perfilRepository, actorId);
        List<Publicacion> candidatas = publicacionRepository.buscarPublicadas(config.candidatasRecomendacion());

        Map<Long, Perfil> autores = armador.autoresDe(candidatas); // una consulta para todos los autores
        Map<Long, Double> reputaciones = new HashMap<>();
        autores.forEach((id, autor) -> reputaciones.put(id, autor.getReputacion()));

        EstrategiaRecomendacion estrategia = EstrategiaRecomendacionFactory.crear(tipoEstrategia);
        List<Publicacion> ranking = estrategia.recomendar(new ContextoRecomendacion(perfil, reputaciones),
                candidatas, config.limiteRecomendaciones());
        Pagina<Publicacion> elegidas = Pagina.recortar(ranking, pagina);
        return new Pagina<>(armador.resumir(elegidas.contenido(), autores), elegidas.pagina(), elegidas.tamano(),
                elegidas.totalElementos());
    }
}
