import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.FranjaDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.PropuestaReprogramacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.domain.model.contratacion.AccionContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.presentation.Resultado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Evidencia del area 2 de la revision: secuencias aleatorias de acciones (semilla fija) sobre el sistema real
 * (Composicion + H2 + JPA, solo por controllers). Despues de CADA accion verifica:
 *   1. cupo: 0 <= cupoDisponible <= cupo, y cupo ocupado == contrataciones con reserva de esa publicacion;
 *   2. franjas: una franja esta ocupada si y solo si la reserva exactamente UNA contratacion aceptada;
 *   3. terminales: un estado terminal no admite ninguna transicion ni declara acciones;
 *   4. acciones declaradas (State) == acciones que realmente se pueden ejecutar, para el actor que las pide;
 *   5. cada transicion deja el estado esperado y agrega UNA linea de historial (estado, actor, motivo, fecha);
 *   6. una accion que falla no cambia nada (ni estados, ni franjas, ni cupo, ni saldos);
 *   7. el total de SkillCoins se conserva.
 * No es parte del repo. Se compila contra las clases del proyecto y se ejecuta con su classpath.
 */
public class SecuenciasAleatorias {

    static final String ANA = "u-ana", BRUNO = "u-bruno", CECILIA = "u-cecilia", TECH = "u-techcorp";
    static final String[] USUARIOS = {ANA, BRUNO, CECILIA, TECH};
    static final String[] AUTORES = {ANA, BRUNO, CECILIA};
    static final Set<String> CON_RESERVA = Set.of("ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA");
    static final Set<String> RETENIDO = Set.of("CONFIRMADA", "EN_CURSO");
    static final Set<String> TERMINAL_C = Set.of("COMPLETADA", "RECHAZADA", "CANCELADA");
    static final Set<String> TERMINAL_P = Set.of("CERRADA", "CANCELADA");
    static final Set<String> REPROGRAMABLE = Set.of("ACEPTADA", "CONFIRMADA");
    static final AccionContratacion[] TRANSICIONES_C = {AccionContratacion.ACEPTAR, AccionContratacion.RECHAZAR,
            AccionContratacion.PAGAR, AccionContratacion.INICIAR, AccionContratacion.COMPLETAR,
            AccionContratacion.CANCELAR};
    static final Map<AccionContratacion, String> DESTINO_C = Map.of(AccionContratacion.ACEPTAR, "ACEPTADA",
            AccionContratacion.RECHAZAR, "RECHAZADA", AccionContratacion.PAGAR, "CONFIRMADA",
            AccionContratacion.INICIAR, "EN_CURSO", AccionContratacion.COMPLETAR, "COMPLETADA",
            AccionContratacion.CANCELAR, "CANCELADA");
    static final Map<AccionPublicacion, String> DESTINO_P = Map.of(AccionPublicacion.PUBLICAR, "PUBLICADA",
            AccionPublicacion.PAUSAR, "PAUSADA", AccionPublicacion.REANUDAR, "PUBLICADA",
            AccionPublicacion.CERRAR, "CERRADA", AccionPublicacion.CANCELAR, "CANCELADA");

    /** Todo lo que se ve del sistema en un momento: sirve para los invariantes y para comparar antes/despues. */
    record Mundo(Map<Long, ContratacionDto> contratos, Map<Long, PublicacionDto> pubs, Map<String, BigDecimal> saldos,
                 String foto) { }

    static JpaContexto ctx;
    static IdentidadMockAdapter identidad;
    static Composicion app;
    static Random azar;
    static Mundo mundo;
    static final Map<String, Long> perfilId = new HashMap<>();
    static final List<Long> pubs = new ArrayList<>();
    static final Map<Long, String> autorDe = new HashMap<>();
    static final List<Long> contratos = new ArrayList<>();
    static final Map<Long, String> clienteDe = new HashMap<>();
    static final Map<Long, String> proveedorDe = new HashMap<>();
    static final Set<String> calificadas = new HashSet<>();
    static int franjasCreadas = 0;
    /** Para informar una sola vez cada contratacion con una propuesta colgada. */
    static final Set<String> propuestasColgadas = new HashSet<>();

    static final List<String> violaciones = new ArrayList<>();
    static final Map<String, Integer> conteo = new TreeMap<>();
    static int acciones = 0;
    static String etiqueta = "";

