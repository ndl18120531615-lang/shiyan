# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This repo contains two independent sub-projects that share the same working directory:

1. **Java Swing Calculator** — desktop GUI app (Maven, Java 17+)
2. **Frontend Project Hub** — static web pages + Node.js local server

---

## Commands

### Java Swing Calculator

**Build (skip test compilation — test file has a UTF-8 BOM issue):**
```bash
/d/Java/apache-maven-3.9.9/bin/mvn package -Dmaven.test.skip=true
```

**Run (after building):**
```bash
/d/Java/jdk/jdk-21_windows-x64_bin/jdk-21.0.10/bin/java -jar target/swing-calculator-1.0.0.jar
```

**Run directly with Maven (compiles and launches):**
```bash
/d/Java/apache-maven-3.9.9/bin/mvn -q exec:java -Dmaven.test.skip=true
```

**Run without Maven (compile from sources):**
```bash
/d/Java/jdk/jdk-21_windows-x64_bin/jdk-21.0.10/bin/javac -d out $(find src/main/java -name "*.java")
/d/Java/jdk/jdk-21_windows-x64_bin/jdk-21.0.10/bin/java -cp out com.calculator.app.SimpleCalculator
```

**Run tests** (note: `ExpressionEvaluatorTest.java` has a BOM character — fix by re-saving as UTF-8 without BOM before running):
```bash
/d/Java/apache-maven-3.9.9/bin/mvn test
```

**Run a single test:**
```bash
/d/Java/apache-maven-3.9.9/bin/mvn test -Dtest=ExpressionEvaluatorTest
```

### Frontend / Node.js server

**Start the server (must be run from the project root):**
```bash
node server/server.js
```
Serves at `http://localhost:8080`. Use `PORT=<n>` env var to change the port.

**Pages:**
- Portal: `http://localhost:8080/`
- Calculator: `http://localhost:8080/projects/calculator/index.html`
- Notepad: `http://localhost:8080/projects/notepad/index.html`

**Smoke test API:**
```bash
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/evaluate -H "Content-Type: application/json" -d '{"expression":"(2+3)*4"}'
```

---

## Architecture

### Java: layered MVC

```
app/          — JFrame entry point (SimpleCalculator); wires model + view + controller
core/         — Pure domain logic; no Swing dependencies
  CalculatorModel       — mutable expression buffer, memory, history calls, evaluation trigger
  ExpressionEvaluator   — tokenizer → Shunting-Yard RPN → stack evaluator; supports sin/cos/tan/log/sqrt
persistence/  — HistoryManager: reads/writes calculator_history.txt (relative to CWD)
config/       — AppConfig singleton: ~/.calculator/config.properties (theme, font size, window bounds)
ui/           — CalculatorView: builds all Swing panels, exposes getButtonMap() and getHistoryList()
ui/controller/ — CalculatorController: bridges button events + keyboard bindings → CalculatorModel calls
```

Key invariants:
- `CalculatorModel` never touches Swing. All UI updates go through `CalculatorController.updateDisplay()`.
- `ExpressionEvaluator.evaluate()` is stateless and throws `IllegalArgumentException` on bad input.
- `HistoryManager` writes to `calculator_history.txt` in the **current working directory** — the app must be launched from the project root.
- `AppConfig` is a singleton; window position/size are persisted on `windowClosing`.

### Node.js server (`server/server.js`)

Single-file HTTP server with no framework or npm dependencies. Requires Node ≥ 22 (uses `node:sqlite` built-in).

- Static files are served from `frontend/` with a path-traversal guard (`isSafePath`).
- API routes are all under `/api/`. Expression evaluation uses `Function()` with an allowlist regex.
- Notepad storage: SQLite via `node:sqlite` (`data/notepad.db`), WAL mode. On first start with an empty DB it auto-migrates from the legacy `data/notepad-notes.json` if present.
- Note fields are capped: title ≤ 60 chars, content ≤ 5000 chars, id ≤ 120 chars.

### Frontend (`frontend/`)

Vanilla JS, no build step, no bundler.

- `frontend/index.html` + `portal.css` — project hub landing page
- `frontend/projects/calculator/` — independent mini-app; calls `/api/evaluate` for server-side evaluation
- `frontend/projects/notepad/app.js` — checks server availability on load; falls back to `localStorage` (`mini_projects_notepad_v1`) when the server is unreachable

### Known issues

- `ExpressionEvaluatorTest.java` is saved with a UTF-8 BOM, causing `javac` to fail. Re-save the file as UTF-8 without BOM to enable tests.
- `mvn test` and `mvn package` (without `-Dmaven.test.skip=true`) will fail until the BOM is removed.
