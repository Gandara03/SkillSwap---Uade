package com.uade.skillswap.presentation.ui.swing;

import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Ventana de la aplicacion: aplica el tema y muestra {@link SkillSwapView}. Al cerrarse corre la accion de cierre que
 * recibe (por ejemplo, cerrar la base) y despues termina el programa; no conoce quien la arma.
 */
public final class SkillSwapFrame extends JFrame {

    public SkillSwapFrame(UiControllers controllers, List<String> usuarios, Runnable alCerrar) {
        super("SkillSwap");
        Theme.install();
        // JFrame avisa primero a los listeners de windowClosing y recien despues aplica EXIT_ON_CLOSE
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                alCerrar.run();
            }
        });
        setMinimumSize(new Dimension(1120, 700));
        setSize(1365, 820);
        setLocationRelativeTo(null);
        setContentPane(new SkillSwapView(controllers, usuarios));
    }
}
