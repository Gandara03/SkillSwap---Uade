package com.uade.skillswap.presentation.ui.swing;

import com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionContratacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.AccionPublicacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Contratacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.EstadoContratacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.EstadoPublicacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.FiltrosBusqueda;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Franja;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Modalidad;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Movimiento;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Notificacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.NuevaPublicacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.OrdenBusqueda;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Perfil;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.PoliticaCancelacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Publicacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.Resena;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.ResultadoCancelacion;
import com.uade.skillswap.presentation.ui.swing.contrato.Models.TipoPublicacion;
import com.uade.skillswap.presentation.ui.swing.contrato.OperacionFallidaException;
import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import static com.uade.skillswap.presentation.ui.swing.Theme.BG;
import static com.uade.skillswap.presentation.ui.swing.Theme.BODY;
import static com.uade.skillswap.presentation.ui.swing.Theme.BORDER;
import static com.uade.skillswap.presentation.ui.swing.Theme.DANGER;
import static com.uade.skillswap.presentation.ui.swing.Theme.GOLD;
import static com.uade.skillswap.presentation.ui.swing.Theme.H1;
import static com.uade.skillswap.presentation.ui.swing.Theme.H2;
import static com.uade.skillswap.presentation.ui.swing.Theme.H3;
import static com.uade.skillswap.presentation.ui.swing.Theme.MUTED;
import static com.uade.skillswap.presentation.ui.swing.Theme.NAVY;
import static com.uade.skillswap.presentation.ui.swing.Theme.NAVY_2;
import static com.uade.skillswap.presentation.ui.swing.Theme.SMALL;
import static com.uade.skillswap.presentation.ui.swing.Theme.TEAL;
import static com.uade.skillswap.presentation.ui.swing.Theme.TEAL_DARK;
import static com.uade.skillswap.presentation.ui.swing.Theme.TEXT;
import static com.uade.skillswap.presentation.ui.swing.Theme.button;
import static com.uade.skillswap.presentation.ui.swing.Theme.card;
import static com.uade.skillswap.presentation.ui.swing.Theme.label;

/**
 * Pantalla principal. Es el Client del Adapter: solo conoce {@link UiControllers} y sus modelos, y cada operacion
 * lleva el userId elegido en el selector de la barra lateral (login simulado).
 */
public final class SkillSwapView extends JPanel {

    private static final String AYUDA_BUSQUEDA = "Buscar mentorías, servicios o habilidades...";

    private final UiControllers controllers;
    private final List<String> usuarios;
    private final List<String> etiquetasUsuarios;
    private String usuarioActivo;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel pageTitle = label("Inicio", H1, TEXT);
    private final JLabel badge = label("", new Font("SansSerif", Font.BOLD, 10), Color.WHITE);
    private final JButton coins = button("", false);

    /** {@code usuarios}: los userIds que ofrece el selector; la pantalla arranca con el primero. */
    public SkillSwapView(UiControllers controllers, List<String> usuarios) {
        if (usuarios.isEmpty()) {
            throw new IllegalArgumentException("La pantalla necesita al menos un usuario");
        }
        this.controllers = controllers;
        this.usuarios = List.copyOf(usuarios);
        // Los nombres no cambian durante la sesion: se piden una sola vez para armar el selector
        this.etiquetasUsuarios = usuarios.stream()
                .map(userId -> controllers.perfiles().verPerfil(userId).nombre() + " (" + userId + ")")
                .toList();
        this.usuarioActivo = usuarios.get(0);
        setLayout(new BorderLayout());
        setBackground(BG);
        // Una sola vez: shell() se vuelve a armar en cada cambio de usuario y sumaria otro listener al mismo boton
        coins.addActionListener(e -> intentar(this::showWallet));
        armar();
    }

    /** Arma la barra lateral y la superior con los datos del usuario activo y abre Inicio. */
    private void armar() {
        removeAll();
        add(sidebar(), BorderLayout.WEST);
        add(shell(), BorderLayout.CENTER);
        showHome();
        revalidate();
        repaint();
    }

