package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.DatosPublicacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.JpaTransaccionAdapter;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaCalificacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaContratacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaNotificacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPerfilRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPromocionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPublicacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Base de los tests de persistencia: levanta una base H2 en memoria NUEVA por test (Flyway + hbm2ddl=validate,
 * igual que en la aplicacion) y arma los repositorios JPA. Tambien trae factories de datos de prueba que
 * usan los repositorios reales, asi cada test parte de filas consistentes con las FKs del esquema.
 */
public abstract class JpaTestBase {

    /** Fecha fija "actual" de los tests, sin fracciones de segundo (la columna guarda microsegundos). */
    protected static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 10, 12, 0, 0);

    protected JpaContexto ctx;
    protected JpaTransaccionAdapter transaccion;
    protected JpaPerfilRepository perfiles;
    protected JpaPublicacionRepository publicaciones;
    protected JpaContratacionRepository contrataciones;
    protected JpaWalletRepository wallets;
    protected JpaCalificacionRepository calificaciones;
    protected JpaNotificacionRepository notificaciones;
    protected JpaPromocionRepository promociones;

    @BeforeEach
    protected void iniciarBase() {
        ctx = BaseDeDatos.iniciarH2();
        transaccion = new JpaTransaccionAdapter(ctx);
        perfiles = new JpaPerfilRepository(ctx);
        publicaciones = new JpaPublicacionRepository(ctx);
        contrataciones = new JpaContratacionRepository(ctx);
        wallets = new JpaWalletRepository(ctx);
        calificaciones = new JpaCalificacionRepository(ctx);
        notificaciones = new JpaNotificacionRepository(ctx);
        promociones = new JpaPromocionRepository(ctx);
    }

    @AfterEach
    protected void cerrarBase() {
        ctx.cerrar();
    }

    /** Cantidad de filas de una entidad JPA en la base de ese contexto (para ver que quedo despues de un commit o rollback). */
    protected static long contarFilas(JpaContexto ctx, Class<?> entidad) {
        return ctx.leer(em -> em.createQuery("select count(e) from " + entidad.getSimpleName() + " e", Long.class)
                .getSingleResult());
    }

    // ---------- perfiles ----------

    protected Perfil profesional(String userId) {
        return perfil(userId, TipoPerfil.PROFESIONAL);
    }

    protected Perfil organizacion(String userId) {
        return perfil(userId, TipoPerfil.ORGANIZACION);
    }

    protected Perfil perfil(String userId, TipoPerfil tipo) {
        return perfiles.guardar(Perfil.crear(userId, "Nombre de " + userId, tipo,
                ZoneId.of("America/Argentina/Buenos_Aires"), AHORA));
    }

    /** El dominio no permite fijar la reputacion directamente: se rehidrata el mismo perfil con otra y se guarda. */
    protected Perfil conReputacion(Perfil p, double reputacion, int cantidadCalificaciones) {
        return perfiles.guardar(Perfil.reconstruir(p.getId(), p.getUserId(), p.getTipo(), p.getNombreVisible(),
                p.getBiografia(), p.getZonaHoraria(), p.getHabilidades(), p.getIntereses(), p.getDisponibilidad(),
                p.getCanalPreferido(), reputacion, cantidadCalificaciones, p.estaActivo(), p.getCreadoEn(),
                p.getVersion()));
    }

    // ---------- publicaciones ----------

    protected PublicacionBuilder publicacion(Perfil autor) {
        return new PublicacionBuilder(autor);
    }

    /** Builder de publicaciones de prueba (por defecto: PUBLICADA, remota, 100 SC, una franja a partir de AHORA+1 dia). */
    protected final class PublicacionBuilder {
        private final Perfil autor;
        private TipoPublicacion tipo;
        private String titulo = "Mentoria de Java";
        private String descripcion = "Aprende Java con ejemplos";
        private String categoria = "programacion";
        private Modalidad modalidad = Modalidad.REMOTA;
        private int cupo = 5;
        private SkillCoins precio = SkillCoins.de(100);
        private List<String> etiquetas = List.of();
        private List<String> habilidades = List.of();
        private List<LocalDateTime> inicios = List.of(AHORA.plusDays(1));
        private boolean publicar = true;
        private LocalDateTime publicadaEn = AHORA;

        private PublicacionBuilder(Perfil autor) {
            this.autor = autor;
        }

        public PublicacionBuilder tipo(TipoPublicacion v) {
            this.tipo = v;
            return this;
        }

        public PublicacionBuilder titulo(String v) {
            this.titulo = v;
            return this;
        }

        public PublicacionBuilder descripcion(String v) {
            this.descripcion = v;
            return this;
        }

        public PublicacionBuilder categoria(String v) {
            this.categoria = v;
            return this;
        }

        public PublicacionBuilder modalidad(Modalidad v) {
            this.modalidad = v;
            return this;
        }

        public PublicacionBuilder cupo(int v) {
            this.cupo = v;
            return this;
        }

        public PublicacionBuilder precio(long v) {
            this.precio = SkillCoins.de(v);
            return this;
        }

        public PublicacionBuilder etiquetas(String... v) {
            this.etiquetas = List.of(v);
            return this;
        }

        public PublicacionBuilder habilidades(String... v) {
            this.habilidades = List.of(v);
            return this;
        }

        /** Una franja de una hora por cada inicio indicado. */
        public PublicacionBuilder franjasEn(LocalDateTime... v) {
            this.inicios = List.of(v);
            return this;
        }

        public PublicacionBuilder borrador() {
            this.publicar = false;
            return this;
        }

        public PublicacionBuilder publicadaEn(LocalDateTime v) {
            this.publicadaEn = v;
            return this;
        }

        /** Arma el agregado SIN guardarlo (para tests que lo guardan ellos). */
        public Publicacion construir() {
            TipoPublicacion t = tipo != null ? tipo
                    : autor.getTipo() == TipoPerfil.ORGANIZACION ? TipoPublicacion.DESAFIO : TipoPublicacion.MENTORIA;
            List<Habilidad> habs = new ArrayList<>();
            for (String h : habilidades) {
                habs.add(Habilidad.de(h));
            }
            Publicacion p = Publicacion.crearBorrador(t, autor.getId(), autor.getTipo(), new DatosPublicacion(titulo,
                    descripcion, categoria, modalidad, 60, cupo, precio, etiquetas, habs,
                    TipoPoliticaCancelacion.FLEXIBLE), AHORA);
            for (LocalDateTime inicio : inicios) {
                p.agregarFranja(inicio, inicio.plusHours(1), AHORA);
            }
            if (publicar) {
                p.publicar(autor.getId(), publicadaEn);
            }
            return p;
        }

        public Publicacion guardar() {
            return publicaciones.guardar(construir());
        }
    }

    // ---------- contrataciones ----------

    /** Solicitud (PENDIENTE) sobre la primera franja de la publicacion, ya guardada. */
    protected Contratacion solicitud(Publicacion publicacion, Perfil cliente) {
        Franja franja = publicacion.getFranjas().get(0);
        return contrataciones.guardar(Contratacion.solicitar(publicacion.getId(), cliente.getId(),
                publicacion.getAutorId(), franja.getId(), "Quiero aprender", "Mejorar mi nivel",
                publicacion.getPrecioBase(), AHORA));
    }

    /** Recorre el flujo completo aceptar -> pagar -> iniciar -> completar, guardando en cada paso. */
    protected Contratacion completada(Publicacion publicacion, Perfil cliente) {
        Contratacion c = solicitud(publicacion, cliente);
        Long proveedor = publicacion.getAutorId();
        c.aceptar(proveedor, AHORA.plusMinutes(1));
        c = contrataciones.guardar(c);
        c.pagar(cliente.getId(), AHORA.plusMinutes(2));
        c = contrataciones.guardar(c);
        c.iniciar(proveedor, AHORA.plusMinutes(3));
        c = contrataciones.guardar(c);
        c.completar(proveedor, AHORA.plusMinutes(4));
        return contrataciones.guardar(c);
    }
}