    public static void main(String[] args) {
        for (long semilla : new long[] {20261111L, 7L, 424242L, 31337L}) {
            secuencia(semilla, 500);
        }
        observaciones();

        System.out.println();
        System.out.println("[SECUENCIAS] acciones ejecutadas: " + acciones + " (cada una con todos los invariantes)");
        conteo.forEach((k, v) -> System.out.println("[SECUENCIAS]   " + k + " = " + v));
        if (violaciones.isEmpty()) {
            System.out.println("[SECUENCIAS] RESULTADO: OK, ningun invariante se rompio");
        } else {
            System.out.println("[SECUENCIAS] RESULTADO: " + violaciones.size() + " VIOLACIONES");
            violaciones.stream().limit(40).forEach(v -> System.out.println("[SECUENCIAS]   - " + v));
            System.exit(1);
        }
    }

    // ---------- secuencia aleatoria ----------

    static void secuencia(long semilla, int pasos) {
        azar = new Random(semilla);
        iniciar();
        int antes = violaciones.size();
        try {
            nuevaPublicacion(ANA, 4, 6, "5.00", true);
            nuevaPublicacion(BRUNO, 5, 6, "0.00", true);
            nuevaPublicacion(CECILIA, 1, 2, "12.50", false);
            mundo = leer();
            invariantes(mundo, null);
            for (int i = 0; i < pasos; i++) {
                etiqueta = "semilla " + semilla + ", paso " + i;
                int r = azar.nextInt(200);
                if (r < 40) {
                    solicitarAlAzar();
                } else if (r < 170 && !contratos.isEmpty()) {
                    accionSobreContratacion();
                } else if (r < 197) {
                    accionSobrePublicacion();
                } else {
                    nuevaPublicacion(elegir(AUTORES), 1 + azar.nextInt(3), azar.nextInt(4),
                            BigDecimal.valueOf(azar.nextInt(1500), 2).toPlainString(), azar.nextBoolean());
                    avanzar(true, "nueva publicacion");
                }
                acciones++;
            }
            for (ContratacionDto c : mundo.contratos().values()) {
                contar("estado final de contratacion " + c.estado());
            }
            for (PublicacionDto p : mundo.pubs().values()) {
                contar("estado final de publicacion " + p.estado());
            }
        } catch (RuntimeException e) {
            violacion("excepcion: " + e);
        } finally {
            cerrar();
        }
        System.out.println("[SECUENCIAS] " + (violaciones.size() == antes ? "OK    " : "FALLA ") + "semilla " + semilla
                + ": " + pasos + " acciones, " + contratos.size() + " contrataciones, " + pubs.size()
                + " publicaciones");
    }

    static void solicitarAlAzar() {
        Long pubId = elegir(pubs);
        if (azar.nextInt(10) < 8) { // casi siempre sobre una publicada, para que haya contrataciones
            List<Long> publicadas = new ArrayList<>();
            for (PublicacionDto candidata : mundo.pubs().values()) {
                if (candidata.estado().equals("PUBLICADA")) {
                    publicadas.add(candidata.id());
                }
            }
            if (!publicadas.isEmpty()) {
                pubId = elegir(publicadas);
            }
        }
        PublicacionDto p = mundo.pubs().get(pubId);
        if (p.franjas().isEmpty()) {
            return;
        }
        FranjaDto franja = elegir(p.franjas());
        String cliente = elegir(USUARIOS);
        como(cliente);
        Resultado<ContratacionDto> res = app.getContratacionController().solicitar(new SolicitarContratacionCommand(
                pubId, franja.id(), "Quiero contratar", "Aprender"));
        boolean deberia = p.estado().equals("PUBLICADA") && !cliente.equals(autorDe.get(pubId)) && !franja.ocupada()
                && p.cupoDisponible() > 0;
        if (res.esOk() != deberia) {
            violacion("solicitar: se esperaba " + (deberia ? "exito" : "error") + " y fue " + res);
        }
        if (res.esOk()) {
            Long id = res.valor().id();
            contratos.add(id);
            clienteDe.put(id, cliente);
            proveedorDe.put(id, autorDe.get(pubId));
            if (!"PENDIENTE".equals(res.valor().estado())) {
                violacion("una solicitud nueva nace " + res.valor().estado());
            }
            contar("ok solicitar");
        } else {
            contar("rechazado solicitar (" + res.codigo() + ")");
        }
        avanzar(res.esOk(), "solicitar");
    }

