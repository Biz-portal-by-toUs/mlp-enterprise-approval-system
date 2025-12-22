/* ===============================
 * doc-write.js (stable + header/category render + policy) - REBASED FIX
 * 기반: 사용자 제공 코드
 *
 * Fixes:
 * 1) FontSize command is chainable (no .run() inside command)
 * 2) Font size dropdown (#fontSizeSelect) change handler added
 * 3) Insert table inside editable cell works (1회 정책 우회 + created 마킹)
 * 4) Inner(created) table cells are editable + row/col ops only in created inner tables
 * 5) PolicyGuard.filterTransaction ALWAYS returns boolean (no implicit undefined)
 * 6) InputField 유지 (사라지지 않도록 extensions/NodeView 보장)
 * =============================== */

const TEMPLATE_GET_URL = (id) => `/api/tiptap/templates/${encodeURIComponent(id)}`

/* ---------------- DOM helpers ---------------- */

const qs = (s, el = document) => el.querySelector(s)

function ensureBox(id, insertAfterSelector) {
    let el = document.getElementById(id)
    if (el) return el
    el = document.createElement("div")
    el.id = id

    const after = qs(insertAfterSelector)
    if (after && after.parentElement) {
        after.parentElement.insertBefore(el, after.nextSibling)
    } else {
        document.body.appendChild(el)
    }
    return el
}

function findAncestorNode(state, predicate) {
    const sel = state.selection

    // CellSelection 대응
    const base =
        sel.$anchorCell ||
        sel.$headCell ||
        sel.$from ||
        sel.$anchor ||
        sel.$head

    if (!base) return null

    for (let d = base.depth; d >= 0; d--) {
        const node = base.node(d)
        if (predicate(node)) return node
    }
    return null
}

// ===== table helpers =====
function isInTable(state) {
    return !!findAncestorNode(state, (n) => n?.type?.name === "table")
}

/* ---------------- policy ---------------- */

function normalizeText(s) {
    return (s || "").replace(/[\s\u200B\uFEFF]/g, "")
}

/**
 * 편집/스타일/툴바 허용 조건
 * - inputField: 항상 허용
 * - tableHeader: 항상 금지
 * - tableCell: (A) created 내부표 안이면 항상 허용 (셀별 editable 없어도 됨)
 *            (B) 템플릿 바디셀: 최초 빈 셀만 editable=true로 허용
 * - 표 밖 일반 텍스트: 금지
 */
function canEditContent(state) {
    const inputField = findAncestorNode(state, (n) => n.type?.name === "inputField")
    if (inputField) return true

    const header = findAncestorNode(state, (n) => n.type?.name === "tableHeader")
    if (header) return false

    const cell = findAncestorNode(state, (n) => n.type?.name === "tableCell")
    if (cell) {
        // ✅ 내부(created) 표 안에서는 모두 허용
        if (typeof isInNewTable === "function" && isInNewTable(state)) return true
        return cell.attrs?.editable === true
    }

    return false
}

// 트랜잭션 검사 시 "새 selection/doc" 기준으로 판정하기 위한 유틸
const POLICY_ALLOW_META = "__policy_allow__"
// ✅ insertTable 직후 selection이 새 표 셀로 이동하면서 정책에 막혀 롤백되는 걸 1회 우회
let __bypassPolicyOnce__ = false

function stateFromTr(tr, state) {
    if (tr && tr.doc) {
        const sel = tr.selection || state.selection
        try {
            if (sel && sel.$from && sel.$from.doc === tr.doc) return { doc: tr.doc, selection: sel }
        } catch {}
        const mappedFrom = tr.mapping ? tr.mapping.map(state.selection.from) : state.selection.from
        const $from = tr.doc.resolve(mappedFrom)
        // 최소 selection 호환 객체
        return { doc: tr.doc, selection: { $from, from: mappedFrom, to: mappedFrom } }
    }
    return state
}

function extractTextFromJson(node) {
    if (!node) return ""
    if (node.type === "text") return node.text || ""
    let out = ""
    if (Array.isArray(node.content)) {
        for (const c of node.content) out += extractTextFromJson(c)
    }
    return out
}

