# Swing Calculator

A Java Swing desktop calculator with expression parsing, history, memory operations, and scientific functions.

## Project layout

- `src/main/java/com/calculator/app`: application entrypoint
- `src/main/java/com/calculator/core`: domain logic and expression evaluator
- `src/main/java/com/calculator/ui`: Swing view and theme
- `src/main/java/com/calculator/ui/controller`: UI controller and event handling
- `src/main/java/com/calculator/config`: app configuration persistence
- `src/main/java/com/calculator/persistence`: history persistence
- `src/test/java/com/calculator/core`: unit tests
- `frontend/index.html`: 小项目集合主页面
- `frontend/projects/calculator`: 计算器前端子项目
- `frontend/projects/notepad`: 简易记事本前端子项目
- `server/server.js`: local static + API server for frontend
- `data/notepad.db`: 记事本永久存储文件（服务端写入）
- `pom.xml`: Maven build definition
- `calculator_history.txt`: persisted history entries

## Run Java app locally (without Maven)

```powershell
$sources = Get-ChildItem -Recurse -Path src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $sources
java -cp out com.calculator.app.SimpleCalculator
```

## Run Java app with Maven (recommended)

After installing Maven:

```powershell
mvn test
mvn -q exec:java
```

## Run frontend project hub

```powershell
node server/server.js
```

Then open `http://localhost:8080`.

Pages:
- 项目集合主页: `/`
- 计算器: `/projects/calculator/index.html`
- 简易记事本: `/projects/notepad/index.html`

API endpoints:
- `GET /api/health`
- `POST /api/evaluate` with body: `{ "expression": "(2+3)*4" }`
- `GET /api/notepad/notes` 读取记事本
- `PUT /api/notepad/notes` 覆盖保存记事本，body: `{ "notes": [...] }`
- `DELETE /api/notepad/notes` 清空记事本

## Notepad persistence

- 记事本优先使用服务端永久存储，数据写入 `data/notepad.db`。
- 当服务端不可用时，会自动回退到浏览器本地存储。

## Upgrades included

- Scientific mode toggle now actually hides/shows the scientific function panel.
- Window size and position are now restored from config on startup.
- Font size from config is now applied to UI components.
- Maven build file and baseline JUnit tests added for regression checks.
- Source code reorganized into layered packages for maintainability.
- Added standalone frontend project hub and split frontend mini-projects.
- Added lightweight Node local server for frontend hosting and API placeholder integration.
- Added server-side persistent storage for notepad.
