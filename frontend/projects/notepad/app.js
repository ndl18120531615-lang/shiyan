(function () {
    const STORAGE_KEY = "mini_projects_notepad_v1";
    const NOTES_API = "/api/notepad/notes";

    const noteTitle = document.getElementById("noteTitle");
    const noteContent = document.getElementById("noteContent");
    const saveBtn = document.getElementById("saveBtn");
    const newNoteBtn = document.getElementById("newNoteBtn");
    const resetBtn = document.getElementById("resetBtn");
    const deleteBtn = document.getElementById("deleteBtn");
    const clearAllBtn = document.getElementById("clearAllBtn");
    const searchInput = document.getElementById("searchInput");
    const noteList = document.getElementById("noteList");
    const editState = document.getElementById("editState");

    const state = {
        notes: [],
        selectedId: null,
        keyword: "",
        serverAvailable: false
    };

    function nowIso() {
        return new Date().toISOString();
    }

    function formatTime(isoString) {
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return "未知时间";
        }
        return date.toLocaleString("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function normalizeNotes(value) {
        if (!Array.isArray(value)) {
            return [];
        }
        return value
            .filter((item) => item && typeof item.id === "string")
            .map((item) => ({
                id: String(item.id),
                title: String(item.title || ""),
                content: String(item.content || ""),
                createdAt: String(item.createdAt || nowIso()),
                updatedAt: String(item.updatedAt || nowIso())
            }))
            .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    }

    function loadNotesFromLocal() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return [];
            }
            const parsed = JSON.parse(raw);
            return normalizeNotes(parsed);
        } catch (_error) {
            return [];
        }
    }

    function saveNotesToLocal() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state.notes));
    }

    async function loadNotesFromServer() {
        const response = await fetch(NOTES_API, { method: "GET" });
        const payload = await response.json();
        if (!response.ok || !payload.ok || !Array.isArray(payload.notes)) {
            throw new Error(payload.error || "服务端读取失败");
        }
        return normalizeNotes(payload.notes);
    }

    async function saveNotesToServer() {
        const response = await fetch(NOTES_API, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ notes: state.notes })
        });
        const payload = await response.json();
        if (!response.ok || !payload.ok || !Array.isArray(payload.notes)) {
            throw new Error(payload.error || "服务端保存失败");
        }
        state.notes = normalizeNotes(payload.notes);
    }

    async function clearNotesOnServer() {
        const response = await fetch(NOTES_API, { method: "DELETE" });
        const payload = await response.json();
        if (!response.ok || !payload.ok) {
            throw new Error(payload.error || "服务端清空失败");
        }
        state.notes = [];
    }

    async function persistNotes() {
        if (state.serverAvailable) {
            await saveNotesToServer();
        }
        saveNotesToLocal();
    }

    async function initStorage() {
        try {
            state.notes = await loadNotesFromServer();
            state.serverAvailable = true;
            saveNotesToLocal();
        } catch (_error) {
            state.serverAvailable = false;
            state.notes = loadNotesFromLocal();
        }
    }

    function resetEditor(keepSelection) {
        noteTitle.value = "";
        noteContent.value = "";
        if (!keepSelection) {
            state.selectedId = null;
        }
        updateEditState();
    }

    function updateEditState() {
        if (state.selectedId) {
            editState.textContent = state.serverAvailable ? "编辑模式（已永久保存）" : "编辑模式（本地模式）";
            deleteBtn.disabled = false;
            saveBtn.textContent = "保存修改";
        } else {
            editState.textContent = state.serverAvailable ? "新建模式（已永久保存）" : "新建模式（本地模式）";
            deleteBtn.disabled = true;
            saveBtn.textContent = "保存笔记";
        }
    }

    function createPreview(text) {
        const normalized = text.replace(/\s+/g, " ").trim();
        if (!normalized) {
            return "(无内容)";
        }
        return normalized.length > 70 ? `${normalized.slice(0, 70)}...` : normalized;
    }

    function getFilteredNotes() {
        const kw = state.keyword.trim().toLowerCase();
        if (!kw) {
            return state.notes;
        }
        return state.notes.filter((note) => {
            return note.title.toLowerCase().includes(kw) || note.content.toLowerCase().includes(kw);
        });
    }

    function renderNoteList() {
        noteList.innerHTML = "";
        const filtered = getFilteredNotes();
        if (filtered.length === 0) {
            const empty = document.createElement("li");
            empty.className = "empty";
            empty.textContent = state.notes.length === 0 ? "还没有笔记，先创建第一条。" : "没有匹配的笔记。";
            noteList.appendChild(empty);
            return;
        }

        filtered.forEach((note) => {
            const li = document.createElement("li");
            li.className = "note-item";
            if (note.id === state.selectedId) {
                li.classList.add("active");
            }

            li.innerHTML = `
                <h3 class="note-title">${escapeHtml(note.title || "未命名笔记")}</h3>
                <p class="note-meta">更新于 ${formatTime(note.updatedAt)}</p>
                <p class="note-preview">${escapeHtml(createPreview(note.content))}</p>
            `;

            li.addEventListener("click", () => {
                selectNote(note.id);
            });

            noteList.appendChild(li);
        });
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function selectNote(id) {
        const note = state.notes.find((item) => item.id === id);
        if (!note) {
            return;
        }
        state.selectedId = id;
        noteTitle.value = note.title;
        noteContent.value = note.content;
        updateEditState();
        renderNoteList();
    }

    function createId() {
        return `note_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    }

    async function upsertNote() {
        const title = noteTitle.value.trim();
        const content = noteContent.value.trim();

        if (!title && !content) {
            alert("标题和内容不能同时为空。");
            return;
        }

        if (state.selectedId) {
            const target = state.notes.find((item) => item.id === state.selectedId);
            if (!target) {
                state.selectedId = null;
                updateEditState();
                return;
            }
            target.title = title;
            target.content = content;
            target.updatedAt = nowIso();
        } else {
            const createdAt = nowIso();
            const note = {
                id: createId(),
                title,
                content,
                createdAt,
                updatedAt: createdAt
            };
            state.notes.unshift(note);
            state.selectedId = note.id;
        }

        state.notes.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

        try {
            await persistNotes();
        } catch (error) {
            alert(`保存失败：${error.message}`);
            return;
        }

        updateEditState();
        renderNoteList();
    }

    async function deleteCurrentNote() {
        if (!state.selectedId) {
            return;
        }

        const target = state.notes.find((item) => item.id === state.selectedId);
        if (!target) {
            state.selectedId = null;
            updateEditState();
            return;
        }

        const ok = confirm(`确定删除笔记「${target.title || "未命名笔记"}」吗？`);
        if (!ok) {
            return;
        }

        state.notes = state.notes.filter((item) => item.id !== state.selectedId);
        state.selectedId = null;

        try {
            await persistNotes();
        } catch (error) {
            alert(`删除失败：${error.message}`);
            return;
        }

        resetEditor(true);
        renderNoteList();
    }

    async function clearAllNotes() {
        if (state.notes.length === 0) {
            return;
        }

        const ok = confirm("确定清空全部笔记吗？此操作不可撤销。");
        if (!ok) {
            return;
        }

        state.notes = [];
        state.selectedId = null;

        try {
            if (state.serverAvailable) {
                await clearNotesOnServer();
                saveNotesToLocal();
            } else {
                saveNotesToLocal();
            }
        } catch (error) {
            alert(`清空失败：${error.message}`);
            return;
        }

        resetEditor(true);
        renderNoteList();
    }

    function installEvents() {
        saveBtn.addEventListener("click", async () => {
            await upsertNote();
        });

        newNoteBtn.addEventListener("click", () => {
            resetEditor(false);
            noteTitle.focus();
            renderNoteList();
        });

        resetBtn.addEventListener("click", () => {
            resetEditor(false);
            renderNoteList();
        });

        deleteBtn.addEventListener("click", async () => {
            await deleteCurrentNote();
        });

        clearAllBtn.addEventListener("click", async () => {
            await clearAllNotes();
        });

        searchInput.addEventListener("input", () => {
            state.keyword = searchInput.value;
            renderNoteList();
        });
    }

    async function boot() {
        await initStorage();
        updateEditState();
        renderNoteList();
        installEvents();
    }

    boot();
})();