function precomputeCellEditableJson(json) {
    if (!json || typeof json !== "object") return json

    const walk = (node, inHeader = false) => {
        if (!node || typeof node !== "object") return
        const type = node.type
        const attrs = node.attrs || (node.attrs = {})

        if (type === "tableHeader") {
            attrs.editable = false
            inHeader = true
        }

        if (type === "tableCell") {
            if (typeof attrs.editable !== "boolean") {
                const txt = normalizeText(extractTextFromJson(node))
                attrs.editable = (txt === "") && !inHeader
            } else {
                if (inHeader) attrs.editable = false
            }
        }

        if (Array.isArray(node.content)) node.content.forEach((child) => walk(child, inHeader))
    }

    walk(json, false)
    return json
}

/* ---------------- template fetch ---------------- */

async function fetchTemplatePayload() {
    const id = new URLSearchParams(location.search).get("id")
    const res = await fetch(TEMPLATE_GET_URL(id))
    if (!res.ok) throw new Error(`template fetch failed: ${res.status}`)
    return res.json()
}

function extractTemplateJson(payload) {
    const candidates = [
        payload?.templateJson,
        payload?.json,
        payload?.cntt_json,
        payload?.data?.templateJson,
        payload?.data?.json,
        payload?.data?.cntt_json,
        payload?.template?.cntt_json,
        payload?.template?.templateJson,
    ]

    const raw = candidates.find((v) => v != null)
    if (!raw) return null

    if (typeof raw === "string") {
        try {
            return JSON.parse(raw)
        } catch {
            return null
        }
    }
    if (typeof raw === "object") return raw
    return null
}

// 헤더 HTML 후보
function extractHeaderHtml(payload) {
    const direct =
        payload?.headerHtml ||
        payload?.presetHeaderHtml ||
        payload?.header_html ||
        payload?.preset_header_html ||
        payload?.data?.headerHtml ||
        payload?.data?.presetHeaderHtml ||
        payload?.template?.headerHtml

    if (direct) return direct

    const left = payload?.uiState?.presetTables?.leftHtml
    const right = payload?.uiState?.presetTables?.rightHtml

    if (left || right) {
        return JSON.stringify({ left: left ?? "", right: right ?? "" })
    }
    return null
}

function extractCategories(payload) {
    const v =
        payload?.uiState?.templateTypes ||
        payload?.categories ||
        payload?.categoryList ||
        payload?.categoryOptions ||
        payload?.data?.categories ||
        payload?.data?.categoryList ||
        null

    if (!v) return []
    if (Array.isArray(v)) return v
    if (typeof v === "string") return v.split(",").map((x) => x.trim()).filter(Boolean)
    return []
}

/* ---------------- render header & categories ---------------- */

function renderPresetHeader(payload) {
    const html = extractHeaderHtml(payload)
    if (!html) return

    // doc-write.html의 presetHeaderBox 안에 render.js와 동일한 DOM 구조를 만들어서
    // tiptap-preset-header.css(.tpl-top / [data-tpl-left]/[data-tpl-right]) 규칙이 그대로 적용되게 한다.
    const box = document.getElementById("presetHeaderBox") || ensureBox("presetHeaderBox", "div#categoryBox")

    // uiState 기반은 JSON 문자열로 전달해두었음 (left/right HTML)
    let leftHtml = ""
    let rightHtml = ""

    if (typeof html === "string" && html.trim().startsWith("{")) {
        try {
            const parsed = JSON.parse(html)
            leftHtml = parsed.left ?? ""
            rightHtml = parsed.right ?? ""
        } catch {
            // fallthrough
        }
    }

    if (!leftHtml && !rightHtml) {
        // direct html(단일 문자열)인 경우: 기존 호환 유지 (그대로 넣되 .tpl-top 래퍼를 제공)
        leftHtml = html
        rightHtml = ""
    }

    box.innerHTML = `
      <div class="tpl-top topGrid">
        <div class="tpl-side" data-tpl-left data-left>
          ${leftHtml || `<span class="tpl-muted">-</span>`}
        </div>

        <div class="tpl-spacer"></div>

        <div class="tpl-side" data-tpl-right data-right>
          ${rightHtml || `<span class="tpl-muted">-</span>`}
        </div>
      </div>
    `
}

