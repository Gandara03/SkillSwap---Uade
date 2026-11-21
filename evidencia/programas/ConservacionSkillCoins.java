import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.presentation.Resultado;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Evidencia del area 1 de la revision: el total de SkillCoins se conserva en cada flujo.
 *
 * Arma el sistema real (Composicion + H2 + Flyway + JPA) y solo habla con los controllers. Despues de CADA paso
 * comprueba:
 *   1. suma de saldos + lo retenido en contrataciones CONFIRMADA/EN_CURSO == total emitido (bonos iniciales);
 *   2. ningun saldo es negativo;
 *   3. cada saldo coincide con (creditos - debitos) de sus movimientos y con el saldoResultante del ultimo.
 *
 * No es parte del repo. Se compila contra las clases del proyecto y se ejecuta con su classpath.
 */
public class ConservacionSkillCoins {

    static final String ANA = "u-ana", BRUNO = "u-bruno", CECILIA = "u-cecilia", TECH = "u-techcorp";
    static final String[] USUARIOS = {ANA, BRUNO, CECILIA, TECH};
    static final Set<String> RETENIDO = Set.of("CONFIRMADA", "EN_CURSO");

    static JpaContexto ctx;
    static IdentidadMockAdapter identidad;
    static Composicion app;
    static BigDecimal totalEmitido;
    /** contratacionId -> un participante (para poder consultarla). */
    static final Map<Long, String> contrataciones = new LinkedHashMap<>();

    static int verificaciones = 0;
    static final List<String> fallas = new ArrayList<>();
    static String escenarioActual = "";

