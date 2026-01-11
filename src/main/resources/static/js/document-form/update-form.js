// /js/document-form/update-form.js
// 문서양식 수정 페이지 (ViewDocumentFormController 기준)
//  - 목록: /form/forms
//  - 상세: /form/{docfoNo}
//  - 수정: /form/{docfoNo}/edit
//
// ✅ make-form.js 저장 포맷과 동일하게 맞춤
//  - 저장 payload: { docfoName, cnttJson, cnttHtml, categories }
//  - cnttJson: TipTap JSON (템플릿 본문) + table 기본 fontSize 주입 + editable 정책 마킹
//  - cnttHtml: header preset + categories + bodyHtml 를 합쳐 전체 HTML 문서로 구성
//  - 저장 성공 시 목록 갱신 신호(localStorage list:dirty) 기록 후 상세로 이동
//
// ✅ TipTap JSON에 inputField 노드가 포함되어도 에러 없이 로드되도록 InputField 노드 추가
//  - make-form과 같은 span[data-input-field] 렌더 형태로 통일

import { Editor, Extension, Node, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'

import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'

import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'

const API_BASE = '/api/v1/forms'
const VIEW_BASE = '/form'

// ===== DOM (update-form.html ID) =====
const elTitle = document.getElementById('docTitle')
const elTypeRadios = document.getElementById('typeRadios')
const btnAddRadio = document.getElementById('addRadioBtn')

const btnClose = document.getElementById('closeBtn')
const btnSave = document.getElementById('saveBtn')
const btnTempSave = document.getElementById('tempSaveBtn')

const toolbar = document.getElementById('toolbar')
const fontSizeSelect = document.getElementById('fontSizeSelect')
const editorMount = document.getElementById('editor')

// preset templates (make-form과 동일 구조)
const presetLeftTemplate = document.getElementById('presetLeftTemplate')
const presetRightTemplate = document.getElementById('presetRightTemplate')

if (!elTitle || !elTypeRadios || !btnAddRadio || !btnClose || !btnSave || !btnTempSave || !toolbar || !fontSizeSelect || !editorMount) {
    alert('update-form.html의 요소 ID가 JS와 맞지 않습니다. (docTitle/typeRadios/addRadioBtn/closeBtn/saveBtn/tempSaveBtn/toolbar/fontSizeSelect/editor)')
    throw new Error('DOM mapping mismatch')
}

// ===== constants =====
const DEFAULT_TABLE_FONT_SIZE = '16px'

// ===== list refresh signal =====
function markFormListDirty() {
    const ts = String(Date.now());

    // temp-list.js가 보는 키
    try {
        localStorage.setItem('list:dirty', 'true');
        localStorage.setItem('list:dirty:ts', ts); // 같은 탭 storage 이벤트 보완
    } catch (_) {}

    // opener가 있으면 즉시 갱신 신호
    try {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage(
                { type: 'DOCUMENT_FORM_DIRTY', at: Date.now() },
                window.location.origin
            );
        }
    } catch (_) {}
}

// ===== auth =====
function getAccessToken() {
    return (localStorage.getItem('accessToken') || '').trim()
}

