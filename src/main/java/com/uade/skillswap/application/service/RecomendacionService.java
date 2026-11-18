package com.uade.skillswap.application.service;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.application.recomendacion.DatosRecomendacion;
import com.uade.skillswap.application.recomendacion.EstrategiaRecomendacion;
import com.uade.skillswap.application.recomendacion.EstrategiaRecomendacionFactory;
import com.uade.skillswap.application.recomendacion.TipoEstrategiaRecomendacion;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomendaciones (Strategy): el servicio solo arma los datos y delega; la estrategia se elige por parametro
 * con EstrategiaRecomendacionFactory, asi que agregar una nueva no modifica este caso de uso.
 * El puntaje se calcula en memoria sobre todas las candidatas, asi que la pagina se recorta del ranking ya ordenado.
 * Las contrataciones pagadas recientes de las candidatas se cuentan con una sola consulta agrupada.
 */
public class RecomendacionService implements RecomendacionUseCase {

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final ContratacionRepository contratacionRepository;
    private final RelojPort reloj;
    private final SkillSwapConfig config;
    private final ArmadorResumenes armador;

    public RecomendacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                                ContratacionRepository contratacionRepository, RelojPort reloj,
                                PromocionRepository promocionRepository, SkillSwapConfig config) {
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.contratacionRepository = contratacionRepository;
        this.reloj = reloj;
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

        LocalDateTime desde = reloj.ahora().minusDays(config.diasPopularidadReciente());
        List<Long> ids = candidatas.stream().map(Publicacion::getId).toList();
        Map<Long, Long> pagadasRecientes = contratacionRepository.contarPagadasDesde(ids, desde);

        EstrategiaRecomendacion estrategia = EstrategiaRecomendacionFactory.crear(tipoEstrategia);
        List<Publicacion> ranking = estrategia.recomendar(new DatosRecomendacion(perfil, reputaciones, pagadasRecientes),
                candidatas, config.limiteRecomendaciones());
        Pagina<Publicacion> elegidas = Pagina.recortar(ranking, pagina);
        return new Pagina<>(armador.resumir(elegidas.contenido(), autores), elegidas.pagina(), elegidas.tamano(),
                elegidas.totalElementos());
    }
}