function renderCategories(payload) {
    const categories = extractCategories(payload)
    if (!categories.length) return

    const root = document.getElementById("typeRadios") || ensureBox("typeRadios", "div#presetHeaderBox")
    if (root.dataset.rendered === "1") return
    root.dataset.rendered = "1"

    root.innerHTML = ""

    categories.forEach((c, idx) => {
        const label = (typeof c === "string") ? c : (c.name ?? c.label ?? c.value ?? `type${idx + 1}`)
        const value = (typeof c === "string") ? c : (c.value ?? c.id ?? label)
        const id = `cat_${idx}_${String(value).replace(/\W+/g, "_")}`

        const lab = document.createElement("label")
        lab.setAttribute("for", id)

        const input = document.createElement("input")
        input.type = "radio"
        input.name = "docCategory"
        input.value = value
        input.id = id

        const span = document.createElement("span")
        span.textContent = label

        lab.appendChild(input)
        lab.appendChild(span)
        root.appendChild(lab)
    })
}

/* ---------------- robust imports (CDN fallback) ---------------- */

async function importModuleWithFallback(urls, label) {
    let lastErr = null
    for (const url of urls) {
        try {
            const m = await import(url)
            return m
        } catch (e) {
            lastErr = e
        }
    }
    console.error(`[import fail] ${label}`, lastErr)
    throw lastErr
}

function pickExport(mod, exportName) {
    if (!mod) return undefined
    if (mod.default) return mod.default
    if (exportName && mod[exportName]) return mod[exportName]
    for (const k of Object.keys(mod)) {
        const v = mod[k]
        if (typeof v === "function" || (typeof v === "object" && v)) return v
    }
    return undefined
}

/* ---------------- boot ---------------- */

// 전역 함수 참조를 위해 선언(boot 안에서 정의됨)
let isInNewTable = null

