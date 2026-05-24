package com.calculator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionEvaluatorTest {

    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Test
    void evaluatesOperatorPrecedence() {
        assertEquals(14.0, evaluator.evaluate("2+3*4"), 1e-10);
    }

    @Test
    void evaluatesParentheses() {
        assertEquals(20.0, evaluator.evaluate("(2+3)*4"), 1e-10);
    }

    @Test
    void evaluatesRightAssociativePower() {
        assertEquals(512.0, evaluator.evaluate("2^3^2"), 1e-10);
    }

    @Test
    void evaluatesFunctionsInDegrees() {
        assertEquals(1.0, evaluator.evaluate("sin(90)"), 1e-10);
        assertEquals(1.0, evaluator.evaluate("cos(0)"), 1e-10);
    }

    @Test
    void supportsUnaryMinusWithFunction() {
        assertEquals(-2.0, evaluator.evaluate("-sqrt(4)"), 1e-10);
    }

    @Test
    void rejectsDivisionByZero() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("1/0"));
    }

    @Test
    void rejectsMismatchedParentheses() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("(1+2"));
    }
}
