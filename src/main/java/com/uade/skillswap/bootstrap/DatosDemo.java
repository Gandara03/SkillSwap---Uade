package com.uade.skillswap.bootstrap;

import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.presentation.Resultado;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Semilla de datos de demostracion. Usa solo los controllers (los mismos casos de uso que la UI), asi que
 * respeta todas las reglas del negocio. Es idempotente: si el perfil o la publicacion ya existen
 * (por ejemplo contra MySQL en una segunda ejecucion) los reutiliza en lugar de duplicarlos.
 */
public final class DatosDemo {

    public static final String ANA = "u-ana";
    public static final String BRUNO = "u-bruno";
    public static final String CECILIA = "u-cecilia";
    public static final String TECHCORP = "u-techcorp";

    /** Ids de los perfiles y publicaciones sembrados. */
    public record Semilla(Long anaId, Long brunoId, Long ceciliaId, Long techcorpId,
                          Long mentoriaAnaId, Long auditoriaCeciliaId, Long figmaCeciliaId, Long desafioTechcorpId) { }

    private final Composicion app;
    private final IdentidadMockAdapter identidad;
    private final LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

    public DatosDemo(Composicion app, IdentidadMockAdapter identidad) {
        this.app = app;
        this.identidad = identidad;
    }

    public Semilla sembrar() {
        try {
            Long ana = asegurarPerfil(new RegistrarPerfilCommand(ANA, "Ana Gomez", TipoPerfil.PROFESIONAL,
                    "America/Argentina/Buenos_Aires", "Ingeniera de software, mentora en arquitectura y patrones.",
                    set("java", "clean architecture", "uml"), set("patrones", "testing", "ux"),
                    List.of(new BloqueDisponibilidadDto(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(21, 0)),
                            new BloqueDisponibilidadDto(DayOfWeek.THURSDAY, LocalTime.of(18, 0), LocalTime.of(21, 0))),
                    CanalNotificacion.EMAIL));
            Long bruno = asegurarPerfil(new RegistrarPerfilCommand(BRUNO, "Bruno Diaz", TipoPerfil.PROFESIONAL,
                    "America/Argentina/Buenos_Aires", "Estudiante avanzado buscando mentorías de diseño de software.",
                    set("sql", "java"), set("java", "patrones", "clean architecture"),
                    List.of(new BloqueDisponibilidadDto(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))),
                    CanalNotificacion.PUSH));
            Long cecilia = asegurarPerfil(new RegistrarPerfilCommand(CECILIA, "Cecilia Paz", TipoPerfil.PROFESIONAL,
                    "America/Argentina/Buenos_Aires", "Diseñadora UX con foco en producto.",
                    set("ux", "figma", "investigación"), set("accesibilidad", "java"),
                    List.of(new BloqueDisponibilidadDto(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))),
                    CanalNotificacion.INTERNA));
            Long techcorp = asegurarPerfil(new RegistrarPerfilCommand(TECHCORP, "TechCorp S.A.",
                    TipoPerfil.ORGANIZACION, "America/Argentina/Buenos_Aires",
                    "Empresa de tecnología que propone desafíos reales a la comunidad.",
                    set("datos", "java"), set("talento joven", "ux"), List.of(), CanalNotificacion.EMAIL));