    static void accionSobreContratacion() {
        Long id = elegir(contratos);
        if (azar.nextInt(10) < 6) { // se prefieren las que ya tienen reserva, para llegar a los estados finales
            List<Long> avanzadas = new ArrayList<>();
            for (ContratacionDto c : mundo.contratos().values()) {
                if (CON_RESERVA.contains(c.estado())) {
                    avanzadas.add(c.id());
                }
            }
            if (!avanzadas.isEmpty()) {
                id = elegir(avanzadas);
            }
        }
        String cliente = clienteDe.get(id), proveedor = proveedorDe.get(id);
        String actor = azar.nextInt(10) < 8 ? (azar.nextBoolean() ? cliente : proveedor) : elegir(USUARIOS);
        boolean participa = actor.equals(cliente) || actor.equals(proveedor);
        ContratacionDto antes = mundo.contratos().get(id);
        como(actor);
        List<AccionContratacion> declaradas = participa ? app.getContratacionController().obtener(id).valor().acciones()
                : List.of();

        int op = azar.nextInt(10);
        // Segun el estado se favorece reprogramar o calificar, para ejercitar tambien esos caminos.
        if (REPROGRAMABLE.contains(antes.estado()) && azar.nextInt(10) < 3) {
            op = 6 + azar.nextInt(3);
        } else if ("COMPLETADA".equals(antes.estado()) && azar.nextInt(10) < 6) {
            op = 9;
        }
        // La mitad de las veces se elige una accion que el estado declara (y de esas, pocas veces cancelar o
        // rechazar), para que las contrataciones avancen hasta los estados finales.
        List<AccionContratacion> transicionesDeclaradas = new ArrayList<>(declaradas);
        transicionesDeclaradas.remove(AccionContratacion.CALIFICAR);
        List<AccionContratacion> queAvanzan = new ArrayList<>(transicionesDeclaradas);
        queAvanzan.remove(AccionContratacion.CANCELAR);
        queAvanzan.remove(AccionContratacion.RECHAZAR);
        AccionContratacion forzada = null;
        if (op < 6 && azar.nextBoolean() && !transicionesDeclaradas.isEmpty()) {
            forzada = (!queAvanzan.isEmpty() && azar.nextInt(10) < 8) ? elegir(queAvanzan)
                    : elegir(transicionesDeclaradas);
        }

        if (forzada != null || op < 6) {
            AccionContratacion a = forzada != null ? forzada : TRANSICIONES_C[op];
            transicion(id, actor, participa, antes, declaradas, a);
        } else if (op == 6) {
            proponer(id, actor, participa, antes);
        } else if (op < 9) {
            responder(id, actor, participa, antes, op == 7);
        } else {
            calificar(id, actor, participa, antes, declaradas);
        }
    }

    static void transicion(Long id, String actor, boolean participa, ContratacionDto antes,
                           List<AccionContratacion> declaradas, AccionContratacion a) {
        como(actor);
        Resultado<?> res = switch (a) {
            case ACEPTAR -> app.getContratacionController().aceptar(id);
            case RECHAZAR -> app.getContratacionController().rechazar(id, "No puedo");
            case PAGAR -> app.getContratacionController().pagar(id);
            case INICIAR -> app.getContratacionController().iniciar(id);
            case COMPLETAR -> app.getContratacionController().completar(id);
            default -> app.getContratacionController().cancelar(id, "Cambio de planes");
        };
        String que = a + " sobre " + antes.estado() + " por " + (participa ? rol(id, actor) : "tercero");
        if (TERMINAL_C.contains(antes.estado()) && res.esOk()) {
            violacion("estado terminal admitio una accion: " + que);
        }
        if (!participa) {
            if (res.esOk() || res.codigo() != ErrorCode.NO_AUTORIZADO) {
                violacion("un tercero no recibio NO_AUTORIZADO: " + que + " -> " + res);
            }
        } else if (!declaradas.contains(a)) {
            if (res.esOk()) {
                violacion("accion NO declarada pero ejecutada: " + que);
            }
        } else if (res.esError()) {
            if (res.codigo() == ErrorCode.TRANSICION_INVALIDA || res.codigo() == ErrorCode.NO_AUTORIZADO) {
                violacion("accion declarada pero rechazada por estado o permiso: " + que + " -> " + res);
            }
            contar("declarada pero fallo por otra regla: " + a + " (" + res.codigo() + ")");
        }
        avanzar(res.esOk(), que);
        if (res.esOk()) {
            contar("ok " + a + " desde " + antes.estado());
            ContratacionDto despues = mundo.contratos().get(id);
            if (!DESTINO_C.get(a).equals(despues.estado())) {
                violacion(que + " dejo el estado " + despues.estado());
            }
            historialSumaUno(que, antes.historial(), despues.historial(), despues.estado(), actor);
        }
    }