async function apiFetch(url, options = {}) {
    const token = getAccessToken()
    const headers = new Headers(options.headers || {})
    if (!headers.has('Accept')) headers.set('Accept', 'application/json')

    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
    if (options.body && !isFormData && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

    if (token) headers.set('Authorization', /^Bearer\s+/i.test(token) ? token : `Bearer ${token}`)

    return fetch(url, { ...options, headers, credentials: 'same-origin' })
}

// ===== util =====
function deepClone(obj) {
    if (typeof structuredClone === 'function') return structuredClone(obj)
    return JSON.parse(JSON.stringify(obj))
}

function uniqStrings(list) {
    const out = []
    const seen = new Set()
    for (const v of Array.isArray(list) ? list : []) {
        const s = String(v ?? '').trim()
        if (!s) continue
        if (seen.has(s)) continue
        seen.add(s)
        out.push(s)
    }
    return out
}

function safeParseJsonMaybe(v) {
    if (!v) return null
    if (typeof v === 'object') return v
    const s = String(v).trim()
    if (!s) return null
    if (s.startsWith('<') || s.toLowerCase().startsWith('<!doctype')) return null
    try { return JSON.parse(s) } catch { return null }
}

function escapeHtml(s) {
    return String(s)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
}

// path: /form/{docfoNo}/edit
function getDocfoNo() {
    // 1) path variable
    const parts = location.pathname.split('/').filter(Boolean)
    // ["form", "{docfoNo}", "edit"]
    if (parts.length >= 3 && parts[0] === 'form' && parts[2] === 'edit' && /^\d+$/.test(parts[1])) {
        return parts[1]
    }

    // 2) meta
    const meta = document.querySelector('meta[name="template-id"]')
    const metaId = meta?.content?.trim()
    if (metaId) return metaId

    // 3) query fallback
    const sp = new URLSearchParams(location.search)
    const q = sp.get('docfoNo') || sp.get('id')
    return q ? String(q).trim() : ''
}

const docfoNo = getDocfoNo()
if (!docfoNo) {
    alert('docfoNo를 찾을 수 없습니다. 경로가 /form/{docfoNo}/edit 인지 확인하세요.')
    throw new Error('docfoNo missing')
}

// ===== categories (radio + add + delete + rename) =====
const allowEmptyCategories = false // 서버가 허용하면 true

function normalizeCategoryNames(arr) {
    if (!Array.isArray(arr)) return []
    return arr
        .map(v => (typeof v === 'string' ? v : v?.name))
        .filter(Boolean)
        .map(s => String(s).trim())
        .filter(Boolean)
}

function getSelectedCategory() {
    const checked = elTypeRadios.querySelector('input[type="radio"][name="templateType"]:checked')
    return checked ? checked.value : null
}

function getAllCategoryNames() {
    const inputs = elTypeRadios.querySelectorAll('input.radio-value-input')
    const list = uniqStrings(Array.from(inputs).map(i => String(i.value ?? '').trim()).filter(Boolean))
    if (!allowEmptyCategories && list.length === 0) return ['type1']
    return list
}

function renderCategoryRadios(names, selected) {
    const cats = uniqStrings(names)
    elTypeRadios.innerHTML = ''

    const finalCats = (!allowEmptyCategories && cats.length === 0) ? ['type1'] : cats
    const sel = selected && finalCats.includes(selected) ? selected : (finalCats[0] ?? null)

    finalCats.forEach((name, idx) => {
        const item = document.createElement('div')
        item.className = 'radio-item'

        const radio = document.createElement('input')
        radio.type = 'radio'
        radio.className = 'radio-choice'
        radio.name = 'templateType'
        radio.value = name
        radio.checked = name === sel

        const input = document.createElement('input')
        input.type = 'text'
        input.className = 'radio-value-input'
        input.value = name
        input.placeholder = '카테고리 이름'
        input.addEventListener('input', () => { radio.value = input.value })

        input.addEventListener('blur', () => {
            const beforeSel = getSelectedCategory()
            const raw = [...elTypeRadios.querySelectorAll('.radio-item')]
                .map(it => it.querySelector('input.radio-value-input')?.value ?? '')
                .map(v => String(v).trim())
                .filter(Boolean)
            const list = uniqStrings(raw)
            const keepSel = beforeSel && list.includes(beforeSel) ? beforeSel : (list[0] ?? null)
            renderCategoryRadios(list, keepSel)
        })

        const del = document.createElement('button')
        del.type = 'button'
        del.className = 'radio-del'
        del.title = '삭제'
        del.textContent = '✕'
        del.addEventListener('click', (e) => {
            e.preventDefault()
            e.stopPropagation()

            const current = getAllCategoryNames()
            if (!allowEmptyCategories && current.length <= 1) return

            const next = current.filter(v => v !== name)
            const prevSelected = getSelectedCategory()

            let nextSelected = next[0] ?? null
            if (prevSelected && prevSelected !== name && next.includes(prevSelected)) {
                nextSelected = prevSelected
            } else if (prevSelected === name) {
                const fallbackIdx = Math.min(idx, next.length - 1)
                nextSelected = next[fallbackIdx] ?? next[0] ?? null
            }

            renderCategoryRadios(next, nextSelected)
        })

        item.appendChild(radio)
        item.appendChild(input)
        item.appendChild(del)
        elTypeRadios.appendChild(item)
    })
}

btnAddRadio.addEventListener('click', () => {
    const current = getAllCategoryNames()
    const nextName = `type${current.length + 1}`
    renderCategoryRadios([...current, nextName], nextName)
})

// ===== preset header html (make-form 동일) =====
function getPresetTablesState() {
    return {
        leftHtml: (presetLeftTemplate?.innerHTML || '').trim(),
        rightHtml: (presetRightTemplate?.innerHTML || '').trim(),
    }
}

function isResponseDto(obj) {
    return obj && typeof obj === 'object' && ('data' in obj) && (('status' in obj) || ('message' in obj));
}

async function unwrapJson(res) {
    const body = await res.json().catch(() => null);
    if (!body) return null;
    return isResponseDto(body) ? body.data : body;
}

function extractHeaderHtmlFromTemplates() {
    const presetTables = getPresetTablesState()
    return `
    <div class="df-header">${presetTables.leftHtml || ''}</div>
    <div class="df-header">${presetTables.rightHtml || ''}</div>
  `.trim()
}

function buildFullHtml({ docfoName, categories, headerHtml, bodyHtml }) {
    const catHtml = (categories || []).map(c => `<span class="df-cat">${escapeHtml(c)}</span>`).join('')
    return `<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>${escapeHtml(docfoName || '')}</title>
<style>
  body{font-family:system-ui,-apple-system,Segoe UI,Roboto,"Noto Sans KR",sans-serif;margin:0;padding:24px;background:#fff;color:#111;}
  .df-wrap{max-width:980px;margin:0 auto;}
  .df-title{font-size:22px;font-weight:800;margin:0 0 14px;}
  .df-cats{display:flex;gap:8px;flex-wrap:wrap;margin:0 0 14px;}
  .df-cat{font-size:12px;padding:6px 10px;border-radius:999px;background:#f0f3ff;border:1px solid #d6ddff;}
  .df-header-wrap{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:0 0 16px;}
  .df-body{margin-top:10px;}
  table{border-collapse:collapse;}
</style>
</head>
<body>
  <div class="df-wrap">
    <h1 class="df-title">${escapeHtml(docfoName || '')}</h1>
    <div class="df-cats">${catHtml}</div>
    <div class="df-header-wrap" id="dfHeader">${headerHtml || ''}</div>
    <div class="df-body" id="dfBody">${bodyHtml || ''}</div>
  </div>
</body>
</html>`
}

// ===== table default font size injection (make-form 동일) =====
function applyDefaultFontSizeInTables(json, fontSize = DEFAULT_TABLE_FONT_SIZE) {
    const cloned = deepClone(json)

    const ensureTextStyleFontSize = (marks = []) => {
        const out = Array.isArray(marks) ? [...marks] : []
        const idx = out.findIndex(m => m?.type === 'textStyle')
        if (idx >= 0) {
            const m = out[idx] || {}
            const attrs = { ...(m.attrs || {}) }
            if (!attrs.fontSize) attrs.fontSize = fontSize
            out[idx] = { ...m, type: 'textStyle', attrs }
            return out
        }
        out.push({ type: 'textStyle', attrs: { fontSize } })
        return out
    }

    const walk = (node, inTable = false) => {
        if (!node || typeof node !== 'object') return
        const t = node.type
        const nowInTable = inTable || t === 'table' || t === 'tableRow' || t === 'tableCell' || t === 'tableHeader'
        if (nowInTable && t === 'text') node.marks = ensureTextStyleFontSize(node.marks)
        if (Array.isArray(node.content)) node.content.forEach(child => walk(child, nowInTable))
    }

    walk(cloned, false)
    return cloned
}

// ASCII 0~32 + NBSP/ZWSP/BOM only => meaningless
function isMeaninglessWhitespace(s) {
    const str = (s ?? '').toString()
    for (let i = 0; i < str.length; i++) {
        const c = str.charCodeAt(i)
        if (c <= 32) continue
        if (c === 160 || c === 8203 || c === 65279) continue
        return false
    }
    return true
}

// 템플릿 편집(create/update) 저장 시:
// - inputField => editable true, locked false
// - cell => "의미 있는 텍스트"가 있으면 editable false, 비어 있으면 editable true
function markEditablePolicyForTemplate(json) {
    const cloned = deepClone(json)

    const hasMeaningfulTextInCell = (cellNode) => {
        const content = Array.isArray(cellNode?.content) ? cellNode.content : []
        for (const child of content) {
            if (!child || typeof child !== 'object') continue
            if (child.type === 'paragraph') {
                const pc = Array.isArray(child.content) ? child.content : []
                for (const n of pc) {
                    if (!n || typeof n !== 'object') continue
                    if (n.type === 'text' && !isMeaninglessWhitespace(n.text)) return true
                    if (n.type && n.type !== 'text' && n.type !== 'inputField') return true
                }
            } else {
                return true
            }
        }
        return false
    }

    const walk = (node) => {
        if (!node || typeof node !== 'object') return

        if (node.type === 'inputField') {
            node.attrs = { ...(node.attrs || {}), editable: true, locked: false }
        }

        if (node.type === 'tableCell' || node.type === 'tableHeader') {
            const filled = hasMeaningfulTextInCell(node)
            node.attrs = { ...(node.attrs || {}), editable: !filled }
        }

        const content = node.content
        if (Array.isArray(content)) content.forEach(walk)
    }

    walk(cloned)
    return cloned
}

// ===== TipTap extensions =====
const FontSizeCompat = Extension.create({
    name: 'fontSizeCompat',
    addGlobalAttributes() {
        return [
            {
                types: ['textStyle'],
                attributes: {
                    fontSize: {
                        default: null,
                        parseHTML: (element) => element.style?.fontSize || null,
                        renderHTML: (attributes) => (attributes.fontSize ? { style: `font-size: ${attributes.fontSize};` } : {}),
                    },
                },
            },
        ]
    },
})

const InputField = Node.create({
    name: 'inputField',
    group: 'inline',
    inline: true,
    atom: true,
    selectable: true,

    addAttributes() {
        return {
            value: { default: '' },
            locked: { default: false },
            placeholder: { default: '입력' },
            editable: { default: true },
        }
    },

    parseHTML() {
        return [{ tag: 'span[data-input-field]' }]
    },

    renderHTML({ HTMLAttributes }) {
        const locked = !!HTMLAttributes.locked
        const value = HTMLAttributes.value || ''
        const placeholder = HTMLAttributes.placeholder || '입력'

        return [
            'span',
            mergeAttributes(HTMLAttributes, {
                'data-input-field': '1',
                'data-locked': locked ? '1' : '0',
                'data-placeholder': placeholder,
                class: `input-field ${locked ? 'locked' : 'editable'}`,
            }),
            value || placeholder,
        ]
    },
})

// RichTextStyle: fontSize 유지
const RichTextStyle = TextStyle.extend({
    addAttributes() {
        return {
            ...(this.parent?.() ?? {}),
            fontSize: {
                default: null,
                parseHTML: el => el.style.fontSize || null,
                renderHTML: attrs => (attrs.fontSize ? { style: `font-size: ${attrs.fontSize}` } : {}),
            },
        }
    },
})

// ===== TipTap boot =====
function bootEditor(initialJson) {
    return new Editor({
        element: editorMount,
        extensions: [
            StarterKit.configure({ history: true }),
            FontSizeCompat,
            RichTextStyle,
            Underline,
            TextAlign.configure({ types: ['heading', 'paragraph'] }),
            Table.configure({ resizable: true, lastColumnResizable: true }),
            TableRow,
            TableHeader,
            TableCell,
            InputField,
        ],
        content: initialJson || { type: 'doc', content: [{ type: 'paragraph' }] },
    })
}

// ===== toolbar =====
let _fontSizeBusy = false

function bindToolbar(editor) {
    toolbar.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-act]')
        if (!btn) return
        const act = btn.dataset.act

        const c = editor.chain().focus()

        switch (act) {
            case 'h1': c.toggleHeading({ level: 1 }).run(); break
            case 'h2': c.toggleHeading({ level: 2 }).run(); break
            case 'h3': c.toggleHeading({ level: 3 }).run(); break
            case 'p':  c.setParagraph().run(); break

            case 'bold':      c.toggleBold().run(); break
            case 'italic':    c.toggleItalic().run(); break
            case 'underline': c.toggleUnderline().run(); break
            case 'strike':    c.toggleStrike().run(); break

            case 'alignLeft':   c.setTextAlign('left').run(); break
            case 'alignCenter': c.setTextAlign('center').run(); break
            case 'alignRight':  c.setTextAlign('right').run(); break

            case 'bullet':  c.toggleBulletList().run(); break
            case 'ordered': c.toggleOrderedList().run(); break

            case 'table': {
                const rowsInput = window.prompt('행(rows) 개수를 입력하세요', '3')
                if (rowsInput === null) break
                const colsInput = window.prompt('열(cols) 개수를 입력하세요', '3')
                if (colsInput === null) break

                let rows = parseInt(rowsInput, 10)
                let cols = parseInt(colsInput, 10)

                if (!Number.isFinite(rows) || rows < 1) rows = 3
                if (!Number.isFinite(cols) || cols < 1) cols = 3
                rows = Math.min(rows, 20)
                cols = Math.min(cols, 20)

                editor.chain().focus().insertTable({ rows, cols, withHeaderRow: false }).createParagraphNear().run()
                break
            }

            case 'addRowAfter':    c.addRowAfter().run(); break
            case 'deleteRow':      c.deleteRow().run(); break
            case 'addColumnAfter': c.addColumnAfter().run(); break
            case 'deleteColumn':   c.deleteColumn().run(); break
            case 'tableDelete':    c.deleteTable().run(); break

            case 'undo': c.undo().run(); break
            case 'redo': c.redo().run(); break
        }
    })

    if (fontSizeSelect) {
        fontSizeSelect.addEventListener('pointerdown', () => (_fontSizeBusy = true))
        fontSizeSelect.addEventListener('blur', () => (_fontSizeBusy = false))

        fontSizeSelect.addEventListener('change', () => {
            const size = fontSizeSelect.value
            if (!size) {
                editor.chain().focus().setMark('textStyle', { fontSize: null }).run()
            } else {
                editor.chain().focus().setMark('textStyle', { fontSize: `${size}px` }).run()
            }
            syncFontSizeSelectFromEditor(editor)
        })

        editor.on('selectionUpdate', () => syncFontSizeSelectFromEditor(editor))
        editor.on('transaction', () => syncFontSizeSelectFromEditor(editor))
    }
}