            Long mentoria = asegurarPublicacion(ANA, new CrearPublicacionCommand(TipoPublicacion.MENTORIA,
                            "Mentoría de Java y Clean Architecture",
                            "Sesiones 1 a 1 para estructurar un proyecto Java en capas, con foco en dependencias y testing.",
                            "Programación", Modalidad.REMOTA, 60, 10, BigDecimal.valueOf(40),
                            set("java", "arquitectura"), set("java", "clean architecture"),
                            TipoPoliticaCancelacion.FLEXIBLE,
                            List.of(franja(2, 0), franja(5, 0), franja(6, 0))),
                    id -> new CrearPromocionCommand(id, "Promo de lanzamiento", TipoPromocion.PORCENTAJE,
                            BigDecimal.valueOf(20), ahora.minusDays(1), ahora.plusDays(30)));
            Long auditoria = asegurarPublicacion(CECILIA, new CrearPublicacionCommand(TipoPublicacion.SERVICIO,
                    "Auditoría de UX completa",
                    "Relevamiento heurístico y recomendaciones priorizadas para tu producto digital.",
                    "Diseño", Modalidad.REMOTA, 120, 5, BigDecimal.valueOf(120),
                    set("ux", "auditoría"), set("ux", "investigación"),
                    TipoPoliticaCancelacion.ESTRICTA, List.of(franja(4, 0), franja(8, 0))), null);
            Long figma = asegurarPublicacion(CECILIA, new CrearPublicacionCommand(TipoPublicacion.SERVICIO,
                    "Prototipado en Figma",
                    "Taller práctico para llevar una idea a un prototipo navegable.",
                    "Diseño", Modalidad.PRESENCIAL, 90, 5, BigDecimal.valueOf(35),
                    set("figma", "prototipos"), set("figma", "ux"),
                    TipoPoliticaCancelacion.FLEXIBLE, List.of(franja(3, 2), franja(7, 2))), null);
            Long desafio = asegurarPublicacion(TECHCORP, new CrearPublicacionCommand(TipoPublicacion.DESAFIO,
                    "Desafío: dashboard de métricas para e-commerce",
                    "Construí un tablero de ventas con datos de ejemplo. La mejor propuesta se presenta al equipo.",
                    "Datos", Modalidad.REMOTA, 240, 20, BigDecimal.valueOf(25),
                    set("datos", "dashboard"), set("datos", "java"),
                    TipoPoliticaCancelacion.FLEXIBLE, List.of(franja(7, 0), franja(14, 0))), null);

            return new Semilla(ana, bruno, cecilia, techcorp, mentoria, auditoria, figma, desafio);
        } finally {
            identidad.cerrarSesion();
        }
    }

    // ---------- helpers ----------

    private Long asegurarPerfil(RegistrarPerfilCommand comando) {
        identidad.iniciarSesion(comando.userId());
        Resultado<PerfilDto> existente = app.getPerfilController().miPerfil();
        if (existente.esOk()) {
            return existente.valor().id();
        }
        return exigir(app.getPerfilController().registrar(comando), "registrar el perfil " + comando.userId()).id();
    }

    /** Busca por titulo entre las publicaciones del autor; si no existe la crea, la publica y (opcional) le pone promocion. */
    private Long asegurarPublicacion(String userId, CrearPublicacionCommand comando,
                                     Function<Long, CrearPromocionCommand> promocion) {
        identidad.iniciarSesion(userId);
        var publicaciones = app.getPublicacionController();
        var propias = exigir(publicaciones.misPublicaciones(PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO)),
                "listar publicaciones de " + userId);
        Optional<PublicacionResumenDto> existente = propias.contenido().stream()
                .filter(p -> p.titulo().equals(comando.titulo())).findFirst();
        if (existente.isPresent()) {
            return existente.get().id();
        }
        Long id = exigir(publicaciones.crearBorrador(comando), "crear '" + comando.titulo() + "'").id();
        exigir(publicaciones.publicar(id), "publicar '" + comando.titulo() + "'");
        if (promocion != null) {
            exigir(publicaciones.agregarPromocion(promocion.apply(id)), "agregar promoción a '" + comando.titulo() + "'");
        }
        return id;
    }

    /**
     * Franja de una hora dentro de {@code dias} dias, a las 18 + {@code horas}. Con {@code dias} de 2 o mas empieza
     * siempre a mas de 24 h de ahora.
     */
    private FranjaCommand franja(int dias, int horas) {
        LocalDateTime inicio = ahora.toLocalDate().plusDays(dias).atTime(18 + horas, 0);
        return new FranjaCommand(inicio, inicio.plusHours(1));
    }

    private static <T> T exigir(Resultado<T> r, String accion) {
        if (r.esError()) {
            throw new IllegalStateException("No se pudieron sembrar los datos de demo (" + accion + "): " + r.mensaje());
        }
        return r.valor();
    }

    private static Set<String> set(String... valores) {
        return new LinkedHashSet<>(List.of(valores));
    }
}