    public static void main(String[] args) {
        escenario("A. Flujo feliz: pagar, iniciar, completar", () -> {
            ContratacionDto c = pagada(ANA, BRUNO, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            completar(ANA, c.id());
            saldoEs(BRUNO, "70.00");
            saldoEs(ANA, "130.00");
        });
        escenario("B. Cancelar ACEPTADA sin pagar: no se mueve nada", () -> {
            PublicacionDto p = publicar(ANA, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            ContratacionDto c = solicitar(BRUNO, p);
            aceptar(ANA, c.id());
            ResultadoCancelacionDto r = cancelar(BRUNO, c.id());
            igual("reintegro", r.reintegroCliente(), "0.00");
            igual("compensacion", r.compensacionProveedor(), "0.00");
            saldoEs(BRUNO, "100.00");
            saldoEs(ANA, "100.00");
        });
        escenario("C. Rechazar una solicitud: no se mueve nada", () -> {
            PublicacionDto p = publicar(ANA, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            ContratacionDto c = solicitar(BRUNO, p);
            como(ANA);
            paso("rechazar", app.getContratacionController().rechazar(c.id(), "No puedo"));
            saldoEs(BRUNO, "100.00");
        });
        escenario("D. Flexible, cliente cancela con 30 h (>= 24): reintegro 100 %", () ->
                cancelacion(TipoPoliticaCancelacion.FLEXIBLE, Duration.ofHours(30), "33.33", true, "33.33", "0.00"));
        escenario("E. Flexible, cliente cancela con 10 h (2 a 24): 50 % con redondeo", () ->
                cancelacion(TipoPoliticaCancelacion.FLEXIBLE, Duration.ofHours(10), "33.33", true, "16.67", "16.66"));
        escenario("F. Flexible, cliente cancela con 1 h (< 2): reintegro 0 %", () ->
                cancelacion(TipoPoliticaCancelacion.FLEXIBLE, Duration.ofMinutes(65), "33.33", true, "0.00", "33.33"));
        escenario("G. Estricta, cliente cancela con 80 h (>= 72): 100 %", () ->
                cancelacion(TipoPoliticaCancelacion.ESTRICTA, Duration.ofHours(80), "45.55", true, "45.55", "0.00"));
        escenario("H. Estricta, cliente cancela con 60 h (48 a 72): 50 %", () ->
                cancelacion(TipoPoliticaCancelacion.ESTRICTA, Duration.ofHours(60), "45.55", true, "22.78", "22.77"));
        escenario("I. Estricta, cliente cancela con 30 h (< 48): 0 %", () ->
                cancelacion(TipoPoliticaCancelacion.ESTRICTA, Duration.ofHours(30), "45.55", true, "0.00", "45.55"));
        escenario("J. Cancela el PROVEEDOR con 1 h, politica estricta: cliente recupera 100 %", () ->
                cancelacion(TipoPoliticaCancelacion.ESTRICTA, Duration.ofMinutes(65), "45.55", false, "45.55", "0.00"));
        escenario("K. Un centavo al 50 %: 0.01 al cliente y 0.00 al proveedor", () ->
                cancelacion(TipoPoliticaCancelacion.FLEXIBLE, Duration.ofHours(10), "0.01", true, "0.01", "0.00"));
        escenario("L. Saldo insuficiente: el pago falla y no cambia nada", () -> {
            PublicacionDto p = publicar(ANA, "150.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            ContratacionDto c = solicitar(BRUNO, p);
            aceptar(ANA, c.id());
            como(BRUNO);
            falla("pagar sin saldo", app.getContratacionController().pagar(c.id()), ErrorCode.SALDO_INSUFICIENTE);
            saldoEs(BRUNO, "100.00");
            estadoEs(BRUNO, c.id(), "ACEPTADA");
        });
        escenario("M. Doble pago: el segundo se rechaza y no cobra de nuevo", () -> {
            ContratacionDto c = pagada(ANA, BRUNO, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            como(BRUNO);
            falla("segundo pago", app.getContratacionController().pagar(c.id()), ErrorCode.TRANSICION_INVALIDA);
            saldoEs(BRUNO, "70.00");
        });
        escenario("N. Doble completar y cancelar una COMPLETADA: no acredita de nuevo", () -> {
            ContratacionDto c = pagada(ANA, BRUNO, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            completar(ANA, c.id());
            como(ANA);
            falla("segundo completar", app.getContratacionController().completar(c.id()),
                    ErrorCode.TRANSICION_INVALIDA);
            falla("cancelar completada", app.getContratacionController().cancelar(c.id(), "tarde"),
                    ErrorCode.TRANSICION_INVALIDA);
            saldoEs(ANA, "130.00");
            saldoEs(BRUNO, "70.00");
        });
        escenario("O. Doble cancelacion: la segunda no reintegra de nuevo", () -> {
            ContratacionDto c = pagada(ANA, BRUNO, "30.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            cancelar(BRUNO, c.id());
            como(BRUNO);
            falla("segunda cancelacion", app.getContratacionController().cancelar(c.id(), "otra vez"),
                    ErrorCode.TRANSICION_INVALIDA);
            saldoEs(BRUNO, "100.00");
            saldoEs(ANA, "100.00");
        });
        escenario("P. Promociones acumuladas 99.99 -10 % -5: precio 84.99", () -> {
            PublicacionDto p = publicar(ANA, "99.99", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            promocion(ANA, p.id(), TipoPromocion.PORCENTAJE, "10");
            promocion(ANA, p.id(), TipoPromocion.MONTO_FIJO, "5");
            ContratacionDto c = solicitar(BRUNO, p);
            igual("precio final", c.precioFinal(), "84.99");
            aceptar(ANA, c.id());
            pagar(BRUNO, c.id());
            completar(ANA, c.id());
            saldoEs(BRUNO, "15.01");
            saldoEs(ANA, "184.99");
        });
        escenario("Q. Promociones que superan el precio: total 0, se paga y completa sin movimientos", () -> {
            PublicacionDto p = publicar(ANA, "20.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            promocion(ANA, p.id(), TipoPromocion.MONTO_FIJO, "15");
            promocion(ANA, p.id(), TipoPromocion.MONTO_FIJO, "15");
            ContratacionDto c = solicitar(BRUNO, p);
            igual("precio final", c.precioFinal(), "0.00");
            aceptar(ANA, c.id());
            pagar(BRUNO, c.id());
            completar(ANA, c.id());
            saldoEs(BRUNO, "100.00");
            saldoEs(ANA, "100.00");
        });
        escenario("R. Precio 0 pagado y cancelado: reparto 0 / 0", () -> {
            PublicacionDto p = publicar(ANA, "0.00", TipoPoliticaCancelacion.ESTRICTA, Duration.ofHours(60));
            ContratacionDto c = solicitar(BRUNO, p);
            aceptar(ANA, c.id());
            pagar(BRUNO, c.id());
            ResultadoCancelacionDto r = cancelar(BRUNO, c.id());
            igual("reintegro", r.reintegroCliente(), "0.00");
            igual("compensacion", r.compensacionProveedor(), "0.00");
        });
        escenario("S. La promocion agregada despues de solicitar no cambia el precio congelado", () -> {
            PublicacionDto p = publicar(ANA, "40.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            ContratacionDto c = solicitar(BRUNO, p);
            promocion(ANA, p.id(), TipoPromocion.PORCENTAJE, "50");
            aceptar(ANA, c.id());
            ContratacionDto pagada = pagar(BRUNO, c.id());
            igual("precio congelado", pagada.precioFinal(), "40.00");
            saldoEs(BRUNO, "60.00");
        });
        escenario("T. Reprogramar una CONFIRMADA y cancelar: la anticipacion usa la franja nueva", () -> {
            PublicacionDto p = publicar(ANA, "40.00", TipoPoliticaCancelacion.FLEXIBLE,
                    Duration.ofDays(3), Duration.ofHours(10));
            ContratacionDto c = solicitar(BRUNO, p); // franja 0: en 3 dias
            aceptar(ANA, c.id());
            pagar(BRUNO, c.id());
            como(BRUNO);
            paso("proponer", app.getContratacionController().proponerReprogramacion(c.id(), p.franjas().get(1).id()));
            como(ANA);
            paso("responder", app.getContratacionController().responderReprogramacion(c.id(), true));
            ResultadoCancelacionDto r = cancelar(BRUNO, c.id()); // ahora faltan 10 h: 50 %
            igual("reintegro", r.reintegroCliente(), "20.00");
            igual("compensacion", r.compensacionProveedor(), "20.00");
        });
        escenario("V. (observacion) Segundo pago de una CONFIRMADA cuando el saldo ya no alcanza", () -> {
            ContratacionDto c = pagada(ANA, BRUNO, "60.00", TipoPoliticaCancelacion.FLEXIBLE, Duration.ofDays(3));
            como(BRUNO);
            Resultado<ContratacionDto> segundo = app.getContratacionController().pagar(c.id());
            verificar("segundo pago sin saldo");
            System.out.println("[CONSERVACION]   observacion V: el segundo pago devuelve " + segundo.codigo());
            saldoEs(BRUNO, "40.00");
        });
        escenario("U. 60 contrataciones aleatorias (semilla 20261111) entre 4 perfiles", ConservacionSkillCoins::aleatorio);

        System.out.println();
        System.out.println("[CONSERVACION] verificaciones del invariante: " + verificaciones);
        if (fallas.isEmpty()) {
            System.out.println("[CONSERVACION] RESULTADO: OK, el total de SkillCoins se conserva en todos los pasos");
        } else {
            System.out.println("[CONSERVACION] RESULTADO: " + fallas.size() + " FALLAS");
            fallas.forEach(f -> System.out.println("[CONSERVACION]   - " + f));
            System.exit(1);
        }
    }

    // ---------- escenario aleatorio ----------

    static void aleatorio() {
        Random azar = new Random(20261111L);
        String[] proveedores = {ANA, BRUNO, CECILIA};
        Duration[] anticipaciones = {Duration.ofMinutes(65), Duration.ofHours(10), Duration.ofHours(30),
                Duration.ofHours(60), Duration.ofHours(80)};
        int pagadas = 0, sinSaldo = 0, canceladas = 0, completadas = 0;
        for (int i = 0; i < 60; i++) {
            String proveedor = proveedores[azar.nextInt(proveedores.length)];
            String cliente;
            do {
                cliente = USUARIOS[azar.nextInt(USUARIOS.length)];
            } while (cliente.equals(proveedor));
            String precio = BigDecimal.valueOf(azar.nextInt(6001), 2).toPlainString(); // 0.00 a 60.00
            TipoPoliticaCancelacion politica = azar.nextBoolean() ? TipoPoliticaCancelacion.FLEXIBLE
                    : TipoPoliticaCancelacion.ESTRICTA;
            PublicacionDto p = publicar(proveedor, precio, politica, anticipaciones[azar.nextInt(5)]);
            if (azar.nextInt(3) == 0) {
                promocion(proveedor, p.id(), TipoPromocion.PORCENTAJE, String.valueOf(1 + azar.nextInt(100)));
            }
            if (azar.nextInt(4) == 0) {
                promocion(proveedor, p.id(), TipoPromocion.MONTO_FIJO,
                        BigDecimal.valueOf(1 + azar.nextInt(3000), 2).toPlainString());
            }
            ContratacionDto c = solicitar(cliente, p);
            aceptar(proveedor, c.id());
            como(cliente);
            Resultado<ContratacionDto> pago = app.getContratacionController().pagar(c.id());
            verificar("pagar (aleatorio " + i + ")");
            if (pago.esError()) {
                if (pago.codigo() != ErrorCode.SALDO_INSUFICIENTE) {
                    fallar("pago aleatorio " + i + " fallo con " + pago);
                }
                sinSaldo++;
                cancelar(azar.nextBoolean() ? cliente : proveedor, c.id());
                continue;
            }
            pagadas++;
            switch (azar.nextInt(5)) {
                case 0 -> { completar(proveedor, c.id()); completadas++; }
                case 1 -> { repartoSuma(cancelar(cliente, c.id()), c.precioFinal()); canceladas++; }
                case 2 -> { repartoSuma(cancelar(proveedor, c.id()), c.precioFinal()); canceladas++; }
                case 3 -> { como(proveedor); paso("iniciar", app.getContratacionController().iniciar(c.id())); }
                default -> { } // queda CONFIRMADA: el monto sigue retenido
            }
        }
        System.out.println("[CONSERVACION]   aleatorio: pagadas=" + pagadas + ", sin saldo=" + sinSaldo
                + ", canceladas=" + canceladas + ", completadas=" + completadas);
    }

    static void repartoSuma(ResultadoCancelacionDto r, BigDecimal precio) {
        if (r == null) {
            return;
        }
        if (r.reintegroCliente().add(r.compensacionProveedor()).compareTo(precio) != 0) {
            fallar("reintegro " + r.reintegroCliente() + " + compensacion " + r.compensacionProveedor()
                    + " != precio " + precio);
        }
        if (r.reintegroCliente().signum() < 0 || r.compensacionProveedor().signum() < 0) {
            fallar("reparto negativo: " + r.reintegroCliente() + " / " + r.compensacionProveedor());
        }
    }

    // ---------- el invariante ----------

    static void verificar(String paso) {
        String sesionDelPaso = sesionActual; // la verificacion cambia de usuario: al final se restaura
        verificaciones++;
        BigDecimal enWallets = BigDecimal.ZERO;
        for (String usuario : USUARIOS) {
            como(usuario);
            BigDecimal saldo = app.getWalletController().consultar().valor().saldo();
            enWallets = enWallets.add(saldo);
            if (saldo.signum() < 0) {
                fallar(paso + ": saldo negativo de " + usuario + " = " + saldo);
            }
            BigDecimal porMovimientos = BigDecimal.ZERO;
            BigDecimal ultimoSaldoResultante = null;
            int pagina = 0;
            Pagina<MovimientoDto> movimientos;
            do {
                movimientos = app.getWalletController().movimientos(PaginaRequest.de(pagina++, 100)).valor();
                for (MovimientoDto m : movimientos.contenido()) {
                    if (ultimoSaldoResultante == null) {
                        ultimoSaldoResultante = m.saldoResultante(); // vienen del mas reciente al mas antiguo
                    }
                    porMovimientos = m.tipo().esCredito() ? porMovimientos.add(m.monto())
                            : porMovimientos.subtract(m.monto());
                }
            } while (movimientos.tieneSiguiente());
            if (porMovimientos.compareTo(saldo) != 0) {
                fallar(paso + ": saldo de " + usuario + " = " + saldo + " pero sus movimientos suman " + porMovimientos);
            }
            if (ultimoSaldoResultante != null && ultimoSaldoResultante.compareTo(saldo) != 0) {
                fallar(paso + ": ultimo saldoResultante de " + usuario + " = " + ultimoSaldoResultante
                        + " y el saldo es " + saldo);
            }
        }
        BigDecimal retenido = BigDecimal.ZERO;
        for (Map.Entry<Long, String> e : contrataciones.entrySet()) {
            como(e.getValue());
            ContratacionDto c = app.getContratacionController().obtener(e.getKey()).valor();
            if (RETENIDO.contains(c.estado())) {
                retenido = retenido.add(c.precioFinal());
            }
        }
        if (enWallets.add(retenido).compareTo(totalEmitido) != 0) {
            fallar(paso + ": wallets " + enWallets + " + retenido " + retenido + " != emitido " + totalEmitido);
        }
        como(sesionDelPaso);
    }

    // ---------- armado y pasos ----------

    static void escenario(String nombre, Runnable cuerpo) {
        escenarioActual = nombre;
        int fallasAntes = fallas.size();
        ctx = BaseDeDatos.iniciarH2();
        identidad = IdentidadMockAdapter.conUsuariosDemo();
        app = Composicion.crear(ctx, identidad, SkillSwapConfig.porDefecto());
        contrataciones.clear();
        try {
            for (String usuario : USUARIOS) {
                como(usuario);
                TipoPerfil tipo = usuario.equals(TECH) ? TipoPerfil.ORGANIZACION : TipoPerfil.PROFESIONAL;
                exigir("registrar " + usuario, app.getPerfilController().registrar(new RegistrarPerfilCommand(usuario,
                        "Perfil " + usuario, tipo, "UTC", null, Set.of(), Set.of(), List.of(),
                        CanalNotificacion.INTERNA)));
            }
            totalEmitido = BigDecimal.valueOf(100L * USUARIOS.length);
            verificar("registro");
            cuerpo.run();
        } catch (RuntimeException e) {
            fallar("excepcion: " + e);
        } finally {
            identidad.cerrarSesion();
            app.cerrar();
        }
        System.out.println("[CONSERVACION] " + (fallas.size() == fallasAntes ? "OK    " : "FALLA ") + nombre);
    }

    /** Publica una mentoria del autor con una franja por cada anticipacion pedida. */
    static PublicacionDto publicar(String autor, String precio, TipoPoliticaCancelacion politica,
                                   Duration... desdeAhora) {
        List<FranjaCommand> franjas = new ArrayList<>();
        for (Duration d : desdeAhora) {
            LocalDateTime inicio = LocalDateTime.now().plus(d).withNano(0);
            franjas.add(new FranjaCommand(inicio, inicio.plusHours(1)));
        }
        como(autor);
        PublicacionDto borrador = exigir("crear borrador", app.getPublicacionController().crearBorrador(
                new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Mentoria", "Descripcion", "programacion",
                        Modalidad.REMOTA, 60, 5, new BigDecimal(precio), Set.of("java"), Set.of("java"), politica,
                        franjas)));
        return exigir("publicar", app.getPublicacionController().publicar(borrador.id()));
    }

    static void promocion(String autor, Long publicacionId, TipoPromocion tipo, String valor) {
        como(autor);
        exigir("promocion", app.getPublicacionController().agregarPromocion(new CrearPromocionCommand(publicacionId,
                "Promo " + tipo + " " + valor, tipo, new BigDecimal(valor), null, null)));
    }

    /** Solicita la primera franja libre de la publicacion (vuelve a leerla para ver el precio con promociones). */
    static ContratacionDto solicitar(String cliente, PublicacionDto publicacion) {
        como(cliente);
        ContratacionDto c = paso("solicitar", app.getContratacionController().solicitar(
                new SolicitarContratacionCommand(publicacion.id(), publicacion.franjas().get(0).id(),
                        "Quiero la mentoria", "Aprender")));
        contrataciones.put(c.id(), cliente);
        return c;
    }

    static void aceptar(String proveedor, Long id) {
        como(proveedor);
        paso("aceptar", app.getContratacionController().aceptar(id));
    }

    static ContratacionDto pagar(String cliente, Long id) {
        como(cliente);
        return paso("pagar", app.getContratacionController().pagar(id));
    }

    static ContratacionDto pagada(String proveedor, String cliente, String precio, TipoPoliticaCancelacion politica,
                                  Duration anticipacion) {
        ContratacionDto c = solicitar(cliente, publicar(proveedor, precio, politica, anticipacion));
        aceptar(proveedor, c.id());
        return pagar(cliente, c.id());
    }

    static void completar(String proveedor, Long id) {
        como(proveedor);
        paso("iniciar", app.getContratacionController().iniciar(id));
        paso("completar", app.getContratacionController().completar(id));
    }

    static ResultadoCancelacionDto cancelar(String quien, Long id) {
        como(quien);
        return paso("cancelar", app.getContratacionController().cancelar(id, "Cambio de planes"));
    }

    /** Ana publica, Bruno paga y uno de los dos cancela; compara el reparto y los saldos finales. */
    static void cancelacion(TipoPoliticaCancelacion politica, Duration anticipacion, String precio,
                            boolean cancelaElCliente, String reintegro, String compensacion) {
        ContratacionDto c = pagada(ANA, BRUNO, precio, politica, anticipacion);
        ResultadoCancelacionDto r = cancelar(cancelaElCliente ? BRUNO : ANA, c.id());
        igual("reintegro", r.reintegroCliente(), reintegro);
        igual("compensacion", r.compensacionProveedor(), compensacion);
        BigDecimal cien = new BigDecimal("100.00");
        saldoEs(BRUNO, cien.subtract(new BigDecimal(precio)).add(new BigDecimal(reintegro)).toPlainString());
        saldoEs(ANA, cien.add(new BigDecimal(compensacion)).toPlainString());
    }

    // ---------- soporte ----------

    static String sesionActual;

    static void como(String usuario) {
        sesionActual = usuario;
        identidad.iniciarSesion(usuario);
    }

    /** Paso que debe salir bien; despues verifica el invariante. */
    static <T> T paso(String nombre, Resultado<T> resultado) {
        T valor = exigir(nombre, resultado);
        verificar(nombre);
        return valor;
    }

    /** Paso que debe fallar con ese codigo; despues verifica que nada se haya movido. */
    static void falla(String nombre, Resultado<?> resultado, ErrorCode esperado) {
        if (resultado.esOk() || resultado.codigo() != esperado) {
            fallar(nombre + ": se esperaba " + esperado + " y fue " + resultado);
        }
        verificar(nombre);
    }

    static <T> T exigir(String nombre, Resultado<T> resultado) {
        if (resultado.esError()) {
            throw new IllegalStateException(nombre + " fallo: " + resultado);
        }
        return resultado.valor();
    }

    static void saldoEs(String usuario, String esperado) {
        como(usuario);
        igual("saldo de " + usuario, app.getWalletController().consultar().valor().saldo(), esperado);
    }

    static void estadoEs(String usuario, Long id, String esperado) {
        como(usuario);
        String estado = app.getContratacionController().obtener(id).valor().estado();
        if (!estado.equals(esperado)) {
            fallar("estado " + estado + ", se esperaba " + esperado);
        }
    }

    static void igual(String que, BigDecimal real, String esperado) {
        if (real.compareTo(new BigDecimal(esperado)) != 0) {
            fallar(que + " = " + real + ", se esperaba " + esperado);
        }
    }

    static void fallar(String detalle) {
        fallas.add(escenarioActual + " -> " + detalle);
    }
}
