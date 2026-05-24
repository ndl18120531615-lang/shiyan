(function () {
    const expressionEl = document.getElementById("expression");
    const resultEl = document.getElementById("result");
    const historyListEl = document.getElementById("historyList");
    const clearHistoryButton = document.getElementById("clearHistory");
    const scientificGrid = document.getElementById("scientificGrid");
    const sciToggleButton = document.getElementById("sciToggle");

    const state = {
        expression: "",
        memoryValue: 0,
        lastAnswer: 0,
        history: [],
        scientificMode: true,
        apiAvailable: false
    };

    const functionsMap = {
        sin: (value) => Math.sin(toRadians(value)),
        cos: (value) => Math.cos(toRadians(value)),
        tan: (value) => Math.tan(toRadians(value)),
        log: (value) => {
            if (value <= 0) {
                throw new Error("log domain");
            }
            return Math.log10(value);
        },
        sqrt: (value) => {
            if (value < 0) {
                throw new Error("sqrt domain");
            }
            return Math.sqrt(value);
        }
    };

    function toRadians(degrees) {
        return (degrees * Math.PI) / 180;
    }

    function isOperator(char) {
        return ["+", "-", "*", "/", "^"].includes(char);
    }

    function getLastChar() {
        return state.expression ? state.expression[state.expression.length - 1] : "";
    }

    function updateDisplay(currentResult) {
        expressionEl.textContent = state.expression || "0";
        if (typeof currentResult === "number" && Number.isFinite(currentResult)) {
            resultEl.textContent = formatNumber(currentResult);
        } else if (state.expression.trim() === "") {
            resultEl.textContent = "0";
        }
    }

    function formatNumber(value) {
        if (!Number.isFinite(value)) {
            return "错误";
        }
        return Number(value.toPrecision(12)).toString();
    }

    function appendDigit(digit) {
        const last = getLastChar();
        if (last === ")") {
            state.expression += "*";
        }
        state.expression += digit;
        updateDisplay(peekValue());
    }

    function appendToken(token) {
        const last = getLastChar();
        if (/[0-9.)]/.test(last)) {
            state.expression += "*";
        }
        state.expression += token;
        updateDisplay(peekValue());
    }

    function appendDecimalPoint() {
        const last = getLastChar();
        if (last === ")") {
            state.expression += "*";
        }
        if (!state.expression || isOperator(last) || last === "(") {
            state.expression += "0.";
            updateDisplay(peekValue());
            return;
        }

        let i = state.expression.length - 1;
        while (i >= 0 && /[0-9.]/.test(state.expression[i])) {
            i -= 1;
        }
        const current = state.expression.slice(i + 1);
        if (current.includes(".")) {
            return;
        }
        state.expression += ".";
        updateDisplay(peekValue());
    }

    function appendOperator(op) {
        if (!state.expression) {
            if (op === "-") {
                state.expression = "-";
            }
            updateDisplay(peekValue());
            return;
        }
        const last = getLastChar();
        if (isOperator(last)) {
            state.expression = state.expression.slice(0, -1) + op;
        } else if (last !== "(") {
            state.expression += op;
        } else if (op === "-") {
            state.expression += op;
        }
        updateDisplay(peekValue());
    }

    function appendLeftParenthesis() {
        const last = getLastChar();
        if (/[0-9.)]/.test(last)) {
            state.expression += "*";
        }
        state.expression += "(";
        updateDisplay(peekValue());
    }

    function appendRightParenthesis() {
        if (!state.expression) {
            return;
        }
        const last = getLastChar();
        if (isOperator(last) || last === "(") {
            return;
        }
        const leftCount = (state.expression.match(/\(/g) || []).length;
        const rightCount = (state.expression.match(/\)/g) || []).length;
        if (leftCount <= rightCount) {
            return;
        }
        state.expression += ")";
        updateDisplay(peekValue());
    }

    function appendFunction(fn) {
        const last = getLastChar();
        if (/[0-9.)]/.test(last)) {
            state.expression += "*";
        }
        state.expression += `${fn}(`;
        updateDisplay(peekValue());
    }

    function toggleSign() {
        if (!state.expression) {
            state.expression = "-";
            updateDisplay(peekValue());
            return;
        }

        const last = getLastChar();
        if (isOperator(last) || last === "(") {
            state.expression += "-";
            updateDisplay(peekValue());
            return;
        }

        const match = state.expression.match(/(-?\d+\.?\d*)$/);
        if (!match) {
            return;
        }

        const token = match[0];
        const start = state.expression.length - token.length;
        const nextToken = token.startsWith("-") ? token.slice(1) : `-${token}`;
        state.expression = state.expression.slice(0, start) + nextToken;
        updateDisplay(peekValue());
    }

    function convertToPercent() {
        const match = state.expression.match(/(-?\d+\.?\d*)$/);
        if (!match) {
            return;
        }
        const token = match[0];
        const start = state.expression.length - token.length;
        const value = Number(token) / 100;
        state.expression = state.expression.slice(0, start) + formatNumber(value);
        updateDisplay(peekValue());
    }

    function clearExpression() {
        state.expression = "";
        updateDisplay();
    }

    function deleteLast() {
        if (!state.expression) {
            return;
        }
        const funcs = ["sin(", "cos(", "tan(", "log(", "sqrt("];
        for (const fn of funcs) {
            if (state.expression.endsWith(fn)) {
                state.expression = state.expression.slice(0, -fn.length);
                updateDisplay(peekValue());
                return;
            }
        }
        state.expression = state.expression.slice(0, -1);
        updateDisplay(peekValue());
    }

    function normalizeBeforeEval() {
        while (state.expression.length > 0) {
            const last = getLastChar();
            if (isOperator(last) || last === "(") {
                state.expression = state.expression.slice(0, -1);
            } else {
                break;
            }
        }
        const leftCount = (state.expression.match(/\(/g) || []).length;
        const rightCount = (state.expression.match(/\)/g) || []).length;
        if (leftCount > rightCount) {
            state.expression += ")".repeat(leftCount - rightCount);
        }
    }

    function tokenize(expr) {
        const tokens = [];
        let i = 0;
        while (i < expr.length) {
            const c = expr[i];
            if (/\s/.test(c)) {
                i += 1;
                continue;
            }
            if (/[a-z]/i.test(c)) {
                let start = i;
                while (i < expr.length && /[a-z]/i.test(expr[i])) {
                    i += 1;
                }
                const fn = expr.slice(start, i).toLowerCase();
                if (!functionsMap[fn]) {
                    throw new Error("Unknown function");
                }
                tokens.push(fn);
                continue;
            }
            if (/\d|\./.test(c)) {
                let start = i;
                let dotCount = 0;
                while (i < expr.length && /[\d.]/.test(expr[i])) {
                    if (expr[i] === ".") {
                        dotCount += 1;
                    }
                    i += 1;
                }
                if (dotCount > 1) {
                    throw new Error("Invalid decimal number");
                }
                tokens.push(expr.slice(start, i));
                continue;
            }
            if (c === "-" && isUnaryContext(tokens)) {
                if (expr[i + 1] === "(" || /[a-z]/i.test(expr[i + 1] || "")) {
                    tokens.push("-1");
                    tokens.push("*");
                    i += 1;
                    continue;
                }
                const start = i;
                i += 1;
                while (i < expr.length && /[\d.]/.test(expr[i])) {
                    i += 1;
                }
                tokens.push(expr.slice(start, i));
                continue;
            }
            if (isOperator(c) || c === "(" || c === ")") {
                tokens.push(c);
                i += 1;
                continue;
            }
            throw new Error("Unsupported token");
        }
        return tokens;
    }

    function isUnaryContext(tokens) {
        if (tokens.length === 0) {
            return true;
        }
        const last = tokens[tokens.length - 1];
        return last === "(" || isOperator(last);
    }

    function precedence(token) {
        if (token === "+" || token === "-") {
            return 1;
        }
        if (token === "*" || token === "/") {
            return 2;
        }
        if (token === "^") {
            return 3;
        }
        if (functionsMap[token]) {
            return 4;
        }
        return -1;
    }

    function toRpn(tokens) {
        const output = [];
        const operators = [];
        for (const token of tokens) {
            if (!Number.isNaN(Number(token))) {
                output.push(token);
                continue;
            }
            if (functionsMap[token]) {
                operators.push(token);
                continue;
            }
            if (token === "(") {
                operators.push(token);
                continue;
            }
            if (token === ")") {
                while (operators.length && operators[operators.length - 1] !== "(") {
                    output.push(operators.pop());
                }
                if (!operators.length) {
                    throw new Error("Mismatched parentheses");
                }
                operators.pop();
                if (operators.length && functionsMap[operators[operators.length - 1]]) {
                    output.push(operators.pop());
                }
                continue;
            }
            if (isOperator(token)) {
                while (
                    operators.length &&
                    (isOperator(operators[operators.length - 1]) || functionsMap[operators[operators.length - 1]]) &&
                    (
                        (token !== "^" && precedence(operators[operators.length - 1]) >= precedence(token)) ||
                        (token === "^" && precedence(operators[operators.length - 1]) > precedence(token))
                    )
                ) {
                    output.push(operators.pop());
                }
                operators.push(token);
                continue;
            }
            throw new Error("Invalid token");
        }

        while (operators.length) {
            const top = operators.pop();
            if (top === "(" || top === ")") {
                throw new Error("Mismatched parentheses");
            }
            output.push(top);
        }
        return output;
    }

    function evalRpn(rpn) {
        const stack = [];
        for (const token of rpn) {
            if (!Number.isNaN(Number(token))) {
                stack.push(Number(token));
                continue;
            }
            if (functionsMap[token]) {
                if (!stack.length) {
                    throw new Error("Missing function argument");
                }
                const value = stack.pop();
                stack.push(functionsMap[token](value));
                continue;
            }
            if (isOperator(token)) {
                if (stack.length < 2) {
                    throw new Error("Missing operator argument");
                }
                const right = stack.pop();
                const left = stack.pop();
                stack.push(applyOperator(token, left, right));
                continue;
            }
            throw new Error("Invalid expression");
        }
        if (stack.length !== 1) {
            throw new Error("Invalid expression");
        }
        return stack[0];
    }

    function applyOperator(op, left, right) {
        if (op === "+") {
            return left + right;
        }
        if (op === "-") {
            return left - right;
        }
        if (op === "*") {
            return left * right;
        }
        if (op === "/") {
            if (Math.abs(right) < 1e-12) {
                throw new Error("Division by zero");
            }
            return left / right;
        }
        if (op === "^") {
            return Math.pow(left, right);
        }
        throw new Error("Unsupported operator");
    }

    async function detectApiAvailability() {
        try {
            const response = await fetch("/api/health", { method: "GET" });
            state.apiAvailable = response.ok;
        } catch (_error) {
            state.apiAvailable = false;
        }
    }

    async function evaluateByApi(expression) {
        const response = await fetch("/api/evaluate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ expression })
        });
        const payload = await response.json();
        if (!response.ok || !payload.ok) {
            throw new Error(payload.error || "API evaluation failed");
        }
        return Number(payload.result);
    }

    async function evaluateCurrentExpression() {
        if (!state.expression) {
            resultEl.textContent = "0";
            return;
        }
        normalizeBeforeEval();
        if (!state.expression) {
            resultEl.textContent = "0";
            return;
        }
        try {
            const result = state.apiAvailable
                ? await evaluateByApi(state.expression)
                : evalRpn(toRpn(tokenize(state.expression)));

            if (!Number.isFinite(result)) {
                throw new Error("Result out of range");
            }

            const formatted = formatNumber(result);
            state.lastAnswer = result;
            addHistory(state.expression, formatted);
            state.expression = formatted;
            updateDisplay(result);
        } catch (_error) {
            resultEl.textContent = "错误";
        }
    }

    function peekValue() {
        if (!state.expression) {
            return 0;
        }
        try {
            const value = evalRpn(toRpn(tokenize(state.expression)));
            return Number.isFinite(value) ? value : undefined;
        } catch (_error) {
            return undefined;
        }
    }

    function addHistory(expression, result) {
        state.history.unshift({ expression, result });
        if (state.history.length > 60) {
            state.history.pop();
        }
        renderHistory();
    }

    function renderHistory() {
        historyListEl.innerHTML = "";
        state.history.forEach((entry) => {
            const item = document.createElement("li");
            item.innerHTML = `<div>${entry.expression}</div><div class="line-result">= ${entry.result}</div>`;
            item.addEventListener("click", () => {
                state.expression = entry.result;
                updateDisplay(peekValue());
            });
            item.addEventListener("dblclick", () => {
                state.expression = entry.expression;
                updateDisplay(peekValue());
            });
            historyListEl.appendChild(item);
        });
    }

    function clearHistory() {
        state.history = [];
        renderHistory();
    }

    function handleMemory(action) {
        if (action === "MC") {
            state.memoryValue = 0;
            return;
        }
        if (action === "MR") {
            const token = formatNumber(state.memoryValue);
            appendToken(token.startsWith("-") ? `(${token})` : token);
            return;
        }
        const current = peekValue();
        const value = typeof current === "number" && Number.isFinite(current) ? current : 0;
        if (action === "M+") {
            state.memoryValue += value;
        } else if (action === "M-") {
            state.memoryValue -= value;
        }
    }

    function toggleScientificMode() {
        state.scientificMode = !state.scientificMode;
        scientificGrid.classList.toggle("hidden", !state.scientificMode);
        sciToggleButton.textContent = state.scientificMode ? "科学开" : "科学关";
    }

    function insertAnswer() {
        const token = formatNumber(state.lastAnswer);
        appendToken(token.startsWith("-") ? `(${token})` : token);
    }

    async function handleAction(action) {
        if (/^\d$/.test(action)) {
            appendDigit(action);
            return;
        }
        if (["MC", "MR", "M+", "M-"].includes(action)) {
            handleMemory(action);
            return;
        }
        if (["+", "-", "*", "/", "^"].includes(action)) {
            appendOperator(action);
            return;
        }
        if (["sin", "cos", "tan", "log", "sqrt"].includes(action)) {
            appendFunction(action);
            return;
        }

        switch (action) {
            case ".":
                appendDecimalPoint();
                break;
            case "(":
                appendLeftParenthesis();
                break;
            case ")":
                appendRightParenthesis();
                break;
            case "+/-":
                toggleSign();
                break;
            case "%":
                convertToPercent();
                break;
            case "C":
                clearExpression();
                break;
            case "DEL":
                deleteLast();
                break;
            case "=":
                await evaluateCurrentExpression();
                break;
            case "Ans":
                insertAnswer();
                break;
            case "SCI":
                toggleScientificMode();
                break;
            default:
                break;
        }
    }

    function installEventListeners() {
        document.querySelectorAll("button[data-action]").forEach((button) => {
            button.addEventListener("click", async () => {
                const action = button.getAttribute("data-action");
                if (action) {
                    await handleAction(action);
                }
            });
        });

        clearHistoryButton.addEventListener("click", clearHistory);

        document.addEventListener("keydown", async (event) => {
            const key = event.key;
            if (/^\d$/.test(key)) {
                await handleAction(key);
                return;
            }
            if (key === "Enter" || key === "=") {
                event.preventDefault();
                await handleAction("=");
                return;
            }
            if (key === "Backspace") {
                await handleAction("DEL");
                return;
            }
            if (key === "Escape") {
                await handleAction("C");
                return;
            }
            if (key === "(" || key === "[") {
                await handleAction("(");
                return;
            }
            if (key === ")" || key === "]") {
                await handleAction(")");
                return;
            }
            if (["+", "-", "*", "/", "^", ".", "%"].includes(key)) {
                await handleAction(key);
            }
        });
    }

    installEventListeners();
    detectApiAvailability();
    updateDisplay();
})();
