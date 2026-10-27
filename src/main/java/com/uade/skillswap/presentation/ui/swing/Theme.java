package com.uade.skillswap.presentation.ui.swing;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public final class Theme {
    private Theme() {}
    public static final Color NAVY = new Color(20, 45, 67);
    public static final Color NAVY_2 = new Color(30, 64, 91);
    public static final Color TEAL = new Color(19, 150, 137);
    public static final Color TEAL_DARK = new Color(13, 113, 103);
    public static final Color BG = new Color(244, 247, 249);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(31, 41, 51);
    public static final Color MUTED = new Color(103, 116, 128);
    public static final Color BORDER = new Color(222, 229, 234);
    public static final Color GOLD = new Color(241, 170, 43);
    public static final Color DANGER = new Color(207, 71, 71);
    public static final Font H1 = new Font("SansSerif", Font.BOLD, 27);
    public static final Font H2 = new Font("SansSerif", Font.BOLD, 20);
    public static final Font H3 = new Font("SansSerif", Font.BOLD, 15);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);

    public static void install() {
        UIManager.put("Label.font", BODY); UIManager.put("Button.font", BODY);
        UIManager.put("TextField.font", BODY); UIManager.put("ComboBox.font", BODY);
        UIManager.put("Panel.background", BG); UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("ScrollBar.width", 10);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l=new JLabel(text); l.setFont(font); l.setForeground(color); return l;
    }

    public static JButton button(String text, boolean primary) {
        JButton b=new JButton(text); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(10,16,10,16));
        b.setForeground(primary?Color.WHITE:NAVY); b.setBackground(primary?TEAL:Color.WHITE);
        b.setOpaque(true); return b;
    }

    public static JPanel card(LayoutManager layout) {
        JPanel p=new RoundedPanel(18, CARD); p.setLayout(layout);
        p.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(16,16,16,16))); return p;
    }

    public static class RoundedPanel extends JPanel {
        private final int radius; private final Color fill;
        public RoundedPanel(int radius, Color fill){this.radius=radius;this.fill=fill;setOpaque(false);}
        @Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(fill);g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,radius,radius));g2.dispose();super.paintComponent(g);}
    }
}
