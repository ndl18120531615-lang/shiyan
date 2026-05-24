package com.calculator.app;

import com.calculator.config.AppConfig;
import com.calculator.core.CalculatorModel;
import com.calculator.ui.CalculatorView;
import com.calculator.ui.Theme;
import com.calculator.ui.controller.CalculatorController;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SimpleCalculator extends JFrame {
    private final CalculatorModel model;
    private final CalculatorView view;
    private final CalculatorController controller;
    private final AppConfig config;

    public SimpleCalculator() {
        setTitle("Simple Swing Calculator");

        config = AppConfig.getInstance();
        int fontSize = config.getFontSize();
        Theme theme = config.getTheme();

        model = new CalculatorModel();
        view = new CalculatorView(theme, fontSize);
        controller = new CalculatorController(model, view);

        setSize(config.getWindowWidth(), config.getWindowHeight());
        setMinimumSize(new Dimension(820, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int x = config.getWindowX();
        int y = config.getWindowY();
        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }

        JPanel mainPanel = view.createMainPanel();
        setContentPane(mainPanel);

        controller.installKeyBindings(getRootPane());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                config.setWindowX(getX());
                config.setWindowY(getY());
                config.setWindowWidth(getWidth());
                config.setWindowHeight(getHeight());
                config.saveConfig();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleCalculator calculator = new SimpleCalculator();
            calculator.setVisible(true);
        });
    }
}
