package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.CalificacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.application.port.out.WalletRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Alta/edicion/baja de perfiles y resumen publico (Facade sobre perfil + calificaciones + publicaciones). */
public class PerfilService implements PerfilUseCase {

    private final PerfilRepository perfilRepository;
    private final WalletRepository walletRepository;
    private final CalificacionRepository calificacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final IdentidadPort identidad;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;
    private final SkillSwapConfig config;
    private final ArmadorResumenes armador;

    public PerfilService(PerfilRepository perfilRepository, WalletRepository walletRepository,
                         CalificacionRepository calificacionRepository, PublicacionRepository publicacionRepository,
                         PromocionRepository promocionRepository, IdentidadPort identidad,
                         TransaccionPort transaccion, RelojPort reloj, SkillSwapConfig config) {
        this.perfilRepository = perfilRepository;
        this.walletRepository = walletRepository;
        this.calificacionRepository = calificacionRepository;
        this.publicacionRepository = publicacionRepository;
        this.identidad = identidad;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.config = config;
        this.armador = new ArmadorResumenes(perfilRepository, new CalculadorPrecios(promocionRepository, reloj));
    }

    @Override
    public PerfilDto registrar(RegistrarPerfilCommand comando) {
        Entradas.exigir(comando, "El comando");
        Entradas.exigirTexto(comando.userId(), "El userId");
        // El userId no lo elige el cliente: debe ser el del usuario autenticado en el servicio de identidad.
        IdentidadPort.UsuarioIdentidad usuario = identidad.usuarioActual()
                .orElseThrow(() -> new NoAutorizadoException("No hay una sesion iniciada"));
        if (!usuario.userId().equals(comando.userId().trim())) {
            throw new NoAutorizadoException("El userId no coincide con el usuario autenticado");
        }
        if (!usuario.activo()) {
            throw new NoAutorizadoException("El usuario esta inactivo en el servicio de identidad");
        }
        var zona = Entradas.zona(comando.zonaHoraria());
        var habilidades = Entradas.habilidades(comando.habilidades());
        var intereses = Entradas.intereses(comando.intereses());
        var disponibilidad = Entradas.bloques(comando.disponibilidad());

        return transaccion.ejecutar(() -> {
            if (perfilRepository.buscarPorUserId(usuario.userId()).isPresent()) {
                throw new ValidacionException("Ya existe un perfil para el usuario " + usuario.userId());
            }
            LocalDateTime ahora = reloj.ahora();
            Perfil perfil = Perfil.crear(usuario.userId(), comando.nombreVisible(), comando.tipo(), zona, ahora);
            perfil.actualizarDatos(comando.nombreVisible(), comando.biografia(), zona, habilidades, intereses,
                    disponibilidad, comando.canalPreferido());
            Perfil guardado = perfilRepository.guardar(perfil);
            // El perfil se guarda primero para conocer su id; el bono inicial entra como movimiento de la wallet.
            walletRepository.guardar(Wallet.abrir(guardado.getId(), config.bonoInicial(), ahora));
            return Mapeador.perfil(guardado);
        });
    }

    @Override
    public PerfilDto actualizar(Long actorId, ActualizarPerfilCommand comando) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(comando, "El comando");
        var zona = Entradas.zona(comando.zonaHoraria());
        var habilidades = Entradas.habilidades(comando.habilidades());
        var intereses = Entradas.intereses(comando.intereses());
        var disponibilidad = Entradas.bloques(comando.disponibilidad());

        return transaccion.ejecutar(() -> {
            Perfil perfil = Cargas.perfil(perfilRepository, actorId);
            perfil.actualizarDatos(comando.nombreVisible(), comando.biografia(), zona, habilidades, intereses,
                    disponibilidad, comando.canalPreferido());
            return Mapeador.perfil(perfilRepository.guardar(perfil));
        });
    }

    @Override
    public PerfilDto obtener(Long perfilId) {
        return Mapeador.perfil(Cargas.perfil(perfilRepository, perfilId));
    }

    @Override
    public PerfilPublicoDto verPerfilPublico(Long perfilId) {
        Perfil perfil = Cargas.perfil(perfilRepository, perfilId);

        int nCalificaciones = config.ultimasCalificacionesPerfilPublico();
        List<CalificacionDto> calificaciones = calificacionRepository
                .paginarPorDestinatario(perfilId, PaginaRequest.de(0, tamano(nCalificaciones)))
                .contenido().stream().limit(Math.max(nCalificaciones, 0)).map(Mapeador::calificacion).toList();

        // El repositorio pagina por autor sin filtrar estado: se pide una pagina acotada y se quedan las PUBLICADAS.
        int nPublicaciones = config.ultimasPublicacionesPerfilPublico();
        List<Publicacion> activas = publicacionRepository
                .buscarPorAutor(perfilId, PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO))
                .contenido().stream().filter(Publicacion::estaPublicada).limit(Math.max(nPublicaciones, 0)).toList();
        List<PublicacionResumenDto> resumenes = armador.resumir(activas, Map.of(perfil.getId(), perfil));

        return new PerfilPublicoDto(perfil.getId(), perfil.getNombreVisible(), perfil.getBiografia(),
                Mapeador.nombresDeHabilidades(perfil.getHabilidades()), perfil.getReputacion(),
                perfil.getCantidadCalificaciones(), calificaciones, resumenes);
    }

    @Override
    public void darDeBaja(Long actorId) {
        Entradas.exigir(actorId, "El actor");
        transaccion.ejecutar(() -> {
            Perfil perfil = Cargas.perfil(perfilRepository, actorId);
            perfil.darDeBaja();
            perfilRepository.guardar(perfil);
            pausarPublicacionesActivas(actorId, reloj.ahora());
        });
    }

    /** Un perfil dado de baja no debe seguir recibiendo solicitudes: se pausan sus publicaciones publicadas. */
    private void pausarPublicacionesActivas(Long autorId, LocalDateTime ahora) {
        int numeroPagina = 0;
        Pagina<Publicacion> pagina;
        do {
            pagina = publicacionRepository.buscarPorAutor(autorId,
                    PaginaRequest.de(numeroPagina++, PaginaRequest.TAMANO_MAXIMO));
            for (Publicacion publicacion : pagina.contenido()) {
                if (publicacion.estaPublicada()) {
                    publicacion.pausar(autorId, ahora);
                    publicacionRepository.guardar(publicacion);
                }
            }
        } while (pagina.tieneSiguiente());
    }

    private static int tamano(int solicitado) {
        return Math.min(Math.max(solicitado, 1), PaginaRequest.TAMANO_MAXIMO);
    }
}
