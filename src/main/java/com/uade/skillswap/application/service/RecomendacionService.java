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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomendaciones (Strategy): el servicio solo arma el contexto y delega; la estrategia se elige por parametro
 * mediante la Simple Factory del dominio, asi que agregar una nueva no modifica este caso de uso.
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
    public List<PublicacionResumenDto> recomendar(Long actorId, TipoEstrategiaRecomendacion tipoEstrategia) {
        Perfil perfil = Cargas.perfilActivo(perfilRepository, actorId);
        List<Publicacion> candidatas = publicacionRepository.buscarPublicadas(config.candidatasRecomendacion());

        Map<Long, Perfil> autores = armador.autoresDe(candidatas); // una consulta para todos los autores
        Map<Long, Double> reputaciones = new HashMap<>();
        autores.forEach((id, autor) -> reputaciones.put(id, autor.getReputacion()));

        EstrategiaRecomendacion estrategia = EstrategiaRecomendacionFactory.crear(tipoEstrategia);
        List<Publicacion> elegidas = estrategia.recomendar(new ContextoRecomendacion(perfil, reputaciones),
                candidatas, config.limiteRecomendaciones());
        return armador.resumir(elegidas, autores);
    }
}
