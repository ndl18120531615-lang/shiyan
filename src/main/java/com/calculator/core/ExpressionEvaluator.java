package com.calculator.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class ExpressionEvaluator {

    public double evaluate(String expr) {
        List<String> tokens = tokenize(expr);
        List<String> rpn = toRpn(tokens);
        return evalRpn(rpn);
    }

    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < expr.length()) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isLetter(c)) {
                int start = i;
                while (i < expr.length() && Character.isLetter(expr.charAt(i))) {
                    i++;
                }
                String fn = expr.substring(start, i).toLowerCase(Locale.ROOT);
                if (!isFunction(fn)) {
                    throw new IllegalArgumentException("Unknown function");
                }
                tokens.add(fn);
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                int start = i;
                boolean hasDigit = false;
                boolean hasDot = false;
                while (i < expr.length()) {
                    char current = expr.charAt(i);
                    if (Character.isDigit(current)) {
                        hasDigit = true;
                        i++;
                    } else if (current == '.') {
                        if (hasDot) {
                            throw new IllegalArgumentException("Invalid decimal number");
                        }
                        hasDot = true;
                        i++;
                    } else {
                        break;
                    }
                }
                if (!hasDigit) {
                    throw new IllegalArgumentException("Invalid decimal number");
                }
                tokens.add(expr.substring(start, i));
                continue;
            }

            if (c == '-' && isUnaryMinusContext(tokens)) {
                if (i + 1 < expr.length() && expr.charAt(i + 1) == '(') {
                    tokens.add("-1");
                    tokens.add("*");
                    i++;
                    continue;
                }

                if (i + 1 < expr.length() && Character.isLetter(expr.charAt(i + 1))) {
                    tokens.add("-1");
                    tokens.add("*");
                    i++;
                    continue;
                }

                int start = i;
                i++;
                boolean hasDigit = false;
                boolean hasDot = false;
                while (i < expr.length()) {
                    char current = expr.charAt(i);
                    if (Character.isDigit(current)) {
                        hasDigit = true;
                        i++;
                    } else if (current == '.') {
                        if (hasDot) {
                            throw new IllegalArgumentException("Invalid decimal number");
                        }
                        hasDot = true;
                        i++;
                    } else {
                        break;
                    }
                }
                if (!hasDigit) {
                    throw new IllegalArgumentException("Invalid negative number");
                }
                tokens.add(expr.substring(start, i));
                continue;
            }

            if (isOperator(c) || c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Unsupported token");
        }
        return tokens;
    }

    private List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> operators = new ArrayDeque<>();

        for (String token : tokens) {
            if (isNumberToken(token)) {
                output.add(token);
                continue;
            }

            if (isFunction(token)) {
                operators.push(token);
                continue;
            }

            if ("(".equals(token)) {
                operators.push(token);
                continue;
            }

            if (")".equals(token)) {
                while (!operators.isEmpty() && !"(".equals(operators.peek())) {
                    output.add(operators.pop());
                }
                if (operators.isEmpty()) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
                operators.pop();
                if (!operators.isEmpty() && isFunction(operators.peek())) {
                    output.add(operators.pop());
                }
                continue;
            }

            if (isOperatorToken(token)) {
                while (!operators.isEmpty()
                    && isOperatorOrFunction(operators.peek())
                    && (
                        (isLeftAssociative(token) && precedence(operators.peek()) >= precedence(token))
                            || (!isLeftAssociative(token) && precedence(operators.peek()) > precedence(token))
                    )) {
                    output.add(operators.pop());
                }
                operators.push(token);
                continue;
            }

            throw new IllegalArgumentException("Invalid token");
        }

        while (!operators.isEmpty()) {
            String top = operators.pop();
            if ("(".equals(top) || ")".equals(top)) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(top);
        }

        return output;
    }

    private double evalRpn(List<String> rpn) {
        Deque<Double> stack = new ArrayDeque<>();

        for (String token : rpn) {
            if (isNumberToken(token)) {
                stack.push(Double.parseDouble(token));
                continue;
            }

            if (isFunction(token)) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Missing function argument");
                }
                double value = stack.pop();
                stack.push(applyFunction(token, value));
                continue;
            }

            if (isOperatorToken(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Missing operator argument");
                }
                double right = stack.pop();
                double left = stack.pop();
                stack.push(applyBinaryOperator(token, left, right));
                continue;
            }

            throw new IllegalArgumentException("Invalid RPN token");
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }
        return stack.pop();
    }

    private double applyBinaryOperator(String op, double left, double right) {
        switch (op) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                if (Math.abs(right) < 1e-12) {
                    throw new IllegalArgumentException("Division by zero");
                }
                return left / right;
            case "^":
                return Math.pow(left, right);
            default:
                throw new IllegalArgumentException("Unsupported operator");
        }
    }

    private double applyFunction(String fn, double value) {
        switch (fn) {
            case "sin":
                return Math.sin(Math.toRadians(value));
            case "cos":
                return Math.cos(Math.toRadians(value));
            case "tan":
                return Math.tan(Math.toRadians(value));
            case "log":
                if (value <= 0) {
                    throw new IllegalArgumentException("log domain");
                }
                return Math.log10(value);
            case "sqrt":
                if (value < 0) {
                    throw new IllegalArgumentException("sqrt domain");
                }
                return Math.sqrt(value);
            default:
                throw new IllegalArgumentException("Unsupported function");
        }
    }

    private boolean isUnaryMinusContext(List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String last = tokens.get(tokens.size() - 1);
        return "(".equals(last) || isOperatorToken(last);
    }

    private boolean isLeftAssociative(String token) {
        return !"^".equals(token);
    }

    private int precedence(String token) {
        if ("+".equals(token) || "-".equals(token)) {
            return 1;
        }
        if ("*".equals(token) || "/".equals(token)) {
            return 2;
        }
        if ("^".equals(token)) {
            return 3;
        }
        if (isFunction(token)) {
            return 4;
        }
        return -1;
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private boolean isOperatorToken(String token) {
        return token.length() == 1 && isOperator(token.charAt(0));
    }

    private boolean isFunction(String token) {
        return "sin".equals(token)
            || "cos".equals(token)
            || "tan".equals(token)
            || "log".equals(token)
            || "sqrt".equals(token);
    }

    private boolean isOperatorOrFunction(String token) {
        return isOperatorToken(token) || isFunction(token);
    }

    private boolean isNumberToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        char first = token.charAt(0);
        return Character.isDigit(first) || first == '-' || first == '.';
    }
}