    static void proponer(Long id, String actor, boolean participa, ContratacionDto antes) {
        boolean deOtraPublicacion = azar.nextInt(10) == 0;
        PublicacionDto p = mundo.pubs().get(deOtraPublicacion ? elegir(pubs) : antes.publicacionId());
        if (p.franjas().isEmpty()) {
            return;
        }
        FranjaDto franja = elegir(p.franjas());
        como(actor);
        Resultado<ContratacionDto> res = app.getContratacionController().proponerReprogramacion(id, franja.id());
        boolean deberia = participa && REPROGRAMABLE.contains(antes.estado()) && antes.propuestaReprogramacion() == null
                && p.id().equals(antes.publicacionId()) && !franja.id().equals(antes.franjaId()) && !franja.ocupada();
        if (res.esOk() != deberia) {
            violacion("proponer reprogramacion sobre " + antes.estado() + ": se esperaba "
                    + (deberia ? "exito" : "error") + " y fue " + res);
        }
        avanzar(res.esOk(), "proponer reprogramacion");
        if (res.esOk()) {
            contar("ok PROPONER REPROGRAMACION desde " + antes.estado());
            ContratacionDto despues = mundo.contratos().get(id);
            if (!despues.estado().equals(antes.estado()) || !despues.franjaId().equals(antes.franjaId())
                    || despues.propuestaReprogramacion() == null) {
                violacion("proponer cambio el estado o la franja, o no dejo la propuesta");
            }
        }
    }

    static void responder(Long id, String actor, boolean participa, ContratacionDto antes, boolean acepta) {
        PropuestaReprogramacionDto propuesta = antes.propuestaReprogramacion();
        boolean deberia = participa && REPROGRAMABLE.contains(antes.estado()) && propuesta != null
                && !propuesta.propuestaPorId().equals(perfilId.get(actor));
        if (deberia && acepta) {
            deberia = !franja(antes.publicacionId(), propuesta.nuevaFranjaId()).ocupada();
        }
        como(actor);
        Resultado<ContratacionDto> res = app.getContratacionController().responderReprogramacion(id, acepta);
        if (res.esOk() != deberia) {
            violacion("responder reprogramacion (" + acepta + ") sobre " + antes.estado() + ": se esperaba "
                    + (deberia ? "exito" : "error") + " y fue " + res);
        }
        avanzar(res.esOk(), "responder reprogramacion");
        if (res.esOk()) {
            contar("ok RESPONDER REPROGRAMACION (" + (acepta ? "acepta" : "rechaza") + ")");
            ContratacionDto despues = mundo.contratos().get(id);
            Long esperada = acepta ? propuesta.nuevaFranjaId() : antes.franjaId();
            if (despues.propuestaReprogramacion() != null || !despues.franjaId().equals(esperada)
                    || !despues.estado().equals(antes.estado())) {
                violacion("responder reprogramacion dejo mal la contratacion " + id);
            }
        }
    }

    static void calificar(Long id, String actor, boolean participa, ContratacionDto antes,
                          List<AccionContratacion> declaradas) {
        String clave = id + "|" + actor;
        boolean deberia = participa && "COMPLETADA".equals(antes.estado()) && !calificadas.contains(clave);
        if (participa && calificadas.contains(clave) && declaradas.contains(AccionContratacion.CALIFICAR)) {
            contar("observacion: CALIFICAR sigue declarada para quien ya califico");
        }
        como(actor);
        Resultado<?> res = app.getCalificacionController().calificar(new CalificarCommand(id, 1 + azar.nextInt(5),
                "Comentario"));
        if (res.esOk() != deberia) {
            violacion("calificar sobre " + antes.estado() + ": se esperaba " + (deberia ? "exito" : "error")
                    + " y fue " + res);
        }
        if (res.esOk()) {
            calificadas.add(clave);
            contar("ok CALIFICAR");
        }
        avanzar(true, "calificar"); // calificar no cambia la contratacion; la foto no incluye calificaciones
    }

