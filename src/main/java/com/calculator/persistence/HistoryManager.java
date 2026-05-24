package com.calculator.persistence;

import javax.swing.DefaultListModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class HistoryManager {
    private static final Path HISTORY_FILE = Paths.get("calculator_history.txt");
    private static final int MAX_HISTORY = 60;
    private final DefaultListModel<String> historyModel;

    public HistoryManager() {
        this.historyModel = new DefaultListModel<>();
        loadHistoryFromFile();
    }

    public DefaultListModel<String> getHistoryModel() {
        return historyModel;
    }

    public void addHistory(String item) {
        historyModel.add(0, item);
        while (historyModel.size() > MAX_HISTORY) {
            historyModel.remove(historyModel.size() - 1);
        }
        saveHistoryToFile();
    }

    public void clearHistory() {
        historyModel.clear();
        saveHistoryToFile();
    }

    private void loadHistoryFromFile() {
        if (!Files.exists(HISTORY_FILE)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    historyModel.addElement(line);
                }
            }
            while (historyModel.size() > MAX_HISTORY) {
                historyModel.remove(historyModel.size() - 1);
            }
        } catch (IOException ignored) {
        }
    }

    private void saveHistoryToFile() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < historyModel.size(); i++) {
            lines.add(historyModel.get(i));
        }
        try {
            Files.write(
                HISTORY_FILE,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ignored) {
        }
    }
}
