package com.calculator.ui.controller;

import com.calculator.core.CalculatorModel;
import com.calculator.ui.CalculatorView;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

public class CalculatorController {
    private final CalculatorModel model;
    private final CalculatorView view;
    private boolean scientificMode;
    private JComponent rootComponent;

    public CalculatorController(CalculatorModel model, CalculatorView view) {
        this.model = model;
        this.view = view;
        this.scientificMode = true;
        setupEventHandlers();
        view.setScientificMode(scientificMode);
        updateDisplay();
    }

    public void installKeyBindings(JComponent root) {
        this.rootComponent = root;
        setupKeyBindings();
    }

    private void setupEventHandlers() {
        JList<String> historyList = view.getHistoryList();
        DefaultListModel<String> historyModel = model.getHistoryManager().getHistoryModel();
        historyList.setModel(historyModel);

        java.util.Map<String, JButton> buttonMap = view.getButtonMap();

        for (java.util.Map.Entry<String, JButton> entry : buttonMap.entrySet()) {
            String buttonName = entry.getKey();
            JButton button = entry.getValue();

            if ("SCI_TOGGLE".equals(buttonName)) {
                button.addActionListener(e -> toggleScientificMode());
            } else if ("CLEAR_HISTORY".equals(buttonName)) {
                button.addActionListener(e -> {
                    model.getHistoryManager().clearHistory();
                    historyList.clearSelection();
                });
            } else {
                button.addActionListener(e -> handleInput(buttonName));
            }
        }

        historyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            String item = historyList.getSelectedValue();
            if (item == null) {
                return;
            }
            int eqIndex = item.lastIndexOf(" = ");
            if (eqIndex > -1) {
                String result = item.substring(eqIndex + 3).trim();
                setExpressionFromHistory(result);
            }
        });

        historyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() < 2) {
                    return;
                }
                int index = historyList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                String item = historyModel.get(index);
                int eqIndex = item.lastIndexOf(" = ");
                if (eqIndex > -1) {
                    String exprPart = item.substring(0, eqIndex).trim();
                    setExpressionFromHistory(exprPart);
                }
            }
        });
    }

    private void toggleScientificMode() {
        scientificMode = !scientificMode;
        view.setScientificMode(scientificMode);
        JButton sciToggle = view.getButton("SCI_TOGGLE");
        if (sciToggle != null) {
            sciToggle.setText(scientificMode ? "SCI ON" : "SCI OFF");
        }
    }

    private void handleInput(String input) {
        if ("Error".equals(view.getDisplay().getText()) && !"C".equals(input)) {
            model.clear();
        }

        if (isDigit(input)) {
            model.appendDigit(input);
            updateDisplay();
            return;
        }

        switch (input) {
            case ".":
                model.appendDecimalPoint();
                updateDisplay();
                break;
            case "C":
                model.clear();
                updateDisplay();
                break;
            case "DEL":
                model.deleteLastCharacter();
                updateDisplay();
                break;
            case "+/-":
                model.toggleSign();
                updateDisplay();
                break;
            case "%":
                model.convertToPercent();
                updateDisplay();
                break;
            case "(":
                model.appendLeftParenthesis();
                updateDisplay();
                break;
            case ")":
                model.appendRightParenthesis();
                updateDisplay();
                break;
            case "+":
            case "-":
            case "*":
            case "/":
            case "^":
                model.appendOperator(input);
                updateDisplay();
                break;
            case "Ans":
                model.insertAnswer();
                updateDisplay();
                break;
            case "MC":
            case "MR":
            case "M+":
            case "M-":
                model.handleMemoryOperation(input);
                updateDisplay();
                break;
            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "sqrt":
                model.appendFunction(input);
                updateDisplay();
                break;
            case "=":
                evaluateExpression();
                break;
        }
    }

    private void evaluateExpression() {
        try {
            model.evaluate();
            updateDisplay();
        } catch (IllegalArgumentException ex) {
            view.showError();
        }
    }

    private void setExpressionFromHistory(String content) {
        model.setExpressionFromHistory(content);
        updateDisplay();
    }

    private void updateDisplay() {
        view.updateDisplay(model.getExpressionText());
    }

    private boolean isDigit(String text) {
        return text.length() == 1 && Character.isDigit(text.charAt(0));
    }

    private void setupKeyBindings() {
        if (rootComponent == null) return;

        bindKey("DIGIT_0", KeyStroke.getKeyStroke('0'), "0");
        bindKey("DIGIT_1", KeyStroke.getKeyStroke('1'), "1");
        bindKey("DIGIT_2", KeyStroke.getKeyStroke('2'), "2");
        bindKey("DIGIT_3", KeyStroke.getKeyStroke('3'), "3");
        bindKey("DIGIT_4", KeyStroke.getKeyStroke('4'), "4");
        bindKey("DIGIT_5", KeyStroke.getKeyStroke('5'), "5");
        bindKey("DIGIT_6", KeyStroke.getKeyStroke('6'), "6");
        bindKey("DIGIT_7", KeyStroke.getKeyStroke('7'), "7");
        bindKey("DIGIT_8", KeyStroke.getKeyStroke('8'), "8");
        bindKey("DIGIT_9", KeyStroke.getKeyStroke('9'), "9");
        bindKey("NUMPAD_0", KeyStroke.getKeyStroke("NUMPAD0"), "0");
        bindKey("NUMPAD_1", KeyStroke.getKeyStroke("NUMPAD1"), "1");
        bindKey("NUMPAD_2", KeyStroke.getKeyStroke("NUMPAD2"), "2");
        bindKey("NUMPAD_3", KeyStroke.getKeyStroke("NUMPAD3"), "3");
        bindKey("NUMPAD_4", KeyStroke.getKeyStroke("NUMPAD4"), "4");
        bindKey("NUMPAD_5", KeyStroke.getKeyStroke("NUMPAD5"), "5");
        bindKey("NUMPAD_6", KeyStroke.getKeyStroke("NUMPAD6"), "6");
        bindKey("NUMPAD_7", KeyStroke.getKeyStroke("NUMPAD7"), "7");
        bindKey("NUMPAD_8", KeyStroke.getKeyStroke("NUMPAD8"), "8");
        bindKey("NUMPAD_9", KeyStroke.getKeyStroke("NUMPAD9"), "9");
        bindKey("DOT", KeyStroke.getKeyStroke('.'), ".");
        bindKey("NUMPAD_DOT", KeyStroke.getKeyStroke("DECIMAL"), ".");
        bindKey("PLUS", KeyStroke.getKeyStroke('+'), "+");
        bindKey("MINUS", KeyStroke.getKeyStroke('-'), "-");
        bindKey("MULTIPLY_CHAR", KeyStroke.getKeyStroke('*'), "*");
        bindKey("DIVIDE_CHAR", KeyStroke.getKeyStroke('/'), "/");
        bindKey("POW", KeyStroke.getKeyStroke('^'), "^");
        bindKey("NUMPAD_PLUS", KeyStroke.getKeyStroke("ADD"), "+");
        bindKey("NUMPAD_MINUS", KeyStroke.getKeyStroke("SUBTRACT"), "-");
        bindKey("NUMPAD_MULTIPLY", KeyStroke.getKeyStroke("MULTIPLY"), "*");
        bindKey("NUMPAD_DIVIDE", KeyStroke.getKeyStroke("DIVIDE"), "/");
        bindKey("LEFT_PAREN_SHIFT", KeyStroke.getKeyStroke('('), "(");
        bindKey("RIGHT_PAREN_SHIFT", KeyStroke.getKeyStroke(')'), ")");
        bindKey("LEFT_BRACKET", KeyStroke.getKeyStroke('['), "(");
        bindKey("RIGHT_BRACKET", KeyStroke.getKeyStroke(']'), ")");
        bindKey("ENTER", KeyStroke.getKeyStroke("ENTER"), "=");
        bindKey("EQUALS", KeyStroke.getKeyStroke('='), "=");
        bindKey("BACKSPACE", KeyStroke.getKeyStroke("BACK_SPACE"), "DEL");
        bindKey("ESCAPE", KeyStroke.getKeyStroke("ESCAPE"), "C");
        bindKey("PERCENT", KeyStroke.getKeyStroke('%'), "%");
        bindKey("ANS", KeyStroke.getKeyStroke('a'), "Ans");
    }

    private void bindKey(String key, KeyStroke stroke, String actionValue) {
        rootComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = view.getButton(actionValue);
                if (button != null) {
                    button.doClick(80);
                } else {
                    handleInput(actionValue);
                }
            }
        };
        rootComponent.getActionMap().put(key, action);
    }

    public boolean isScientificMode() {
        return scientificMode;
    }
}
