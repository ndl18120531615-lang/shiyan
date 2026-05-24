package com.calculator.core;

import com.calculator.persistence.HistoryManager;

import java.math.BigDecimal;

public class CalculatorModel {
    private final StringBuilder expression;
    private final ExpressionEvaluator evaluator;
    private final HistoryManager historyManager;

    private double memoryValue;
    private double lastAnswer;
    private boolean justEvaluated;
    private String expressionBeforeHistoryBrowse;

    public CalculatorModel() {
        this.expression = new StringBuilder();
        this.evaluator = new ExpressionEvaluator();
        this.historyManager = new HistoryManager();
        this.memoryValue = 0.0;
        this.lastAnswer = 0.0;
        this.justEvaluated = false;
        this.expressionBeforeHistoryBrowse = "";
    }

    public String getExpressionText() {
        return expression.toString();
    }

    public void appendDigit(String digit) {
        if (justEvaluated) {
            expression.setLength(0);
            justEvaluated = false;
        }

        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (last == ')') {
                expression.append('*');
            }
        }

        expression.append(digit);
    }

    public void appendDecimalPoint() {
        if (justEvaluated) {
            expression.setLength(0);
            justEvaluated = false;
        }

        if (expression.length() > 0 && expression.charAt(expression.length() - 1) == ')') {
            expression.append('*');
        }

        if (expression.length() == 0 || isOperator(expression.charAt(expression.length() - 1))
            || expression.charAt(expression.length() - 1) == '(') {
            expression.append("0.");
            return;
        }

        int start = getCurrentNumberStart(expression.length());
        if (start < expression.length() && expression.substring(start).contains(".")) {
            return;
        }

        expression.append('.');
    }

    public void appendOperator(String op) {
        if (justEvaluated) {
            justEvaluated = false;
        }

        if (expression.length() == 0) {
            if ("-".equals(op)) {
                expression.append('-');
            }
            return;
        }

        if (hasPendingUnaryMinus()) {
            if ("-".equals(op)) {
                return;
            }
            expression.deleteCharAt(expression.length() - 1);
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOperator(last)) {
            if ("-".equals(op) && (last == '*' || last == '/' || last == '+' || last == '^')) {
                expression.append('-');
            } else {
                expression.setCharAt(expression.length() - 1, op.charAt(0));
            }
        } else if (last == '(') {
            if ("-".equals(op)) {
                expression.append('-');
            }
        } else {
            expression.append(op);
        }
    }

    public void appendLeftParenthesis() {
        if (justEvaluated) {
            expression.setLength(0);
            justEvaluated = false;
        }

        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (Character.isDigit(last) || last == '.' || last == ')') {
                expression.append('*');
            }
        }

        expression.append('(');
    }

    public void appendRightParenthesis() {
        if (expression.length() == 0) {
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOperator(last) || last == '(') {
            return;
        }

        int leftCount = countChar('(');
        int rightCount = countChar(')');
        if (leftCount <= rightCount) {
            return;
        }

        expression.append(')');
    }

    public void appendFunction(String functionName) {
        if (justEvaluated) {
            expression.setLength(0);
            justEvaluated = false;
        }

        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (Character.isDigit(last) || last == '.' || last == ')') {
                expression.append('*');
            }
        }

        expression.append(functionName).append('(');
    }

    public void insertAnswer() {
        appendValueToken(formatNumber(lastAnswer));
    }

    public void insertMemoryValue() {
        appendValueToken(formatNumber(memoryValue));
    }

    private void appendValueToken(String valueToken) {
        if (justEvaluated) {
            expression.setLength(0);
            justEvaluated = false;
        }

        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (Character.isDigit(last) || last == '.' || last == ')') {
                expression.append('*');
            }
        }

        if (valueToken.startsWith("-")) {
            expression.append('(').append(valueToken).append(')');
        } else {
            expression.append(valueToken);
        }
    }

    public void deleteLastCharacter() {
        if (expression.length() == 0) {
            return;
        }

        int len = expression.length();
        String[] funcs = {"sin(", "cos(", "tan(", "log(", "sqrt("};
        for (String fn : funcs) {
            if (len >= fn.length() && expression.substring(len - fn.length()).equals(fn)) {
                expression.delete(len - fn.length(), len);
                justEvaluated = false;
                return;
            }
        }

        expression.deleteCharAt(len - 1);
        justEvaluated = false;
    }

    public void clear() {
        expression.setLength(0);
        justEvaluated = false;
    }

    public void toggleSign() {
        if (justEvaluated) {
            justEvaluated = false;
        }

        if (expression.length() == 0) {
            expression.append('-');
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOperator(last) || last == '(') {
            if (!hasPendingUnaryMinus()) {
                expression.append('-');
            }
            return;
        }

        if (last == ')') {
            return;
        }

        int numberStart = getCurrentNumberStart(expression.length());
        if (numberStart == expression.length()) {
            return;
        }

        boolean hasUnaryMinus = numberStart > 0
            && expression.charAt(numberStart - 1) == '-'
            && (numberStart - 1 == 0
                || isOperator(expression.charAt(numberStart - 2))
                || expression.charAt(numberStart - 2) == '(');

        if (hasUnaryMinus) {
            expression.deleteCharAt(numberStart - 1);
        } else {
            expression.insert(numberStart, '-');
        }
    }

    public void convertToPercent() {
        if (justEvaluated) {
            justEvaluated = false;
        }

        if (expression.length() == 0) {
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOperator(last) || last == '(' || last == ')') {
            return;
        }

        int start = getCurrentNumberStart(expression.length());
        if (start == expression.length()) {
            return;
        }

        if (start > 0
            && expression.charAt(start - 1) == '-'
            && (start - 1 == 0
                || isOperator(expression.charAt(start - 2))
                || expression.charAt(start - 2) == '(')) {
            start = start - 1;
        }

        String token = expression.substring(start);
        double value = Double.parseDouble(token) / 100.0;
        String formatted = formatNumber(value);
        expression.replace(start, expression.length(), formatted);
    }

    public double evaluate() throws IllegalArgumentException {
        if (expression.length() == 0) {
            return 0;
        }

        normalizeTrailingInput();
        if (expression.length() == 0) {
            return 0;
        }

        int leftCount = countChar('(');
        int rightCount = countChar(')');
        while (rightCount < leftCount) {
            expression.append(')');
            rightCount++;
        }

        String expressionText = expression.toString();
        try {
            double result = evaluator.evaluate(expressionText);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                throw new IllegalArgumentException("Result out of range");
            }
            String formatted = formatNumber(result);
            historyManager.addHistory(expressionText + " = " + formatted);
            expression.setLength(0);
            expression.append(formatted);
            lastAnswer = result;
            justEvaluated = true;
            return result;
        } catch (IllegalArgumentException ex) {
            throw ex;
        }
    }

    public void handleMemoryOperation(String operation) {
        switch (operation) {
            case "MC":
                memoryValue = 0.0;
                historyManager.addHistory("MC");
                break;
            case "MR":
                insertMemoryValue();
                break;
            case "M+":
                memoryValue += getCurrentDisplayOrExpressionValue();
                historyManager.addHistory("M+ -> " + formatNumber(memoryValue));
                break;
            case "M-":
                memoryValue -= getCurrentDisplayOrExpressionValue();
                historyManager.addHistory("M- -> " + formatNumber(memoryValue));
                break;
        }
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public double getLastAnswer() {
        return lastAnswer;
    }

    public boolean isJustEvaluated() {
        return justEvaluated;
    }

    public void setExpressionFromHistory(String content) {
        expression.setLength(0);
        expression.append(content);
        justEvaluated = false;
    }

    public void setExpressionBeforeHistoryBrowse(String expr) {
        expressionBeforeHistoryBrowse = expr;
    }

    public String getExpressionBeforeHistoryBrowse() {
        return expressionBeforeHistoryBrowse;
    }

    // Private helper methods
    private double getCurrentDisplayOrExpressionValue() {
        if (expression.length() == 0) {
            return 0.0;
        }

        normalizeTrailingInput();
        if (expression.length() == 0) {
            return 0.0;
        }

        try {
            return evaluator.evaluate(expression.toString());
        } catch (IllegalArgumentException ex) {
            return 0.0;
        }
    }

    private void normalizeTrailingInput() {
        while (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (isOperator(last) || last == '(' || hasPendingUnaryMinus()) {
                expression.deleteCharAt(expression.length() - 1);
            } else {
                break;
            }
        }
    }

    private boolean hasPendingUnaryMinus() {
        int len = expression.length();
        if (len == 0 || expression.charAt(len - 1) != '-') {
            return false;
        }
        if (len == 1) {
            return true;
        }
        char before = expression.charAt(len - 2);
        return isOperator(before) || before == '(';
    }

    private int countChar(char target) {
        int count = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private int getCurrentNumberStart(int endExclusive) {
        int i = endExclusive - 1;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                i--;
            } else {
                break;
            }
        }
        return i + 1;
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
