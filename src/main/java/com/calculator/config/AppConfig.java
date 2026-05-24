package com.calculator.config;

import com.calculator.ui.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {
    private static AppConfig instance;
    private final Path configDir;
    private final Path configFile;
    private Properties properties;

    private static final String THEME_KEY = "theme";
    private static final String FONT_SIZE_KEY = "fontSize";
    private static final String WINDOW_X_KEY = "windowX";
    private static final String WINDOW_Y_KEY = "windowY";
    private static final String WINDOW_WIDTH_KEY = "windowWidth";
    private static final String WINDOW_HEIGHT_KEY = "windowHeight";

    private AppConfig() {
        this.configDir = Paths.get(System.getProperty("user.home"), ".calculator");
        this.configFile = configDir.resolve("config.properties");
        this.properties = new Properties();
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (Files.exists(configFile)) {
                properties.load(Files.newInputStream(configFile));
            } else {
                initializeDefaults();
            }
        } catch (IOException e) {
            initializeDefaults();
        }
    }

    private void initializeDefaults() {
        properties.setProperty(THEME_KEY, "DARK");
        properties.setProperty(FONT_SIZE_KEY, "30");
        properties.setProperty(WINDOW_X_KEY, "100");
        properties.setProperty(WINDOW_Y_KEY, "100");
        properties.setProperty(WINDOW_WIDTH_KEY, "900");
        properties.setProperty(WINDOW_HEIGHT_KEY, "620");
    }

    public void saveConfig() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            properties.store(Files.newOutputStream(configFile), "Calculator Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public Theme getTheme() {
        String themeName = properties.getProperty(THEME_KEY, "DARK");
        return Theme.fromString(themeName);
    }

    public void setTheme(Theme theme) {
        properties.setProperty(THEME_KEY, theme.name());
    }

    public int getFontSize() {
        String size = properties.getProperty(FONT_SIZE_KEY, "30");
        try {
            return Integer.parseInt(size);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public void setFontSize(int size) {
        properties.setProperty(FONT_SIZE_KEY, String.valueOf(size));
    }

    public int getWindowX() {
        return getIntProperty(WINDOW_X_KEY, 100);
    }

    public void setWindowX(int x) {
        properties.setProperty(WINDOW_X_KEY, String.valueOf(x));
    }

    public int getWindowY() {
        return getIntProperty(WINDOW_Y_KEY, 100);
    }

    public void setWindowY(int y) {
        properties.setProperty(WINDOW_Y_KEY, String.valueOf(y));
    }

    public int getWindowWidth() {
        return getIntProperty(WINDOW_WIDTH_KEY, 900);
    }

    public void setWindowWidth(int width) {
        properties.setProperty(WINDOW_WIDTH_KEY, String.valueOf(width));
    }

    public int getWindowHeight() {
        return getIntProperty(WINDOW_HEIGHT_KEY, 620);
    }

    public void setWindowHeight(int height) {
        properties.setProperty(WINDOW_HEIGHT_KEY, String.valueOf(height));
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
