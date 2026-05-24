const http = require('http');
const fs = require('fs');
const path = require('path');
const { DatabaseSync } = require('node:sqlite');

const PORT = process.env.PORT ? Number(process.env.PORT) : 8080;
const ROOT_DIR = process.cwd();
const FRONTEND_DIR = path.join(ROOT_DIR, 'frontend');
const DATA_DIR = path.join(ROOT_DIR, 'data');
const DB_FILE = path.join(DATA_DIR, 'notepad.db');
const LEGACY_NOTES_FILE = path.join(DATA_DIR, 'notepad-notes.json');

let notepadDb = null;

function sendJson(res, status, payload) {
    const body = JSON.stringify(payload);
    res.writeHead(status, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    });
    res.end(body);
}

function resolveMime(filePath) {
    const ext = path.extname(filePath).toLowerCase();
    if (ext === '.html') return 'text/html; charset=utf-8';
    if (ext === '.css') return 'text/css; charset=utf-8';
    if (ext === '.js') return 'application/javascript; charset=utf-8';
    if (ext === '.json') return 'application/json; charset=utf-8';
    if (ext === '.svg') return 'image/svg+xml';
    if (ext === '.png') return 'image/png';
    if (ext === '.jpg' || ext === '.jpeg') return 'image/jpeg';
    return 'application/octet-stream';
}

function isSafePath(candidatePath) {
    const normalizedFrontend = path.resolve(FRONTEND_DIR);
    const normalizedCandidate = path.resolve(candidatePath);
    const relativePath = path.relative(normalizedFrontend, normalizedCandidate);
    return relativePath && !relativePath.startsWith('..') && !path.isAbsolute(relativePath);
}

function parseBody(req) {
    return new Promise((resolve, reject) => {
        let data = '';
        req.on('data', (chunk) => {
            data += chunk;
            if (data.length > 1024 * 1024) {
                reject(new Error('Payload too large'));
                req.destroy();
            }
        });
        req.on('end', () => {
            if (!data) {
                resolve({});
                return;
            }
            try {
                resolve(JSON.parse(data));
            } catch (_error) {
                reject(new Error('Invalid JSON body'));
            }
        });
        req.on('error', reject);
    });
}

