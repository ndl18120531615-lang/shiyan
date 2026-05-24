package com.calculator.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public class CalculatorView {
    private final JTextField display;
    private final JPanel buttonPanel;
    private final JList<String> historyList;
    private final java.util.Map<String, JButton> buttonMap;
    private final Theme theme;
    private final int baseFontSize;
    private JPanel scientificPanel;

    public CalculatorView(Theme theme, int fontSize) {
        this.theme = theme;
        this.baseFontSize = Math.max(16, fontSize);
        this.buttonMap = new java.util.HashMap<>();
        this.display = createDisplay();
        this.buttonPanel = createButtonPanel();
        this.historyList = createHistoryList();
    }

    public JTextField getDisplay() {
        return display;
    }

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public JList<String> getHistoryList() {
        return historyList;
    }

    public JButton getButton(String name) {
        return buttonMap.get(name);
    }

    public java.util.Map<String, JButton> getButtonMap() {
        return buttonMap;
    }

    public JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBackground(theme.BG_MAIN);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(display, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBackground(theme.BG_MAIN);
        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        centerPanel.add(createHistoryPanel(), BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JTextField createDisplay() {
        JTextField field = new JTextField("0");
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setFont(new Font("SansSerif", Font.BOLD, baseFontSize));
        field.setBackground(theme.BG_PANEL);
        field.setForeground(theme.TEXT_PRIMARY);
        field.setCaretColor(theme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));
        return field;
    }

    private JPanel createButtonPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.setBackground(theme.BG_MAIN);

        JPanel topBar = new JPanel(new GridLayout(1, 2, 8, 8));
        topBar.setBackground(theme.BG_MAIN);

        JButton sciToggle = new JButton("SCI ON");
        sciToggle.setName("SCI_TOGGLE");
        styleTopButton(sciToggle);
        topBar.add(sciToggle);
        buttonMap.put("SCI_TOGGLE", sciToggle);

        JButton ansButton = new JButton("Ans");
        styleTopButton(ansButton);
        topBar.add(ansButton);
        buttonMap.put("Ans", ansButton);

        wrapper.add(topBar, BorderLayout.NORTH);

        JPanel padContainer = new JPanel(new BorderLayout(8, 8));
        padContainer.setBackground(theme.BG_MAIN);

        scientificPanel = createScientificPanel();
        scientificPanel.setName("SCI_PANEL");
        padContainer.add(scientificPanel, BorderLayout.WEST);

        JPanel basicPanel = createBasicPanel();
        padContainer.add(basicPanel, BorderLayout.CENTER);

        wrapper.add(padContainer, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createScientificPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBackground(theme.BG_MAIN);

        String[] buttons = {"sin", "cos", "tan", "log", "sqrt", "^"};
        for (String text : buttons) {
            JButton button = new JButton(text);
            styleFunctionButton(button);
            buttonMap.put(text, button);
            panel.add(button);
        }
        return panel;
    }

    private JPanel createBasicPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(7, 4, 8, 8));
        buttonPanel.setBackground(theme.BG_MAIN);

        String[] buttons = {
            "MC", "MR", "M+", "M-",
            "C", "DEL", "(", ")",
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "+/-", "0", ".", "+",
            "%", "", "=", ""
        };

        for (String text : buttons) {
            if (text.isEmpty()) {
                JPanel spacer = new JPanel();
                spacer.setBackground(theme.BG_MAIN);
                buttonPanel.add(spacer);
                continue;
            }
            JButton button = new JButton(text);
            styleButton(button, text);
            buttonMap.put(text, button);
            buttonPanel.add(button);
        }
        return buttonPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(theme.BG_PANEL);
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new java.awt.Color(66, 72, 83)),
            "History"
        ));

        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clearHistory = new JButton("Clear History");
        clearHistory.setFocusPainted(false);
        clearHistory.setFont(new Font("SansSerif", Font.BOLD, 13));
        clearHistory.setBackground(theme.BG_ACTION);
        clearHistory.setForeground(theme.TEXT_SECONDARY);
        buttonMap.put("CLEAR_HISTORY", clearHistory);
        panel.add(clearHistory, BorderLayout.SOUTH);

        return panel;
    }

    private JList<String> createHistoryList() {
        JList<String> list = new JList<>();
        list.setBackground(theme.BG_PANEL);
        list.setForeground(theme.TEXT_PRIMARY);
        list.setFont(new Font("SansSerif", Font.PLAIN, 14));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return list;
    }

    private void styleTopButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, baseFontSize - 12)));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setForeground(theme.TEXT_PRIMARY);
        button.setBackground(theme.BG_ACCENT);
        final java.awt.Color normal = theme.BG_ACCENT;
        final java.awt.Color hover = theme.BG_ACCENT.brighter();
        button.addChangeListener(e -> {
            if (button.getModel().isRollover() || button.getModel().isPressed()) {
                button.setBackground(hover);
            } else {
                button.setBackground(normal);
            }
        });
    }

    private void styleFunctionButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, baseFontSize - 10)));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setForeground(theme.TEXT_PRIMARY);
        button.setBackground(theme.BG_ACCENT);
        final java.awt.Color normal = theme.BG_ACCENT;
        final java.awt.Color hover = theme.BG_ACCENT.brighter();
        button.addChangeListener(e -> {
            if (button.getModel().isRollover() || button.getModel().isPressed()) {
                button.setBackground(hover);
            } else {
                button.setBackground(normal);
            }
        });
    }

    private void styleButton(JButton button, String label) {
        button.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, baseFontSize - 8)));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        button.setForeground(theme.TEXT_SECONDARY);

        java.awt.Color baseColor = theme.BG_NUMBER;
        if ("C".equals(label) || "DEL".equals(label) || "+/-".equals(label) || "%".equals(label)
            || "(".equals(label) || ")".equals(label)
            || "MC".equals(label) || "MR".equals(label) || "M+".equals(label) || "M-".equals(label)) {
            baseColor = theme.BG_ACTION;
        }
        if ("/".equals(label) || "*".equals(label) || "-".equals(label) || "+".equals(label) || "^".equals(label)) {
            baseColor = theme.BG_OPERATOR;
        }
        if ("=".equals(label)) {
            baseColor = theme.BG_EQUALS;
        }

        final java.awt.Color normalColor = baseColor;
        final java.awt.Color hoverColor = baseColor.brighter();
        button.setBackground(normalColor);
        button.addChangeListener(e -> {
            if (button.getModel().isRollover() || button.getModel().isPressed()) {
                button.setBackground(hoverColor);
            } else {
                button.setBackground(normalColor);
            }
        });
    }

    public void updateDisplay(String text) {
        display.setText(text.isEmpty() ? "0" : text);
    }

    public void showError() {
        display.setText("Error");
    }

    public void applyTheme(Theme newTheme) {
        // Theme update logic can be added here in the future
    }

    public void setScientificMode(boolean enabled) {
        if (scientificPanel != null) {
            scientificPanel.setVisible(enabled);
            scientificPanel.revalidate();
            scientificPanel.repaint();
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }
}