    static void accionSobrePublicacion() {
        Long id = elegir(pubs);
        String autor = autorDe.get(id);
        String actor = azar.nextInt(10) < 8 ? autor : elegir(USUARIOS);
        PublicacionDto antes = mundo.pubs().get(id);
        int r = azar.nextInt(100);
        como(actor);
        if (r < 20) {
            LocalDateTime inicio = LocalDateTime.now().plusDays(5).plusHours(franjasCreadas++).withNano(0);
            Resultado<PublicacionDto> res = app.getPublicacionController().agregarFranja(id, inicio,
                    inicio.plusHours(1));
            boolean deberia = actor.equals(autor) && !TERMINAL_P.contains(antes.estado());
            if (res.esOk() != deberia) {
                violacion("agregar franja a " + antes.estado() + ": se esperaba " + (deberia ? "exito" : "error")
                        + " y fue " + res);
            }
            avanzar(res.esOk(), "agregar franja");
            return;
        }
        AccionPublicacion a = r < 40 ? AccionPublicacion.PUBLICAR : r < 68 ? AccionPublicacion.PAUSAR
                : r < 96 ? AccionPublicacion.REANUDAR : r < 98 ? AccionPublicacion.CERRAR : AccionPublicacion.CANCELAR;
        Resultado<PublicacionDto> res = switch (a) {
            case PUBLICAR -> app.getPublicacionController().publicar(id);
            case PAUSAR -> app.getPublicacionController().pausar(id);
            case REANUDAR -> app.getPublicacionController().reanudar(id);
            case CERRAR -> app.getPublicacionController().cerrar(id);
            case CANCELAR -> app.getPublicacionController().cancelar(id, "Ya no la ofrezco");
        };
        String que = a + " sobre publicacion " + antes.estado() + " por " + (actor.equals(autor) ? "autor" : "otro");
        if (TERMINAL_P.contains(antes.estado()) && res.esOk()) {
            violacion("publicacion terminal admitio una accion: " + que);
        }
        if (!actor.equals(autor)) {
            if (res.esOk()) {
                violacion("alguien que no es el autor cambio el estado: " + que);
            } else {
                contar("no autor sobre publicacion (" + res.codigo() + ")");
            }
        } else if (!antes.accionesDelAutor().contains(a)) {
            if (res.esOk()) {
                violacion("accion NO declarada pero ejecutada: " + que);
            }
        } else if (res.esError()) {
            if (res.codigo() == ErrorCode.TRANSICION_INVALIDA || res.codigo() == ErrorCode.NO_AUTORIZADO) {
                violacion("accion declarada pero rechazada por estado o permiso: " + que + " -> " + res);
            }
            contar("declarada pero fallo por otra regla: " + a + " publicacion (" + res.codigo() + ")");
        }
        avanzar(res.esOk(), que);
        if (res.esOk()) {
            contar("ok publicacion " + a + " desde " + antes.estado());
            PublicacionDto despues = mundo.pubs().get(id);
            if (!DESTINO_P.get(a).equals(despues.estado())) {
                violacion(que + " dejo el estado " + despues.estado());
            }
            historialSumaUno(que, antes.historial(), despues.historial(), despues.estado(), actor);
        }
    }

    // ---------- invariantes ----------

    /** Vuelve a leer el mundo, compara con el anterior y verifica los invariantes. */
    static void avanzar(boolean huboExito, String que) {
        Mundo previo = mundo;
        mundo = leer();
        if (!huboExito && previo != null && !previo.foto().equals(mundo.foto())) {
            violacion("la accion fallo pero cambio algo: " + que);
        }
        invariantes(mundo, previo);
    }