function syncFontSizeSelectFromEditor(editor) {
    if (!fontSizeSelect || _fontSizeBusy) return
    const cur = editor.getAttributes('textStyle')?.fontSize || ''
    const num = String(cur).replace('px', '').trim()
    const has = [...fontSizeSelect.options].some((o) => o.value === num)
    fontSizeSelect.value = has ? num : ''
}

function closeOrBack() {
    window.close()
    setTimeout(() => { if (!document.hidden) history.back() }, 80)
}

// ===== API calls =====
async function fetchDetail() {
    const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`)
    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`상세 조회 실패 HTTP ${res.status} ${t}`)
    }
    return await unwrapJson(res)
}

function buildPayload({ editor, baseDetail }) {
    const docfoName = elTitle.value.trim()
    if (!docfoName) {
        alert('양식 제목을 입력하세요.')
        elTitle.focus()
        return null
    }

    const categories = getAllCategoryNames()

    const rawJson = editor.getJSON()
    const withTableFont = applyDefaultFontSizeInTables(rawJson, DEFAULT_TABLE_FONT_SIZE)
    const templateJson = markEditablePolicyForTemplate(withTableFont)

    const cnttJson = JSON.stringify(templateJson)
    const bodyHtml = editor.getHTML()

    const headerHtml = extractHeaderHtmlFromTemplates()
    const cnttHtml = buildFullHtml({ docfoName, categories, headerHtml, bodyHtml })

    const payload = { docfoName, cnttJson, cnttHtml, categories }

    if (baseDetail?.comId || baseDetail?.com_id) payload.comId = baseDetail.comId ?? baseDetail.com_id
    if (baseDetail?.writerId || baseDetail?.writer_id) payload.writerId = baseDetail.writerId ?? baseDetail.writer_id

    return payload
}

async function saveUpdate({ editor, baseDetail }) {
    const payload = buildPayload({ editor, baseDetail })
    if (!payload) return null

    const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })

    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`저장 실패 HTTP ${res.status} ${t}`)
    }

    const out = await unwrapJson(res);
    return out ?? {};
}

async function saveTempUpdate({ editor, baseDetail }) {
    const payload = buildPayload({ editor, baseDetail })
    if (!payload) return null

    const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/temp`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })

    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`임시저장 실패 HTTP ${res.status} ${t}`)
    }

    return true
}