async function boot() {
    const payload = await fetchTemplatePayload()
    renderCategories(payload)
    renderPresetHeader(payload)

    const templateJson = extractTemplateJson(payload)

    // TipTap modules
    const core = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/core", "https://unpkg.com/@tiptap/core?module", "https://cdn.jsdelivr.net/npm/@tiptap/core/+esm"],
        "@tiptap/core"
    )

    const Editor = core.Editor
    const Extension = core.Extension
    const Node = core.Node

    const starterKitMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/starter-kit", "https://unpkg.com/@tiptap/starter-kit?module", "https://cdn.jsdelivr.net/npm/@tiptap/starter-kit/+esm"],
        "@tiptap/starter-kit"
    )
    const StarterKit = pickExport(starterKitMod, "StarterKit")

    const tableMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-table", "https://unpkg.com/@tiptap/extension-table?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-table/+esm"],
        "@tiptap/extension-table"
    )
    const Table = pickExport(tableMod, "Table")

    const tableRowMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-table-row", "https://unpkg.com/@tiptap/extension-table-row?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-table-row/+esm"],
        "@tiptap/extension-table-row"
    )
    const TableRow = pickExport(tableRowMod, "TableRow")

    const tableCellMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-table-cell", "https://unpkg.com/@tiptap/extension-table-cell?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-table-cell/+esm"],
        "@tiptap/extension-table-cell"
    )
    const TableCell = pickExport(tableCellMod, "TableCell")

    const tableHeaderMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-table-header", "https://unpkg.com/@tiptap/extension-table-header?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-table-header/+esm"],
        "@tiptap/extension-table-header"
    )
    const TableHeader = pickExport(tableHeaderMod, "TableHeader")

    // ✅ tableCell/tableHeader가 attrs.editable을 "허용"하도록 스키마 확장
    const CustomTableCell = TableCell.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                editable: { default: false },
            }
        },
    })

    const CustomTableHeader = TableHeader.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                editable: { default: false },
            }
        },
    })

    const textAlignMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-text-align", "https://unpkg.com/@tiptap/extension-text-align?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-text-align/+esm"],
        "@tiptap/extension-text-align"
    )
    const TextAlign = pickExport(textAlignMod, "TextAlign")

    const textStyleMod = await importModuleWithFallback(
        ["https://esm.sh/@tiptap/extension-text-style", "https://unpkg.com/@tiptap/extension-text-style?module", "https://cdn.jsdelivr.net/npm/@tiptap/extension-text-style/+esm"],
        "@tiptap/extension-text-style"
    )
    const TextStyle = pickExport(textStyleMod, "TextStyle")

    // ---- FontSize (TextStyle 기반) ----
    // ✅ chainable command로 수정 (내부에서 .run() 호출 금지)
    const FontSize = Extension.create({
        name: "fontSize",
        addGlobalAttributes() {
            return [
                {
                    types: ["textStyle"],
                    attributes: {
                        fontSize: {
                            default: null,
                            parseHTML: (element) => element.style.fontSize || null,
                            renderHTML: (attrs) => {
                                if (!attrs.fontSize) return {}
                                return { style: `font-size: ${attrs.fontSize}` }
                            },
                        },
                    },
                },
            ]
        },
        addCommands() {
            return {
                setFontSize:
                    (fontSize) =>
                        ({ chain }) => chain().setMark("textStyle", { fontSize }),
                unsetFontSize:
                    () =>
                        ({ chain }) => chain().setMark("textStyle", { fontSize: null }).removeEmptyTextStyle(),
            }
        },
    })

    const pmStateMod = await importModuleWithFallback(
        ["https://esm.sh/prosemirror-state", "https://unpkg.com/prosemirror-state?module", "https://cdn.jsdelivr.net/npm/prosemirror-state/+esm"],
        "prosemirror-state"
    )
    const Plugin = pmStateMod.Plugin

    const pmTablesMod = await importModuleWithFallback(
        ["https://esm.sh/prosemirror-tables", "https://unpkg.com/prosemirror-tables?module", "https://cdn.jsdelivr.net/npm/prosemirror-tables/+esm"],
        "prosemirror-tables"
    )
    const CellSelection = pmTablesMod.CellSelection
    const selectedRect = pmTablesMod.selectedRect

    if (!StarterKit || !Table || !TableRow || !TableCell || !TableHeader || !TextAlign || !TextStyle || !Plugin || !CellSelection || !selectedRect) {
        console.error("TipTap module sanity check failed:", { StarterKit, Table, TableRow, TableCell, TableHeader, TextAlign, TextStyle, Plugin })
        throw new Error("TipTap modules not loaded correctly.")
    }

    /* -------- created 플래그로 '새 표'만 구조 변경 허용 -------- */

    const CustomTable = Table.extend({
        addAttributes() {
            return { ...this.parent?.(), created: { default: false } }
        },
    })

    // ✅ 전역 canEditContent에서 참조할 수 있도록 외부 변수에 할당
    isInNewTable = function (state) {
        // "셀 안에서 만든 표" 판정: created=true AND nested table
        const sel = state.selection
        const base = sel.$anchorCell || sel.$headCell || sel.$from || sel.$anchor || sel.$head
        if (!base) return false

        let closestTable = null
        let sawAnotherTableAbove = false

        for (let d = base.depth; d >= 0; d--) {
            const node = base.node(d)
            if (node.type?.name === "table") {
                if (!closestTable) closestTable = node
                else { sawAnotherTableAbove = true; break }
            }
        }
        return !!(closestTable && closestTable.attrs?.created === true && sawAnotherTableAbove)
    }

    function markNearestTableCreated(editor) {
        // ✅ 삽입 직후 커서가 "내부 표" 안에 있을 때만 created=true 마킹
        const { state, view } = editor
        const { $from } = state.selection

        let closestTableDepth = null
        let sawAnotherTableAbove = false

        for (let d = $from.depth; d >= 0; d--) {
            const node = $from.node(d)
            if (node.type.name === "table") {
                if (closestTableDepth == null) closestTableDepth = d
                else { sawAnotherTableAbove = true; break }
            }
        }
        if (closestTableDepth == null || !sawAnotherTableAbove) return false

        const tableNode = $from.node(closestTableDepth)
        const pos = $from.before(closestTableDepth)
        const tr = state.tr.setNodeMarkup(pos, undefined, { ...tableNode.attrs, created: true })
        tr.setMeta(POLICY_ALLOW_META, true)
        view.dispatch(tr)
        return true
    }

    /* -------- inputField (inline atom node) -------- */

    const InputField = Node.create({
        name: "inputField",
        group: "inline",
        inline: true,
        atom: true,
        selectable: true,

        addAttributes() {
            return {
                value: { default: "" },
                placeholder: { default: "입력" },
                locked: { default: false },
                editable: { default: true },
            }
        },

        parseHTML() {
            return [
                { tag: "input[data-input-field]" },
                { tag: "span[data-input-field]" },
            ]
        },

        renderHTML({ HTMLAttributes }) {
            return ["span", { "data-input-field": "1", ...HTMLAttributes }]
        },

        addNodeView() {
            return ({ node, editor, getPos }) => {
                const input = document.createElement("input")
                input.type = "text"
                input.setAttribute("data-input-field", "1")
                input.className = "tpl-input-field"
                input.placeholder = node.attrs.placeholder || ""
                input.value = node.attrs.value || ""

                const setDisabled = () => {
                    input.disabled = !!node.attrs.locked || node.attrs.editable === false
                    input.readOnly = !!node.attrs.locked || node.attrs.editable === false
                }
                setDisabled()

                const onInput = () => {
                    if (input.disabled || input.readOnly) return
                    const pos = typeof getPos === "function" ? getPos() : null
                    if (pos == null) return
                    const tr = editor.state.tr.setNodeMarkup(pos, undefined, { ...node.attrs, value: input.value })
                    tr.setMeta(POLICY_ALLOW_META, true)
                    editor.view.dispatch(tr)
                }

                input.addEventListener("input", onInput)

                return {
                    dom: input,
                    stopEvent() { return true },
                    update(updatedNode) {
                        if (updatedNode.type.name !== "inputField") return false
                        input.placeholder = updatedNode.attrs.placeholder || ""
                        const v = updatedNode.attrs.value || ""
                        if (input.value !== v) input.value = v
                        input.disabled = !!updatedNode.attrs.locked || updatedNode.attrs.editable === false
                        input.readOnly = !!updatedNode.attrs.locked || updatedNode.attrs.editable === false
                        return true
                    },
                    destroy() {
                        input.removeEventListener("input", onInput)
                    },
                }
            }
        },
    })

    /* -------- policy guard -------- */

    function clearSelectedCellsContent(view) {
        const { state } = view
        if (!(state.selection instanceof CellSelection)) return false

        const rect = selectedRect(state)
        const { tableStart, map } = rect
        const paragraph = state.schema.nodes.paragraph
        if (!paragraph) return false

        let tr = state.tr
        for (let row = rect.top; row < rect.bottom; row++) {
            for (let col = rect.left; col < rect.right; col++) {
                const rel = map.map[row * map.width + col]
                const cellPos = tableStart + rel
                const cell = state.doc.nodeAt(cellPos)
                if (!cell) continue

                const start = cellPos + 1
                const end = start + cell.content.size
                tr = tr.replaceWith(start, end, paragraph.createAndFill())
            }
        }
        tr.setMeta(POLICY_ALLOW_META, true)
        view.dispatch(tr.scrollIntoView())
        return true
    }

    const PolicyGuard = Extension.create({
        name: "policyGuard",
        addProseMirrorPlugins() {
            return [
                new Plugin({
                    filterTransaction(tr, state) {
                        if (!tr.docChanged) return true

                        // meta 우회
                        if (tr.getMeta && tr.getMeta(POLICY_ALLOW_META)) return true

                        // ✅ insertTable 1회 우회
                        if (__bypassPolicyOnce__) {
                            __bypassPolicyOnce__ = false
                            return true
                        }

                        const s = stateFromTr(tr, state)

                        // ✅ editable=true 템플릿 빈 셀은 항상 허용
                        const cell = findAncestorNode(s, (n) => n.type?.name === "tableCell")
                        if (cell && cell.attrs?.editable === true) return true

                        // ✅ 내부(created) 표는 셀 전체 허용
                        if (isInNewTable && isInNewTable(s)) return true

                        // 그 외는 정책으로 차단
                        return canEditContent(s)
                    },
                    props: {
                        handlePaste(view) {
                            return !canEditContent(view.state)
                        },
                        handleDrop(view) {
                            return !canEditContent(view.state)
                        },
                        handleKeyDown(view, e) {
                            if (["Backspace", "Delete"].includes(e.key) && (view.state.selection instanceof CellSelection)) {
                                e.preventDefault()
                                return clearSelectedCellsContent(view)
                            }

                            if (canEditContent(view.state)) return false
                            if (e.key.length === 1 || ["Enter", "Backspace", "Delete"].includes(e.key)) {
                                e.preventDefault()
                                return true
                            }
                            return false
                        },
                    },
                }),
            ]
        },
    })

    /* -------- Editor 생성 -------- */

    const editor = new Editor({
        element: qs("#editor"),
        extensions: [
            StarterKit,
            TextStyle,
            FontSize,
            TextAlign.configure({ types: ["heading", "paragraph", "tableCell", "tableHeader"] }),

            CustomTable.configure({ resizable: true }),
            TableRow,
            CustomTableCell,
            CustomTableHeader,

            InputField,
            PolicyGuard,
        ],
        content: (templateJson && typeof templateJson === "object") ? precomputeCellEditableJson(templateJson) : (templateJson ?? "<p></p>"),
    })

    // ✅ toolbar focus 이동 문제: 마지막 유효 selection 기준으로 판정
    let __lastCanEdit__ = false
    editor.on("selectionUpdate", () => { __lastCanEdit__ = canEditContent(editor.state) })
    editor.on("focus", () => { __lastCanEdit__ = canEditContent(editor.state) })
    __lastCanEdit__ = canEditContent(editor.state)

    window.__DOC_WRITE_EDITOR__ = editor

    /* -------- Toolbar -------- */

    qs("#toolbar")?.addEventListener("click", (e) => {
        const btn = e.target.closest("button[data-act]")
        if (!btn) return

        if (!__lastCanEdit__) return

        const act = btn.dataset.act
        const ch = editor.chain().focus()

        switch (act) {
            case "bold": ch.toggleBold().run(); break
            case "italic": ch.toggleItalic().run(); break
            case "strike": ch.toggleStrike().run(); break
            case "h1": ch.toggleHeading({ level: 1 }).run(); break
            case "h2": ch.toggleHeading({ level: 2 }).run(); break
            case "h3": ch.toggleHeading({ level: 3 }).run(); break
            case "bullet": ch.toggleBulletList().run(); break
            case "ordered": ch.toggleOrderedList().run(); break
            case "alignLeft": ch.setTextAlign("left").run(); break
            case "alignCenter": ch.setTextAlign("center").run(); break
            case "alignRight": ch.setTextAlign("right").run(); break
            case "alignJustify": ch.setTextAlign("justify").run(); break

            // 표 생성: 빈 셀에서만 가능 (정책 유지)
            case "table":
                // ✅ insertTable 직후 selection 이동으로 filterTransaction이 막는 문제 방지
                __bypassPolicyOnce__ = true
                ch.insertTable({ rows: 3, cols: 3, withHeaderRow: false }).run()
                // created 마킹 즉시 + 지연 재시도
                markNearestTableCreated(editor)
                setTimeout(() => markNearestTableCreated(editor), 0)
                break

            // 구조 변경: 내부 created 표에서만
            case "addRowAfter":
                if (isInNewTable(editor.state)) ch.addRowAfter().run()
                break
            case "deleteRow":
                if (isInNewTable(editor.state)) ch.deleteRow().run()
                break
            case "addColumnAfter":
                if (isInNewTable(editor.state)) ch.addColumnAfter().run()
                break
            case "deleteColumn":
                if (isInNewTable(editor.state)) ch.deleteColumn().run()
                break
            case "tableDelete":
                if (isInNewTable(editor.state)) ch.deleteTable().run()
                break

            case "undo":
                ch.undo?.().run?.()
                break
            case "redo":
                ch.redo?.().run?.()
                break
        }
    })

    /* -------- Font size dropdown (사용자 구현: <select>) -------- */
    const fontSizeSelect = qs("#fontSizeSelect")
    if (fontSizeSelect) {
        fontSizeSelect.addEventListener("change", () => {
            // selection이 toolbar로 이동해도 lastCanEdit로 판단
            if (!__lastCanEdit__) return
            const v = fontSizeSelect.value
            if (!v) return
            // value가 "12" 같은 숫자라면 px 붙임, "12px"면 그대로
            const size = /px$/i.test(v) ? v : `${v}px`
            editor.chain().focus().setFontSize(size).run()
        })
    }
}

boot().catch((e) => console.error("boot failed:", e))