    static void invariantes(Mundo m, Mundo previo) {
        for (PublicacionDto p : m.pubs().values()) {
            if (p.cupoDisponible() < 0 || p.cupoDisponible() > p.cupo()) {
                violacion("cupo fuera de rango en publicacion " + p.id() + ": " + p.cupoDisponible() + " de "
                        + p.cupo());
            }
            int reservas = 0;
            Map<Long, Integer> porFranja = new HashMap<>();
            for (ContratacionDto c : m.contratos().values()) {
                if (c.publicacionId().equals(p.id()) && CON_RESERVA.contains(c.estado())) {
                    reservas++;
                    porFranja.merge(c.franjaId(), 1, Integer::sum);
                }
            }
            if (reservas != p.cupo() - p.cupoDisponible()) {
                violacion("publicacion " + p.id() + ": cupo ocupado " + (p.cupo() - p.cupoDisponible()) + " pero hay "
                        + reservas + " contrataciones con reserva");
            }
            for (FranjaDto f : p.franjas()) {
                int usos = porFranja.getOrDefault(f.id(), 0);
                if (usos > 1) {
                    violacion("franja " + f.id() + " reservada por " + usos + " contrataciones");
                }
                if (f.ocupada() != (usos == 1)) {
                    violacion("franja " + f.id() + " ocupada=" + f.ocupada() + " con " + usos + " reservas");
                }
            }
            if (TERMINAL_P.contains(p.estado()) && !p.accionesDelAutor().isEmpty()) {
                violacion("publicacion terminal declara acciones: " + p.accionesDelAutor());
            }
            ultimoHistorial("publicacion " + p.id(), p.historial(), p.estado());
            if (previo != null && previo.pubs().containsKey(p.id())
                    && previo.pubs().get(p.id()).historial().size() > p.historial().size()) {
                violacion("el historial de la publicacion " + p.id() + " se achico");
            }
        }
        BigDecimal retenido = BigDecimal.ZERO;
        for (ContratacionDto c : m.contratos().values()) {
            if (TERMINAL_C.contains(c.estado())) {
                for (AccionContratacion a : c.acciones()) {
                    if (a != AccionContratacion.CALIFICAR) {
                        violacion("contratacion terminal " + c.estado() + " declara " + a);
                    }
                }
            }
            if (!REPROGRAMABLE.contains(c.estado()) && c.propuestaReprogramacion() != null
                    && propuestasColgadas.add(etiqueta.split(",")[0] + "#" + c.id())) {
                violacion("contratacion " + c.id() + " en " + c.estado()
                        + " conserva una propuesta de reprogramacion pendiente");
            }
            ultimoHistorial("contratacion " + c.id(), c.historial(), c.estado());
            if (previo != null && previo.contratos().containsKey(c.id())
                    && previo.contratos().get(c.id()).historial().size() > c.historial().size()) {
                violacion("el historial de la contratacion " + c.id() + " se achico");
            }
            if (RETENIDO.contains(c.estado())) {
                retenido = retenido.add(c.precioFinal());
            }
        }
        BigDecimal enWallets = BigDecimal.ZERO;
        for (BigDecimal saldo : m.saldos().values()) {
            if (saldo.signum() < 0) {
                violacion("saldo negativo: " + saldo);
            }
            enWallets = enWallets.add(saldo);
        }
        if (enWallets.add(retenido).compareTo(BigDecimal.valueOf(400)) != 0) {
            violacion("SkillCoins: wallets " + enWallets + " + retenido " + retenido + " != 400");
        }
    }

    static void ultimoHistorial(String de, List<HistorialDto> historial, String estado) {
        if (historial.isEmpty()) {
            violacion(de + " no tiene historial");
            return;
        }
        HistorialDto ultimo = historial.get(historial.size() - 1);
        if (!estado.equals(ultimo.estadoNuevo())) {
            violacion(de + ": estado " + estado + " pero el ultimo historial dice " + ultimo.estadoNuevo());
        }
    }

    static void historialSumaUno(String que, List<HistorialDto> antes, List<HistorialDto> despues, String estado,
                                 String actor) {
        if (despues.size() != antes.size() + 1) {
            violacion(que + ": el historial paso de " + antes.size() + " a " + despues.size() + " lineas");
            return;
        }
        HistorialDto h = despues.get(despues.size() - 1);
        if (!estado.equals(h.estadoNuevo()) || !perfilId.get(actor).equals(h.actorId()) || h.fecha() == null
                || h.motivo() == null || h.motivo().isBlank()) {
            violacion(que + ": linea de historial incompleta o incorrecta: " + h);
        }
    }