// ===== run =====
;(async function main() {
    try {
        const detail = await fetchDetail()

        const title = detail.docfoName ?? detail.docfo_name ?? ''
        const json = safeParseJsonMaybe(detail.cnttJson ?? detail.cntt_json ?? null)
        const cats = normalizeCategoryNames(detail.categories ?? [])

        elTitle.value = title
        renderCategoryRadios(cats, cats[0] ?? null)

        const editor = bootEditor(json)
        bindToolbar(editor)

        btnClose.addEventListener('click', closeOrBack)

        btnSave.addEventListener('click', async () => {
            try {
                btnSave.disabled = true
                await saveUpdate({ editor, baseDetail: detail })
                markFormListDirty()
                // 상세로 이동: /form/{docfoNo}
                location.href = `${VIEW_BASE}/${encodeURIComponent(docfoNo)}`
            } catch (e) {
                console.error(e)
                alert('저장 실패: ' + (e?.message || e))
            } finally {
                btnSave.disabled = false
            }
        })

        btnTempSave.addEventListener('click', async () => {
            try {
                btnTempSave.disabled = true
                await saveTempUpdate({ editor, baseDetail: detail })
                markFormListDirty()
                location.href = `${VIEW_BASE}/${encodeURIComponent(docfoNo)}`
            } catch (e) {
                console.error(e)
                alert('임시저장 실패: ' + (e?.message || e))
            } finally {
                btnTempSave.disabled = false
            }
        })
    } catch (e) {
        console.error(e)
        alert('로드 실패: ' + (e?.message || e))
    }
})()