function evaluateExpression(expr) {
    if (typeof expr !== 'string' || expr.trim().length === 0) {
        throw new Error('Expression is required');
    }

    const safeExpr = expr
        .replace(/sin\s*\(/g, 'Math.sin((')
        .replace(/cos\s*\(/g, 'Math.cos((')
        .replace(/tan\s*\(/g, 'Math.tan((')
        .replace(/sqrt\s*\(/g, 'Math.sqrt(')
        .replace(/log\s*\(/g, 'Math.log10(')
        .replace(/\^/g, '**')
        .replace(/(sin|cos|tan)\(\(([^)]+)\)\)/g, (_m, fn, inner) => `${fn}((${inner})*Math.PI/180)`);

    const allowed = /^[0-9+\-*/().,%\sA-Za-z*]+$/;
    if (!allowed.test(expr)) {
        throw new Error('Unsupported characters in expression');
    }

    const result = Function(`'use strict'; return (${safeExpr});`)();
    if (!Number.isFinite(result)) {
        throw new Error('Result out of range');
    }
    return Number(result.toPrecision(12));
}

function normalizeNotes(value) {
    if (!Array.isArray(value)) {
        return [];
    }

    const now = new Date().toISOString();
    return value
        .filter((item) => item && typeof item === 'object')
        .map((item, index) => {
            const idValue = typeof item.id === 'string' ? item.id.trim() : '';
            const id = idValue ? idValue.slice(0, 120) : `note_${Date.now()}_${index}`;
            return {
                id,
                title: typeof item.title === 'string' ? item.title.slice(0, 60) : '',
                content: typeof item.content === 'string' ? item.content.slice(0, 5000) : '',
                createdAt: typeof item.createdAt === 'string' && item.createdAt ? item.createdAt : now,
                updatedAt: typeof item.updatedAt === 'string' && item.updatedAt ? item.updatedAt : now
            };
        })
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
}

function getNotepadDb() {
    if (notepadDb) {
        return notepadDb;
    }

    fs.mkdirSync(DATA_DIR, { recursive: true });
    notepadDb = new DatabaseSync(DB_FILE);
    notepadDb.exec(`
        PRAGMA journal_mode = WAL;
        PRAGMA synchronous = NORMAL;
        CREATE TABLE IF NOT EXISTS notes (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL DEFAULT '',
            content TEXT NOT NULL DEFAULT '',
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC);
    `);

    migrateLegacyJsonIfNeeded(notepadDb);
    return notepadDb;
}

function migrateLegacyJsonIfNeeded(db) {
    const row = db.prepare('SELECT COUNT(1) AS total FROM notes').get();
    const total = typeof row.total === 'number' ? row.total : Number(row.total || 0);
    if (total > 0 || !fs.existsSync(LEGACY_NOTES_FILE)) {
        return;
    }

    try {
        const raw = fs.readFileSync(LEGACY_NOTES_FILE, 'utf8');
        const parsed = JSON.parse(raw);
        const notes = normalizeNotes(parsed);
        if (notes.length === 0) {
            return;
        }

        const insert = db.prepare(
            'INSERT OR REPLACE INTO notes (id, title, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)'
        );

        db.exec('BEGIN IMMEDIATE');
        try {
            for (const note of notes) {
                insert.run(note.id, note.title, note.content, note.createdAt, note.updatedAt);
            }
            db.exec('COMMIT');
        } catch (error) {
            db.exec('ROLLBACK');
            throw error;
        }
    } catch (_error) {
        // Ignore migration errors and keep a clean database.
    }
}

function readNotepadNotes() {
    const db = getNotepadDb();
    const rows = db.prepare(
        `SELECT id, title, content, created_at AS createdAt, updated_at AS updatedAt
         FROM notes
         ORDER BY updated_at DESC`
    ).all();
    return normalizeNotes(rows);
}

function writeNotepadNotes(notes) {
    const normalized = normalizeNotes(notes);
    const db = getNotepadDb();
    const insert = db.prepare(
        'INSERT OR REPLACE INTO notes (id, title, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)'
    );

    db.exec('BEGIN IMMEDIATE');
    try {
        db.exec('DELETE FROM notes');
        for (const note of normalized) {
            insert.run(note.id, note.title, note.content, note.createdAt, note.updatedAt);
        }
        db.exec('COMMIT');
    } catch (error) {
        db.exec('ROLLBACK');
        throw error;
    }

    return readNotepadNotes();
}

function clearNotepadNotes() {
    const db = getNotepadDb();
    db.exec('DELETE FROM notes');
    return [];
}

const server = http.createServer(async (req, res) => {
    if (!req.url || !req.method) {
        sendJson(res, 400, { ok: false, error: 'Bad request' });
        return;
    }

    const requestUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = requestUrl.pathname;

    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type'
        });
        res.end();
        return;
    }

    if (pathname === '/api/health' && req.method === 'GET') {
        sendJson(res, 200, { ok: true, service: 'mini-projects-local-api' });
        return;
    }

    if (pathname === '/api/evaluate' && req.method === 'POST') {
        try {
            const body = await parseBody(req);
            const result = evaluateExpression(body.expression);
            sendJson(res, 200, { ok: true, result });
        } catch (error) {
            sendJson(res, 400, { ok: false, error: error.message });
        }
        return;
    }

    if (pathname === '/api/notepad/notes' && req.method === 'GET') {
        try {
            const notes = readNotepadNotes();
            sendJson(res, 200, { ok: true, notes });
        } catch (error) {
            sendJson(res, 500, { ok: false, error: error.message });
        }
        return;
    }

    if (pathname === '/api/notepad/notes' && (req.method === 'PUT' || req.method === 'POST')) {
        try {
            const body = await parseBody(req);
            if (!body || !Array.isArray(body.notes)) {
                throw new Error('notes array is required');
            }
            const notes = writeNotepadNotes(body.notes);
            sendJson(res, 200, { ok: true, notes });
        } catch (error) {
            sendJson(res, 400, { ok: false, error: error.message });
        }
        return;
    }

    if (pathname === '/api/notepad/notes' && req.method === 'DELETE') {
        try {
            const notes = clearNotepadNotes();
            sendJson(res, 200, { ok: true, notes });
        } catch (error) {
            sendJson(res, 500, { ok: false, error: error.message });
        }
        return;
    }

    if (req.method !== 'GET') {
        sendJson(res, 405, { ok: false, error: 'Method not allowed' });
        return;
    }

    const urlPath = pathname === '/' ? '/index.html' : pathname;
    const filePath = path.join(FRONTEND_DIR, decodeURIComponent(urlPath));

    if (!isSafePath(filePath)) {
        sendJson(res, 403, { ok: false, error: 'Forbidden' });
        return;
    }

    fs.readFile(filePath, (error, content) => {
        if (error) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            res.end('Not Found');
            return;
        }

        res.writeHead(200, {
            'Content-Type': resolveMime(filePath),
            'Cache-Control': 'no-store'
        });
        res.end(content);
    });
});

server.listen(PORT, () => {
    console.log(`Server started: http://localhost:${PORT}`);
});