    static Mundo leer() {
        StringBuilder foto = new StringBuilder();
        Map<Long, PublicacionDto> publicaciones = new LinkedHashMap<>();
        for (Long id : pubs) {
            como(autorDe.get(id));
            PublicacionDto p = app.getPublicacionController().obtener(id).valor();
            publicaciones.put(id, p);
            foto.append("P").append(id).append(':').append(p.estado()).append(',').append(p.cupoDisponible())
                    .append(',').append(p.historial().size());
            for (FranjaDto f : p.franjas()) {
                foto.append(',').append(f.id()).append(f.ocupada() ? "x" : "o");
            }
            foto.append(';');
        }
        Map<Long, ContratacionDto> contrataciones = new LinkedHashMap<>();
        for (Long id : contratos) {
            como(clienteDe.get(id));
            ContratacionDto c = app.getContratacionController().obtener(id).valor();
            contrataciones.put(id, c);
            foto.append("C").append(id).append(':').append(c.estado()).append(',').append(c.franjaId()).append(',')
                    .append(c.historial().size()).append(',')
                    .append(c.propuestaReprogramacion() == null ? "-" : c.propuestaReprogramacion().nuevaFranjaId())
                    .append(';');
        }
        Map<String, BigDecimal> saldos = new LinkedHashMap<>();
        for (String u : USUARIOS) {
            como(u);
            BigDecimal saldo = app.getWalletController().consultar().valor().saldo();
            saldos.put(u, saldo);
            foto.append(u).append('=').append(saldo.toPlainString()).append(';');
        }
        return new Mundo(contrataciones, publicaciones, saldos, foto.toString());
    }

    // ---------- observaciones puntuales (no son violaciones: describen el comportamiento actual) ----------

    static void observaciones() {
        iniciar();
        try {
            etiqueta = "observaciones";
            // O1: una propuesta de reprogramacion pendiente sobrevive a "iniciar".
            Long pub = nuevaPublicacion(ANA, 3, 4, "5.00", true);
            mundo = leer();
            List<FranjaDto> fr = mundo.pubs().get(pub).franjas();
            Long c1 = solicitar(BRUNO, pub, fr.get(0).id());
            como(ANA);
            app.getContratacionController().aceptar(c1);
            como(BRUNO);
            app.getContratacionController().pagar(c1);
            app.getContratacionController().proponerReprogramacion(c1, fr.get(1).id());
            como(ANA);
            app.getContratacionController().iniciar(c1);
            ContratacionDto enCurso = app.getContratacionController().obtener(c1).valor();
            observar("O1", "tras INICIAR con propuesta pendiente: estado " + enCurso.estado() + ", propuesta "
                    + (enCurso.propuestaReprogramacion() == null ? "limpia" : "SIGUE PENDIENTE"));
            observar("O1", "responderla en EN_CURSO -> "
                    + app.getContratacionController().responderReprogramacion(c1, false).codigo());

            // O2: dos solicitudes del mismo cliente para la misma franja.
            Long c2 = solicitar(CECILIA, pub, fr.get(2).id());
            Long c3 = solicitar(CECILIA, pub, fr.get(2).id());
            observar("O2", "segunda solicitud identica del mismo cliente: " + (c3 == null ? "rechazada" : "ACEPTADA como otra PENDIENTE"));
            como(ANA);
            app.getContratacionController().aceptar(c2);
            if (c3 != null) {
                Resultado<ContratacionDto> segunda = app.getContratacionController().aceptar(c3);
                como(CECILIA);
                observar("O2", "aceptar la segunda -> " + segunda.codigo() + "; queda "
                        + app.getContratacionController().obtener(c3).valor().estado());
            }

            // O3: franjas solapadas en la misma publicacion.
            como(ANA);
            LocalDateTime inicio = fr.get(3).inicio();
            Resultado<PublicacionDto> solapada = app.getPublicacionController().agregarFranja(pub, inicio,
                    inicio.plusHours(1));
            observar("O3", "agregar una franja con el mismo horario que otra: " + (solapada.esOk() ? "ACEPTADA"
                    : "rechazada " + solapada.codigo()));

            // O4: aceptar una solicitud pendiente con la publicacion pausada y con la publicacion cancelada.
            Long pub2 = nuevaPublicacion(BRUNO, 2, 2, "0.00", true);
            mundo = leer();
            List<FranjaDto> fr2 = mundo.pubs().get(pub2).franjas();
            Long c4 = solicitar(ANA, pub2, fr2.get(0).id());
            Long c5 = solicitar(CECILIA, pub2, fr2.get(1).id());
            como(BRUNO);
            app.getPublicacionController().pausar(pub2);
            observar("O4", "aceptar con la publicacion PAUSADA -> "
                    + describir(app.getContratacionController().aceptar(c4)));
            app.getPublicacionController().cancelar(pub2, "La retiro");
            observar("O4", "aceptar con la publicacion CANCELADA -> "
                    + describir(app.getContratacionController().aceptar(c5)));
            como(ANA);
            observar("O4", "pagar la ACEPTADA de una publicacion CANCELADA -> "
                    + describir(app.getContratacionController().pagar(c4)));

            // O5: CALIFICAR sigue entre las acciones despues de calificar.
            como(ANA);
            app.getContratacionController().completar(c1);
            como(BRUNO);
            app.getCalificacionController().calificar(new CalificarCommand(c1, 5, "Muy bien"));
            observar("O5", "acciones del cliente despues de calificar: "
                    + app.getContratacionController().obtener(c1).valor().acciones() + "; calificar de nuevo -> "
                    + app.getCalificacionController().calificar(new CalificarCommand(c1, 4, "Otra")).codigo());
        } catch (RuntimeException e) {
            violacion("excepcion en observaciones: " + e);
        } finally {
            cerrar();
        }
    }