    private JComponent sidebar() {
        JPanel side = new JPanel();
        side.setBackground(NAVY);
        side.setPreferredSize(new Dimension(228, 720));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 16, 18, 16));
        JLabel logo = label("SkillSwap", new Font("SansSerif", Font.BOLD, 25), Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        side.add(logo);
        JLabel sub = label("Aprendé. Compartí. Crecé.", SMALL, new Color(177, 200, 216));
        sub.setAlignmentX(LEFT_ALIGNMENT);
        side.add(sub);
        side.add(Box.createVerticalStrut(28));
        side.add(nav("⌂  Inicio", this::showHome));
        side.add(nav("⌕  Explorar", this::showExplore));
        side.add(nav("▣  Mis publicaciones", this::showListings));
        side.add(nav("✓  Contrataciones", this::showContracts));
        side.add(nav("◈  SkillCoins", this::showWallet));
        side.add(nav("●  Notificaciones", this::showNotifications));
        side.add(nav("♙  Mi perfil", this::showProfile));
        side.add(Box.createVerticalGlue());
        JPanel user = new JPanel(new BorderLayout(10, 0));
        user.setOpaque(false);
        user.setBorder(new EmptyBorder(8, 6, 4, 4));
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        user.setAlignmentX(LEFT_ALIGNMENT);
        Perfil yo = controllers.perfiles().verPerfil(usuarioActivo);
        JLabel avatar = avatar(iniciales(yo.nombre()), 36);
        user.add(avatar, BorderLayout.WEST);
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.add(label(yo.nombre(), H3, Color.WHITE));
        txt.add(label(yo.rol(), SMALL, new Color(177, 200, 216)));
        user.add(txt);
        side.add(user);
        side.add(Box.createVerticalStrut(8));
        side.add(selectorUsuario());
        return side;
    }

    /** Login simulado: elegir otro usuario rearma la pantalla con su sesion. */
    private JComboBox<String> selectorUsuario() {
        JComboBox<String> selector = new JComboBox<>(etiquetasUsuarios.toArray(String[]::new));
        selector.setSelectedIndex(usuarios.indexOf(usuarioActivo));
        selector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        selector.setAlignmentX(LEFT_ALIGNMENT);
        selector.addActionListener(e -> intentar(() -> cambiarUsuario(usuarios.get(selector.getSelectedIndex()))));
        return selector;
    }

    private void cambiarUsuario(String userId) {
        usuarioActivo = userId;
        armar();
    }

    private JButton nav(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(new Color(224, 234, 241));
        b.setBackground(NAVY);
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> intentar(action));
        return b;
    }

    private JComponent shell() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG);
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(16, 26, 16, 26));
        top.add(pageTitle, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        badge.setOpaque(true);
        badge.setBackground(DANGER);
        badge.setBorder(new EmptyBorder(5, 8, 5, 8));
        actions.add(badge);
        actions.add(coins);
        top.add(actions, BorderLayout.EAST);
        shell.add(top, BorderLayout.NORTH);
        content.setBackground(BG);
        shell.add(content);
        return shell;
    }

    private void page(String title, JComponent view) {
        pageTitle.setText(title);
        long unread = controllers.notificaciones().listarNotificaciones(usuarioActivo).stream()
                .filter(n -> !n.leida()).count();
        badge.setText(unread + " NUEVAS");
        coins.setText("◈  " + controllers.wallet().consultarSaldo(usuarioActivo) + " SkillCoins");
        content.removeAll();
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getViewport().setBackground(BG);
        content.add(sp);
        content.revalidate();
        content.repaint();
    }

    private JPanel base() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(24, 26, 28, 26));
        return p;
    }

    private void addSection(JPanel p, String title, String subtitle) {
        JLabel h = label(title, H2, TEXT);
        h.setAlignmentX(LEFT_ALIGNMENT);
        p.add(h);
        if (subtitle != null) {
            JLabel s = label(subtitle, BODY, MUTED);
            s.setAlignmentX(LEFT_ALIGNMENT);
            p.add(Box.createVerticalStrut(3));
            p.add(s);
        }
        p.add(Box.createVerticalStrut(14));
    }

    private void showHome() {
        Perfil yo = controllers.perfiles().verPerfil(usuarioActivo);
        JPanel p = base();
        JPanel hero = new Theme.RoundedPanel(22, NAVY_2);
        hero.setLayout(new BorderLayout(22, 0));
        hero.setBorder(new EmptyBorder(24, 26, 24, 26));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label("Hola, " + primerNombre(yo.nombre()) + " 👋", new Font("SansSerif", Font.BOLD, 25),
                Color.WHITE));
        words.add(Box.createVerticalStrut(7));
        words.add(label("¿Qué habilidad querés desbloquear hoy?", BODY, new Color(213, 228, 238)));
        words.add(Box.createVerticalStrut(16));
        JTextField search = new JTextField();
        search.setToolTipText(AYUDA_BUSQUEDA);
        search.setBorder(new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(11, 13, 11, 13)));
        search.setAlignmentX(LEFT_ALIGNMENT);
        search.addActionListener(e -> intentar(() -> showExplore(search.getText())));
        words.add(search);
        hero.add(words);
        JPanel stat = new JPanel();
        stat.setOpaque(false);
        stat.setLayout(new BoxLayout(stat, BoxLayout.Y_AXIS));
        stat.add(label("TU PROGRESO", SMALL, new Color(169, 202, 219)));
        stat.add(label(String.valueOf(yo.proyectosCompletados()), new Font("SansSerif", Font.BOLD, 34), Color.WHITE));
        stat.add(label("proyectos completados", SMALL, new Color(213, 228, 238)));
        hero.add(stat, BorderLayout.EAST);
        hero.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hero);
        p.add(Box.createVerticalStrut(26));
        addSection(p, "Recomendado para vos", "Selecciones basadas en tus habilidades e intereses");
        List<Publicacion> recomendadas = controllers.busqueda().recomendar(usuarioActivo);
        if (recomendadas.isEmpty()) {
            JLabel vacio = label("Todavía no hay publicaciones que coincidan con tus intereses. Probá en Explorar.",
                    BODY, MUTED);
            vacio.setAlignmentX(LEFT_ALIGNMENT);
            p.add(vacio);
        } else {
            p.add(cardGrid(recomendadas));
        }
        p.add(Box.createVerticalStrut(26));
        addSection(p, "Explorá por categoría", null);
        JPanel cats = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        cats.setOpaque(false);
        for (String c : controllers.busqueda().categorias()) {
            if (!c.equals("Todas")) {
                JButton b = button(c, false);
                b.addActionListener(e -> intentar(() -> showExploreCategory(c)));
                cats.add(b);
            }
        }
        cats.setAlignmentX(LEFT_ALIGNMENT);
        p.add(cats);
        page("Inicio", p);
    }

    private void showExplore() {
        showExplore("");
    }

    private void showExplore(String initial) {
        showExploreWith(initial, "Todas");
    }

    private void showExploreCategory(String category) {
        showExploreWith("", category);
    }

    private void showExploreWith(String initial, String category) {
        JPanel p = base();
        JPanel filters = card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setAlignmentX(LEFT_ALIGNMENT);
        JTextField q = new JTextField(initial, 24);
        q.setToolTipText(AYUDA_BUSQUEDA);
        q.setBorder(new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(10, 12, 10, 12)));
        JComboBox<String> cat = new JComboBox<>(controllers.busqueda().categorias().toArray(String[]::new));
        cat.setSelectedItem(category);
        JComboBox<OrdenBusqueda> order = new JComboBox<>(OrdenBusqueda.values());
        JButton go = button("Buscar", true);
        filters.add(q);
        filters.add(cat);
        filters.add(order);
        filters.add(go);
        // Sin tope de alto, con pocos resultados la tarjeta se queda con el alto que sobra en la pagina
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, filters.getPreferredSize().height));
        p.add(filters);
        p.add(Box.createVerticalStrut(22));
        JPanel results = new JPanel();
        results.setOpaque(false);
        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        Runnable refresh = () -> {
            results.removeAll();
            List<Publicacion> list = controllers.busqueda().buscar(new FiltrosBusqueda(q.getText(),
                    (String) cat.getSelectedItem(), null, (OrdenBusqueda) order.getSelectedItem()));
            addSection(results, list.size() + " resultados", "Mentorías y servicios disponibles");
            results.add(cardGrid(list));
            results.revalidate();
            results.repaint();
        };
        go.addActionListener(e -> intentar(refresh));
        q.addActionListener(e -> intentar(refresh));
        p.add(results);
        refresh.run();
        page("Explorar", p);
    }

    private JPanel cardGrid(List<Publicacion> list) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        for (Publicacion x : list) {
            grid.add(listingCard(x));
        }
        // Sin tope de alto, con pocas tarjetas la grilla las estira hasta llenar la pagina
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        return grid;
    }

    private JComponent listingCard(Publicacion x) {
        JPanel c = card(new BorderLayout(0, 12));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(pill(x.categoria(), TEAL), BorderLayout.WEST);
        top.add(label("★ " + x.reputacion(), H3, GOLD), BorderLayout.EAST);
        c.add(top, BorderLayout.NORTH);
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel t = label("<html><div style='width:210px'>" + x.titulo() + "</div></html>", H3, TEXT);
        mid.add(t);
        mid.add(Box.createVerticalStrut(7));
        mid.add(label("Por " + x.proveedor(), SMALL, MUTED));
        mid.add(Box.createVerticalStrut(9));
        mid.add(label("<html><div style='width:215px;color:#677480'>" + x.descripcion() + "</div></html>",
                BODY, MUTED));
        mid.add(Box.createVerticalStrut(9));
        mid.add(label(x.modalidad() + "  ·  " + x.duracionMinutos() + " min", SMALL, MUTED));
        c.add(mid);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(label(x.precio() + " ◈", new Font("SansSerif", Font.BOLD, 20), NAVY), BorderLayout.WEST);
        JButton detail = button("Ver detalle", true);
        detail.addActionListener(e -> intentar(() -> showDetail(x)));
        bottom.add(detail, BorderLayout.EAST);
        c.add(bottom, BorderLayout.SOUTH);
        return c;
    }

    private void showDetail(Publicacion x) {
        JPanel p = base();
        JButton back = button("← Volver a explorar", false);
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.addActionListener(e -> intentar(this::showExplore));
        p.add(back);
        p.add(Box.createVerticalStrut(18));
        JPanel c = card(new BorderLayout(28, 0));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(pill(x.categoria(), TEAL));
        info.add(Box.createVerticalStrut(14));
        info.add(label("<html><div style='width:540px'>" + x.titulo() + "</div></html>", H1, TEXT));
        info.add(Box.createVerticalStrut(10));
        info.add(label("★ " + x.reputacion() + "  ·  " + x.proveedor() + "  ·  " + x.modalidad(), H3, MUTED));
        info.add(Box.createVerticalStrut(22));
        info.add(label("Sobre esta propuesta", H2, TEXT));
        info.add(Box.createVerticalStrut(8));
        info.add(label("<html><div style='width:550px'>" + x.descripcion() + "</div></html>", BODY, MUTED));
        info.add(Box.createVerticalStrut(16));
        info.add(label("Duración: " + x.duracionMinutos() + " minutos", BODY, TEXT));
        c.add(info);
        JPanel buy = new Theme.RoundedPanel(16, new Color(239, 247, 246));
        buy.setLayout(new BoxLayout(buy, BoxLayout.Y_AXIS));
        buy.setBorder(new EmptyBorder(22, 22, 22, 22));
        buy.add(label("INVERSIÓN", SMALL, MUTED));
        buy.add(label(x.precio() + " SkillCoins", new Font("SansSerif", Font.BOLD, 25), NAVY));
        buy.add(Box.createVerticalStrut(12));
        buy.add(label("Saldo disponible: " + controllers.wallet().consultarSaldo(usuarioActivo), SMALL, MUTED));
        buy.add(Box.createVerticalStrut(20));
        JButton req = button("Solicitar mentoría", true);
        req.addActionListener(e -> intentar(() -> solicitar(x)));
        buy.add(req);
        c.add(buy, BorderLayout.EAST);
        c.setAlignmentX(LEFT_ALIGNMENT);
        p.add(c);
        page("Detalle", p);
    }

    /** Dialogo de solicitud: franja libre, mensaje y objetivo. Si se cierra sin confirmar, no se envia nada. */
    private void solicitar(Publicacion x) {
        List<Franja> franjas = controllers.publicaciones().franjasDisponibles(usuarioActivo, x.id());
        if (franjas.isEmpty()) {
            message("Sin franjas libres", "Esta publicación no tiene franjas disponibles para solicitar.");
            return;
        }
        JComboBox<String> franja = new JComboBox<>(franjas.stream().map(SkillSwapView::textoFranja)
                .toArray(String[]::new));
        JTextField mensaje = new JTextField(30);
        JTextField objetivo = new JTextField(30);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 5));
        form.add(new JLabel("Franja"));
        form.add(franja);
        form.add(new JLabel("Mensaje para el proveedor"));
        form.add(mensaje);
        form.add(new JLabel("Objetivo: qué querés lograr"));
        form.add(objetivo);
        if (!confirmar("Solicitar: " + x.titulo(), form)) {
            return;
        }
        controllers.contrataciones().solicitar(usuarioActivo, x.id(), franjas.get(franja.getSelectedIndex()).id(),
                mensaje.getText(), objetivo.getText());
        message("¡Solicitud enviada!", "Podés seguir su estado desde Contrataciones.");
        showContracts();
    }

    private static String textoFranja(Franja franja) {
        return franja.inicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " a "
                + franja.fin().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void showListings() {
        JPanel p = base();
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(label("Gestioná tus publicaciones", H2, TEXT));
        JButton create = button("+ Nueva publicación", true);
        create.addActionListener(e -> intentar(this::createListing));
        title.add(create, BorderLayout.EAST);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(15));
        List<Publicacion> own = controllers.publicaciones().listarPropias(usuarioActivo);
        if (own.isEmpty()) {
            p.add(empty("Todavía no publicaste servicios",
                    "Creá tu primera propuesta para comenzar a compartir tu talento."));
        } else {
            for (Publicacion x : own) {
                JPanel row = card(new BorderLayout(12, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                row.setAlignmentX(LEFT_ALIGNMENT);
                JPanel txt = new JPanel();
                txt.setOpaque(false);
                txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
                txt.add(label(x.titulo(), H3, TEXT));
                txt.add(Box.createVerticalStrut(5));
                txt.add(label("Cupo disponible: " + x.cupoDisponible(), BODY, MUTED));
                row.add(txt, BorderLayout.WEST);
                row.add(estado(x.estado().name(), x.estado() == EstadoPublicacion.PUBLICADA ? TEAL : MUTED),
                        BorderLayout.CENTER);
                JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                acciones.setOpaque(false);
                for (AccionPublicacion a : x.accionesDelAutor()) {
                    JButton b = button(a.etiqueta(), a != AccionPublicacion.CERRAR && a != AccionPublicacion.CANCELAR);
                    b.addActionListener(e -> intentar(() -> cambiarEstado(x, a)));
                    acciones.add(b);
                }
                row.add(acciones, BorderLayout.EAST);
                p.add(row);
                p.add(Box.createVerticalStrut(10));
            }
        }
        page("Mis publicaciones", p);
    }

    /** Cancelar pide el motivo antes; si el usuario cierra ese dialogo, no se llama al sistema. */
    private void cambiarEstado(Publicacion x, AccionPublicacion accion) {
        String motivo = null;
        if (accion == AccionPublicacion.CANCELAR) {
            motivo = pedirMotivo("Cancelar la publicación");
            if (motivo == null) {
                return;
            }
        }
        controllers.publicaciones().cambiarEstado(usuarioActivo, x.id(), accion, motivo);
        showListings();
    }

    /**
     * Formulario de nueva publicacion con su primera franja. La franja se puede dejar vacia: el borrador se crea igual,
     * pero el sistema no lo deja publicar. Si un numero o la fecha no tienen formato, se avisa sin llamar al sistema.
     */
    private void createListing() {
        JComboBox<TipoPublicacion> tipo = new JComboBox<>(TipoPublicacion.values());
        JTextField title = new JTextField(28);
        JComboBox<String> cat = new JComboBox<>(controllers.busqueda().categorias().stream()
                .filter(c -> !c.equals("Todas")).toArray(String[]::new));
        cat.setEditable(true);
        JComboBox<Modalidad> modalidad = new JComboBox<>(Modalidad.values());
        JTextField duracion = new JTextField("60");
        JTextField cupo = new JTextField("1");
        JTextField precio = new JTextField();
        JComboBox<PoliticaCancelacion> politica = new JComboBox<>(PoliticaCancelacion.values());
        JTextField etiquetas = new JTextField();
        JTextField habilidades = new JTextField();
        JTextField inicioFranja = new JTextField();
        JTextField minutosFranja = new JTextField("60");
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 6));
        campo(campos, "Tipo", tipo);
        campo(campos, "Título", title);
        campo(campos, "Categoría (elegí o escribí una)", cat);
        campo(campos, "Modalidad", modalidad);
        campo(campos, "Duración (minutos)", duracion);
        campo(campos, "Cupo", cupo);
        campo(campos, "Precio (SkillCoins)", precio);
        campo(campos, "Política de cancelación", politica);
        campo(campos, "Etiquetas (separadas por coma)", etiquetas);
        campo(campos, "Habilidades (separadas por coma)", habilidades);
        campo(campos, "Inicio de la franja (dd/MM/aaaa HH:mm)", inicioFranja);
        campo(campos, "Duración de la franja (minutos)", minutosFranja);
        JTextArea desc = new JTextArea(4, 26);
        JPanel descripcion = new JPanel(new BorderLayout(0, 4));
        descripcion.add(new JLabel("Descripción"), BorderLayout.NORTH);
        descripcion.add(new JScrollPane(desc));
        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.add(campos, BorderLayout.NORTH);
        form.add(descripcion);
        if (!confirmar("Nueva publicación", form)) {
            return;
        }
        NuevaPublicacion datos;
        try {
            datos = new NuevaPublicacion((TipoPublicacion) tipo.getSelectedItem(), title.getText(), desc.getText(),
                    String.valueOf(cat.getEditor().getItem()), (Modalidad) modalidad.getSelectedItem(),
                    entero(duracion), entero(cupo), decimal(precio), (PoliticaCancelacion) politica.getSelectedItem(),
                    lista(etiquetas), lista(habilidades), fechaHora(inicioFranja), entero(minutosFranja));
        } catch (NumberFormatException e) {
            error("Duración, cupo, precio y duración de la franja tienen que ser números.");
            return;
        } catch (DateTimeParseException e) {
            error("El inicio de la franja tiene que tener el formato dd/MM/aaaa HH:mm, por ejemplo 15/11/2026 18:00.");
            return;
        }
        controllers.publicaciones().crearBorrador(usuarioActivo, datos);
        message("Borrador creado", "La publicación quedó en borrador. Publicala desde Mis publicaciones.");
        showListings();
    }

    private static void campo(JPanel form, String etiqueta, JComponent componente) {
        form.add(new JLabel(etiqueta));
        form.add(componente);
    }

    private static int entero(JTextField campo) {
        return Integer.parseInt(campo.getText().trim());
    }

    /** Acepta coma o punto como separador decimal. */
    private static BigDecimal decimal(JTextField campo) {
        return new BigDecimal(campo.getText().trim().replace(',', '.'));
    }

    /** null si el campo esta vacio. */
    private static LocalDateTime fechaHora(JTextField campo) {
        String texto = campo.getText().trim();
        return texto.isEmpty() ? null : LocalDateTime.parse(texto, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Valores separados por coma, sin espacios de mas ni vacios. */
    private static List<String> lista(JTextField campo) {
        return Arrays.stream(campo.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private void showContracts() {
        JPanel p = base();
        addSection(p, "Tus contrataciones", "Solicitudes que hiciste y que recibiste");
        for (Contratacion x : controllers.contrataciones().listarContrataciones(usuarioActivo)) {
            JPanel row = card(new BorderLayout(16, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JPanel txt = new JPanel();
            txt.setOpaque(false);
            txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
            txt.add(label(x.titulo(), H3, TEXT));
            txt.add(Box.createVerticalStrut(5));
            txt.add(label(x.contraparte() + "  ·  " + x.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")),
                    BODY, MUTED));
            row.add(txt, BorderLayout.WEST);
            row.add(estado(x.estado().name(), x.estado() == EstadoContratacion.COMPLETADA ? TEAL : NAVY_2),
                    BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            for (AccionContratacion a : x.acciones()) {
                JButton b = button(a.etiqueta(), a != AccionContratacion.CANCELAR && a != AccionContratacion.RECHAZAR);
                b.addActionListener(e -> intentar(() -> ejecutar(x, a)));
                actions.add(b);
            }
            row.add(actions, BorderLayout.EAST);
            p.add(row);
            p.add(Box.createVerticalStrut(11));
        }
        page("Contrataciones", p);
    }

    /**
     * Ejecuta la accion del boton y refresca la lista. Rechazar, cancelar y calificar piden datos antes; si el usuario
     * cierra ese dialogo sin confirmar, no se llama al sistema.
     */
    private void ejecutar(Contratacion x, AccionContratacion accion) {
        switch (accion) {
            case ACEPTAR -> controllers.contrataciones().aceptar(usuarioActivo, x.id());
            case RECHAZAR -> {
                String motivo = pedirMotivo("Rechazar la solicitud");
                if (motivo == null) {
                    return;
                }
                controllers.contrataciones().rechazar(usuarioActivo, x.id(), motivo);
            }
            case PAGAR -> controllers.contrataciones().pagar(usuarioActivo, x.id());
            case INICIAR -> controllers.contrataciones().iniciar(usuarioActivo, x.id());
            case COMPLETAR -> controllers.contrataciones().completar(usuarioActivo, x.id());
            case CANCELAR -> {
                String motivo = pedirMotivo("Cancelar la contratación");
                if (motivo == null) {
                    return;
                }
                mostrarCancelacion(controllers.contrataciones().cancelar(usuarioActivo, x.id(), motivo));
            }
            case CALIFICAR -> {
                DatosCalificacion datos = pedirCalificacion(x.titulo());
                if (datos == null) {
                    return;
                }
                controllers.calificaciones().calificar(usuarioActivo, x.id(), datos.puntaje(), datos.comentario());
            }
        }
        showContracts();
    }

    /**
     * Como se repartio lo retenido al cancelar. Se muestra la politica de la publicacion: si cancela el proveedor, el
     * cliente recupera el 100 % igual. Si todavia no se habia pagado, los dos montos son 0.
     */
    private void mostrarCancelacion(ResultadoCancelacion r) {
        message("Contratación cancelada", "Política de la publicación: " + r.politica()
                + "\nReintegro al cliente: " + dosDecimales(r.reintegroCliente()) + " SkillCoins"
                + "\nCompensación al proveedor: " + dosDecimales(r.compensacionProveedor()) + " SkillCoins");
    }

    private static String dosDecimales(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Lo que se carga en el dialogo de calificar. */
    private record DatosCalificacion(int puntaje, String comentario) {}

    /** Dialogo de calificar: puntaje de 5 a 1 y comentario opcional; null si se cierra sin confirmar. */
    private DatosCalificacion pedirCalificacion(String titulo) {
        JComboBox<Integer> puntaje = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        JTextField comentario = new JTextField(30);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 5));
        form.add(new JLabel("Puntaje (5 es el mejor)"));
        form.add(puntaje);
        form.add(new JLabel("Comentario (opcional)"));
        form.add(comentario);
        if (!confirmar("Calificar: " + titulo, form)) {
            return null;
        }
        return new DatosCalificacion((Integer) puntaje.getSelectedItem(), comentario.getText());
    }

    /** Pide el motivo de un rechazo o una cancelacion; null si el usuario cancela o cierra el dialogo. */
    private String pedirMotivo(String titulo) {
        return JOptionPane.showInputDialog(this, "Motivo", titulo, JOptionPane.QUESTION_MESSAGE);
    }

    private void showWallet() {
        JPanel p = base();
        JPanel balance = new Theme.RoundedPanel(22, NAVY_2);
        balance.setLayout(new BorderLayout());
        balance.setBorder(new EmptyBorder(24, 26, 24, 26));
        balance.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        JPanel btxt = new JPanel();
        btxt.setOpaque(false);
        btxt.setLayout(new BoxLayout(btxt, BoxLayout.Y_AXIS));
        btxt.add(label("SALDO DISPONIBLE", SMALL, new Color(184, 209, 222)));
        btxt.add(label(controllers.wallet().consultarSaldo(usuarioActivo) + " SkillCoins",
                new Font("SansSerif", Font.BOLD, 30), Color.WHITE));
        balance.add(btxt);
        balance.setAlignmentX(LEFT_ALIGNMENT);
        p.add(balance);
        p.add(Box.createVerticalStrut(24));
        addSection(p, "Movimientos recientes", null);
        for (Movimiento m : controllers.wallet().listarMovimientos(usuarioActivo)) {
            JPanel row = card(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(label(m.concepto(), H3, TEXT));
            t.add(label(m.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")), SMALL, MUTED));
            row.add(t);
            row.add(label((m.importe().signum() > 0 ? "+" : "") + m.importe() + " ◈", H3,
                    m.importe().signum() > 0 ? TEAL : DANGER), BorderLayout.EAST);
            p.add(row);
            p.add(Box.createVerticalStrut(8));
        }
        page("SkillCoins", p);
    }

    private void showNotifications() {
        JPanel p = base();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(label("Centro de notificaciones", H2, TEXT));
        JButton all = button("Marcar todas como leídas", false);
        all.addActionListener(e -> intentar(() -> {
            controllers.notificaciones().marcarTodasLeidas(usuarioActivo);
            showNotifications();
        }));
        head.add(all, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        head.setAlignmentX(LEFT_ALIGNMENT);
        p.add(head);
        p.add(Box.createVerticalStrut(15));
        for (Notificacion n : controllers.notificaciones().listarNotificaciones(usuarioActivo)) {
            JPanel row = card(new BorderLayout(14, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JLabel dot = label(n.leida() ? "○" : "●", H2, n.leida() ? BORDER : TEAL);
            row.add(dot, BorderLayout.WEST);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(label(n.titulo(), H3, TEXT));
            t.add(label(n.detalle(), BODY, MUTED));
            t.add(label(n.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")), SMALL, MUTED));
            row.add(t);
            if (!n.leida()) {
                JButton read = button("Marcar leída", false);
                read.addActionListener(e -> intentar(() -> {
                    controllers.notificaciones().marcarLeida(usuarioActivo, n.id());
                    showNotifications();
                }));
                row.add(read, BorderLayout.EAST);
            }
            p.add(row);
            p.add(Box.createVerticalStrut(9));
        }
        page("Notificaciones", p);
    }

    private void showProfile() {
        Perfil x = controllers.perfiles().verPerfil(usuarioActivo);
        JPanel p = base();
        JPanel hero = card(new BorderLayout(20, 0));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        hero.add(avatar(iniciales(x.nombre()), 86), BorderLayout.WEST);
        JPanel data = new JPanel();
        data.setOpaque(false);
        data.setLayout(new BoxLayout(data, BoxLayout.Y_AXIS));
        data.add(label(x.nombre(), H1, TEXT));
        data.add(label(x.rol(), H3, TEAL_DARK));
        data.add(Box.createVerticalStrut(10));
        data.add(label("<html><div style='width:570px'>" + x.bio() + "</div></html>", BODY, MUTED));
        hero.add(data);
        JPanel stats = new JPanel(new GridLayout(3, 1, 0, 4));
        stats.setOpaque(false);
        stats.add(label("★ " + x.reputacion() + " reputación", H3, GOLD));
        stats.add(label(x.resenas() + " reseñas", BODY, MUTED));
        stats.add(label(x.proyectosCompletados() + " proyectos", BODY, MUTED));
        hero.add(stats, BorderLayout.EAST);
        hero.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hero);
        p.add(Box.createVerticalStrut(24));
        addSection(p, "Habilidades", null);
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tags.setOpaque(false);
        for (String s : x.habilidades()) {
            tags.add(pill(s, NAVY_2));
        }
        tags.setAlignmentX(LEFT_ALIGNMENT);
        // El alto que sobra en la pagina va al relleno del final; sin este tope se lo quedaria este panel
        tags.setMaximumSize(new Dimension(Integer.MAX_VALUE, tags.getPreferredSize().height));
        p.add(tags);
        p.add(Box.createVerticalStrut(24));
        addSection(p, "Últimas reseñas", null);
        if (x.ultimasResenas().isEmpty()) {
            JLabel vacio = label("Todavía no tenés reseñas.", BODY, MUTED);
            vacio.setAlignmentX(LEFT_ALIGNMENT);
            p.add(vacio);
        }
        for (Resena r : x.ultimasResenas()) {
            JPanel row = card(new BorderLayout(14, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.add(label("★ " + r.puntaje(), H3, GOLD), BorderLayout.WEST);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(label(r.comentario() == null || r.comentario().isBlank() ? "Sin comentario" : r.comentario(),
                    BODY, TEXT));
            t.add(label(r.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), SMALL, MUTED));
            row.add(t);
            p.add(row);
            p.add(Box.createVerticalStrut(8));
        }
        p.add(Box.createVerticalGlue());
        page("Mi perfil", p);
    }

    private JLabel pill(String text, Color color) {
        JLabel l = label(text, new Font("SansSerif", Font.BOLD, 10), Color.WHITE);
        l.setOpaque(true);
        l.setBackground(color);
        l.setBorder(new EmptyBorder(6, 9, 6, 9));
        return l;
    }

    /** El estado con su tamano: sola en el centro de un BorderLayout, la etiqueta se estiraria a toda la fila. */
    private JPanel estado(String texto, Color color) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(pill(texto, color));
        return panel;
    }

    private JLabel avatar(String initials, int size) {
        JLabel a = new JLabel(initials, SwingConstants.CENTER);
        a.setPreferredSize(new Dimension(size, size));
        a.setMinimumSize(new Dimension(size, size));
        a.setMaximumSize(new Dimension(size, size));
        a.setOpaque(true);
        a.setBackground(TEAL);
        a.setForeground(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.BOLD, size / 3));
        a.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 2, true));
        return a;
    }

    private JComponent empty(String title, String subtitle) {
        JPanel e = card(new GridBagLayout());
        e.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        e.setAlignmentX(LEFT_ALIGNMENT);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel h = label(title, H2, TEXT);
        h.setAlignmentX(CENTER_ALIGNMENT);
        t.add(h);
        t.add(Box.createVerticalStrut(8));
        JLabel s = label(subtitle, BODY, MUTED);
        s.setAlignmentX(CENTER_ALIGNMENT);
        t.add(s);
        e.add(t);
        return e;
    }

    private void message(String title, String body) {
        JOptionPane.showMessageDialog(this, body, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Muestra el formulario con Aceptar y Cancelar; true solo si el usuario confirma. */
    private boolean confirmar(String titulo, JComponent formulario) {
        return JOptionPane.showConfirmDialog(this, formulario, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /**
     * Unico lugar donde la pantalla muestra errores: corre la accion y, si falla, avisa en un dialogo y deja la
     * pantalla como estaba. Los errores del sistema llegan con su mensaje para el usuario.
     */
    private void intentar(Runnable accion) {
        try {
            accion.run();
        } catch (OperacionFallidaException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            error("Error inesperado: " + e.getMessage());
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo completar la operación", JOptionPane.ERROR_MESSAGE);
    }

    private static String primerNombre(String nombre) {
        return nombre.trim().split("\\s+")[0];
    }

    /** Iniciales del avatar: la primera letra de las dos primeras palabras del nombre. */
    private static String iniciales(String nombre) {
        String[] palabras = nombre.trim().split("\\s+");
        String iniciales = palabras[0].substring(0, 1);
        if (palabras.length > 1) {
            iniciales += palabras[1].substring(0, 1);
        }
        return iniciales.toUpperCase();
    }
}
