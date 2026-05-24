package com.calculator.ui;

import java.awt.Color;

public enum Theme {
    DARK("Dark",
        new Color(23, 27, 34),
        new Color(30, 35, 44),
        new Color(53, 59, 72),
        new Color(73, 82, 99),
        new Color(246, 153, 63),
        new Color(71, 166, 106),
        new Color(110, 132, 214),
        new Color(242, 244, 248),
        new Color(220, 224, 230)
    ),
    LIGHT("Light",
        new Color(250, 250, 250),
        new Color(240, 240, 240),
        new Color(220, 220, 220),
        new Color(200, 200, 200),
        new Color(255, 140, 0),
        new Color(70, 160, 100),
        new Color(100, 150, 220),
        new Color(30, 30, 30),
        new Color(60, 60, 60)
    );

    private final String displayName;
    public final Color BG_MAIN;
    public final Color BG_PANEL;
    public final Color BG_NUMBER;
    public final Color BG_ACTION;
    public final Color BG_OPERATOR;
    public final Color BG_EQUALS;
    public final Color BG_ACCENT;
    public final Color TEXT_PRIMARY;
    public final Color TEXT_SECONDARY;

    Theme(String displayName, Color bgMain, Color bgPanel, Color bgNumber, Color bgAction,
          Color bgOperator, Color bgEquals, Color bgAccent, Color textPrimary, Color textSecondary) {
        this.displayName = displayName;
        this.BG_MAIN = bgMain;
        this.BG_PANEL = bgPanel;
        this.BG_NUMBER = bgNumber;
        this.BG_ACTION = bgAction;
        this.BG_OPERATOR = bgOperator;
        this.BG_EQUALS = bgEquals;
        this.BG_ACCENT = bgAccent;
        this.TEXT_PRIMARY = textPrimary;
        this.TEXT_SECONDARY = textSecondary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Theme fromString(String name) {
        try {
            return Theme.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }
}