    static String describir(Resultado<ContratacionDto> r) {
        return r.esOk() ? "OK (" + r.valor().estado() + ")" : String.valueOf(r.codigo());
    }

    static void observar(String id, String texto) {
        System.out.println("[SECUENCIAS] observacion " + id + ": " + texto);
    }

    // ---------- soporte ----------

    static void iniciar() {
        ctx = BaseDeDatos.iniciarH2();
        identidad = IdentidadMockAdapter.conUsuariosDemo();
        app = Composicion.crear(ctx, identidad, SkillSwapConfig.porDefecto());
        pubs.clear();
        autorDe.clear();
        contratos.clear();
        clienteDe.clear();
        proveedorDe.clear();
        calificadas.clear();
        perfilId.clear();
        mundo = null;
        for (String usuario : USUARIOS) {
            como(usuario);
            TipoPerfil tipo = usuario.equals(TECH) ? TipoPerfil.ORGANIZACION : TipoPerfil.PROFESIONAL;
            perfilId.put(usuario, app.getPerfilController().registrar(new RegistrarPerfilCommand(usuario,
                    "Perfil " + usuario, tipo, "UTC", null, Set.of(), Set.of(), List.of(),
                    CanalNotificacion.INTERNA)).valor().id());
        }
    }

    static void cerrar() {
        identidad.cerrarSesion();
        app.cerrar();
    }

    static Long nuevaPublicacion(String autor, int cupo, int cantidadFranjas, String precio, boolean publicar) {
        List<FranjaCommand> franjas = new ArrayList<>();
        for (int i = 0; i < cantidadFranjas; i++) {
            LocalDateTime inicio = LocalDateTime.now().plusDays(5).plusHours(franjasCreadas++).withNano(0);
            franjas.add(new FranjaCommand(inicio, inicio.plusHours(1)));
        }
        como(autor);
        Long id = app.getPublicacionController().crearBorrador(new CrearPublicacionCommand(TipoPublicacion.MENTORIA,
                "Mentoria", "Descripcion", "programacion", Modalidad.REMOTA, 60, cupo, new BigDecimal(precio),
                Set.of("java"), Set.of("java"), TipoPoliticaCancelacion.FLEXIBLE, franjas)).valor().id();
        pubs.add(id);
        autorDe.put(id, autor);
        if (publicar) {
            app.getPublicacionController().publicar(id); // sin franjas falla: queda en BORRADOR
        }
        return id;
    }

    static Long solicitar(String cliente, Long pubId, Long franjaId) {
        como(cliente);
        Resultado<ContratacionDto> r = app.getContratacionController().solicitar(new SolicitarContratacionCommand(
                pubId, franjaId, "Quiero contratar", "Aprender"));
        return r.esOk() ? r.valor().id() : null;
    }

    static FranjaDto franja(Long pubId, Long franjaId) {
        for (FranjaDto f : mundo.pubs().get(pubId).franjas()) {
            if (f.id().equals(franjaId)) {
                return f;
            }
        }
        throw new IllegalStateException("No existe la franja " + franjaId);
    }

    static String rol(Long contratacionId, String actor) {
        return actor.equals(clienteDe.get(contratacionId)) ? "cliente" : "proveedor";
    }

    static void como(String usuario) {
        identidad.iniciarSesion(usuario);
    }

    static <T> T elegir(List<T> lista) {
        return lista.get(azar.nextInt(lista.size()));
    }

    static String elegir(String[] arreglo) {
        return arreglo[azar.nextInt(arreglo.length)];
    }

    static void contar(String clave) {
        conteo.merge(clave, 1, Integer::sum);
    }

    static void violacion(String detalle) {
        violaciones.add(etiqueta + " -> " + detalle);
    }
}
