// /js/document-form/update-form.js
// 문서양식 수정 페이지 (ViewDocumentFormController 기준)
//  - 목록: /form/forms
//  - 상세: /form/{docfoNo}
//  - 수정: /form/{docfoNo}/edit
//
// ✅ make-form.js 저장 포맷과 동일하게 맞춤
//  - 저장 payload: { docfoName, cnttJson, cnttHtml, categories }
//  - cnttJson: TipTap JSON (템플릿 본문) + table 기본 fontSize 주입 + editable 정책 마킹
//  - cnttHtml: categories + bodyHtml 를 합쳐 전체 HTML 문서로 구성 (헤더 프리셋 제거)
//  - 저장 성공 시 목록 갱신 신호(localStorage list:dirty) 기록 후 pending/temp로 이동
//
// ✅ TipTap JSON에 inputField 노드가 포함되어도 에러 없이 로드되도록 InputField 노드 추가

import { Editor, Extension, Node, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'

import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'

import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { CellSelection } from '@tiptap/pm/tables'

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

if (
    !elTitle ||
    !elTypeRadios ||
    !btnAddRadio ||
    !btnClose ||
    !btnSave ||
    !btnTempSave ||
    !toolbar ||
    !fontSizeSelect ||
    !editorMount
) {
    ;(async () => {
        await swalError(
            '화면 구성 오류',
            'update-form.html의 요소 ID가 JS와 맞지 않습니다. (docTitle/typeRadios/addRadioBtn/closeBtn/saveBtn/tempSaveBtn/toolbar/fontSizeSelect/editor)'
        )
    })()
    throw new Error('DOM mapping mismatch')
}

// ===== constants =====
const DEFAULT_TABLE_FONT_SIZE = '16px'

// ===== list refresh signal =====
function markFormListDirty() {
    const ts = String(Date.now())

    try {
        localStorage.setItem('list:dirty', 'true')
        localStorage.setItem('list:dirty:ts', ts)
    } catch (_) {}

    try {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage({ type: 'DOCUMENT_FORM_DIRTY', at: Date.now() }, window.location.origin)
        }
    } catch (_) {}
}

// ===== server-injected me =====
function getMeEmpId() {
    return String(window.__ME_EMP_ID || '').trim()
}

function getMeRoles() {
    return Array.isArray(window.__ME_ROLES) ? window.__ME_ROLES : []
}

function getMeComId() {
    const v = window.__ME_COM_ID
    if (v === null || v === undefined) return null
    const s = String(v).trim()
    return s ? s : null
}

// ===== auth =====
async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {})
    if (!headers.has('Accept')) headers.set('Accept', 'application/json')

    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
    if (options.body && !isFormData && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json')
    }

    // Authorization 헤더는 넣지 않음
    return fetch(url, { ...options, headers, credentials: 'same-origin' })
}

async function extractErrorMessage(res) {
    const text = await res.text().catch(() => '')

    try {
        const json = text ? JSON.parse(text) : null
        if (json && typeof json === 'object') {
            if (typeof json.message === 'string' && json.message.trim()) return json.message
            if (json.error && typeof json.error.message === 'string') return json.error.message
            if (json.data && typeof json.data.message === 'string') return json.data.message
        }
    } catch (_) {}

    const trimmed = (text || '').trim()
    if (trimmed) return trimmed.length > 200 ? trimmed.slice(0, 200) + '…' : trimmed

    if (res.status === 401) return '로그인이 필요합니다.'
    if (res.status === 403) return '권한이 없습니다.'
    return `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`
}

async function apiFetchOrThrow(url, options = {}) {
    const res = await apiFetch(url, options)
    if (!res.ok) {
        const msg = await extractErrorMessage(res)
        throw new Error(msg)
    }
    return res
}

// ===== SweetAlert2 helpers =====
function hasSwal() {
    return typeof window.Swal !== 'undefined' && window.Swal && typeof window.Swal.fire === 'function'
}

function getSwal() {
    if (!hasSwal()) return null
    return window.Swal.mixin({
        confirmButtonText: '확인',
        cancelButtonText: '취소',
        buttonsStyling: true,
        heightAuto: false,
    })
}

async function swalError(title, text) {
    const swal = getSwal()
    if (!swal) {
        alert(`${title}\n${text || ''}`.trim())
        return { isConfirmed: true }
    }
    return swal.fire({ icon: 'error', title, text: text || undefined })
}

async function swalSuccess(title, text) {
    const swal = getSwal()
    if (!swal) {
        alert(`${title}\n${text || ''}`.trim())
        return { isConfirmed: true }
    }
    return swal.fire({ icon: 'success', title, text: text || undefined })
}

function swalLoading(title = '처리 중...') {
    const swal = getSwal()
    if (!swal) return
    swal.fire({
        title,
        allowOutsideClick: false,
        didOpen: () => window.Swal.showLoading(),
        heightAuto: false,
    })
}

function swalClose() {
    if (hasSwal()) window.Swal.close()
}

async function swalPromptNumber({ title, inputLabel, value = 3, min = 1, max = 20 }) {
    const swal = getSwal()
    if (!swal) {
        const raw = window.prompt(title, String(value))
        if (raw === null) return null
        let n = parseInt(raw, 10)
        if (!Number.isFinite(n)) n = value
        n = Math.max(min, Math.min(max, n))
        return n
    }

    const r = await swal.fire({
        title,
        input: 'number',
        inputLabel,
        inputValue: value,
        inputAttributes: { min: String(min), max: String(max), step: '1' },
        showCancelButton: true,
        preConfirm: (v) => {
            let n = parseInt(v, 10)
            if (!Number.isFinite(n)) n = value
            n = Math.max(min, Math.min(max, n))
            return n
        },
    })

    if (!r.isConfirmed) return null
    return r.value
}

// ===== success after flow =====
function safeNavigateOpener(url) {
    try {
        if (window.opener && !window.opener.closed) {
            window.opener.location.href = url
            window.opener.focus?.()
            return true
        }
    } catch (_) {}
    return false
}

async function successAndReturn({ title, text, openerUrl, fallbackUrl }) {
    await swalSuccess(title, text)

    const moved = openerUrl ? safeNavigateOpener(openerUrl) : false

    try {
        window.close()
    } catch (_) {}

    setTimeout(() => {
        if (!document.hidden) {
            if (!moved && fallbackUrl) location.href = fallbackUrl
        }
    }, 80)
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
    try {
        return JSON.parse(s)
    } catch {
        return null
    }
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
function getDocfoNoFromServerInjected() {
    const v = window.__DOCFO_NO__
    if (v === null || v === undefined) return ''
    const n = Number(v)
    return Number.isFinite(n) && n > 0 ? String(n) : ''
}

const docfoNo = getDocfoNoFromServerInjected()

if (!docfoNo) {
    ;(async () => {
        await swalError(
            '잘못된 접근',
            'docfoNo를 찾을 수 없습니다. ViewController에서 window.__DOCFO_NO__ 를 주입했는지 확인하세요.'
        )
    })()
    throw new Error('docfoNo missing (server injected)')
}

// ===== categories (radio + add + delete + rename) =====
const allowEmptyCategories = false

function normalizeCategoryNames(arr) {
    if (!Array.isArray(arr)) return []
    return arr
        .map((v) => (typeof v === 'string' ? v : v?.name))
        .filter(Boolean)
        .map((s) => String(s).trim())
        .filter(Boolean)
}

function getSelectedCategory() {
    const checked = elTypeRadios.querySelector('input[type="radio"][name="templateType"]:checked')
    return checked ? checked.value : null
}

function getAllCategoryNames() {
    const inputs = elTypeRadios.querySelectorAll('input.radio-value-input')
    const list = uniqStrings(Array.from(inputs).map((i) => String(i.value ?? '').trim()).filter(Boolean))
    if (!allowEmptyCategories && list.length === 0) return ['type1']
    return list
}

function renderCategoryRadios(names, selected) {
    const cats = uniqStrings(names)
    elTypeRadios.innerHTML = ''

    const finalCats = !allowEmptyCategories && cats.length === 0 ? ['type1'] : cats
    const sel = selected && finalCats.includes(selected) ? selected : finalCats[0] ?? null

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
        input.addEventListener('input', () => {
            radio.value = input.value
            radio.checked = true
        })

        input.addEventListener('blur', () => {
            const beforeSel = getSelectedCategory()
            const raw = [...elTypeRadios.querySelectorAll('.radio-item')]
                .map((it) => it.querySelector('input.radio-value-input')?.value ?? '')
                .map((v) => String(v).trim())
                .filter(Boolean)

            const list = uniqStrings(raw)
            const keepSel = beforeSel && list.includes(beforeSel) ? beforeSel : list[0] ?? null
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

            const next = current.filter((v) => v !== name)
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

// ===== response dto unwrap =====
function isResponseDto(obj) {
    return obj && typeof obj === 'object' && 'data' in obj && ('status' in obj || 'message' in obj)
}

async function unwrapJson(res) {
    const body = await res.json().catch(() => null)
    if (!body) return null
    return isResponseDto(body) ? body.data : body
}

// ===== full html build (헤더 프리셋 제거) =====
function buildFullHtml({ docfoName, categories, bodyHtml }) {
    const catHtml = (categories || []).map((c) => `<span class="df-cat">${escapeHtml(c)}</span>`).join('')
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
  .df-body{margin-top:10px;}
  table{border-collapse:collapse;}
</style>
</head>
<body>
  <div class="df-wrap">
    <h1 class="df-title">${escapeHtml(docfoName || '')}</h1>
    <div class="df-cats">${catHtml}</div>
    <div class="df-body" id="dfBody">${bodyHtml || ''}</div>
  </div>
</body>
</html>`
}

// ===== table default font size injection =====
function applyDefaultFontSizeInTables(json, fontSize = DEFAULT_TABLE_FONT_SIZE) {
    const cloned = deepClone(json)

    const ensureTextStyleFontSize = (marks = []) => {
        const out = Array.isArray(marks) ? [...marks] : []
        const idx = out.findIndex((m) => m?.type === 'textStyle')
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
        if (Array.isArray(node.content)) node.content.forEach((child) => walk(child, nowInTable))
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

// 템플릿 편집 저장 시 정책 마킹
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
                parseHTML: (el) => el.style.fontSize || null,
                renderHTML: (attrs) => (attrs.fontSize ? { style: `font-size: ${attrs.fontSize}` } : {}),
            },
        }
    },
})

const GoogleDocsCellDragSelection = Extension.create({
    name: 'googleDocsCellDragSelection',

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('googleDocsCellDragSelection'),

                view(view) {
                    let dragging = false
                    let anchorCellPos = null

                    const findCellPosFromEvent = (event) => {
                        const coords = { left: event.clientX, top: event.clientY }
                        const hit = view.posAtCoords(coords)
                        if (!hit || typeof hit.pos !== 'number') return null

                        const $pos = view.state.doc.resolve(hit.pos)

                        for (let d = $pos.depth; d > 0; d--) {
                            const n = $pos.node(d)
                            if (n.type?.name === 'tableCell' || n.type?.name === 'tableHeader') {
                                return $pos.before(d)
                            }
                        }
                        return null
                    }

                    const isResizeCursorActive = () => {
                        const c = document.body.style.cursor
                        return c === 'row-resize' || c === 'col-resize'
                    }

                    const onMouseDown = (e) => {
                        if (e.button !== 0) return
                        if (isResizeCursorActive()) return

                        // inputField 내부에서 시작하면 건드리지 않음
                        const t = e.target
                        if (t && t.closest?.('.input-field__input')) return

                        const pos = findCellPosFromEvent(e)
                        if (typeof pos !== 'number') return

                        dragging = true
                        anchorCellPos = pos
                    }

                    const onMouseMove = (e) => {
                        if (!dragging) return
                        if (isResizeCursorActive()) return

                        const headCellPos = findCellPosFromEvent(e)
                        if (typeof headCellPos !== 'number') return
                        if (typeof anchorCellPos !== 'number') return

                        if (headCellPos === anchorCellPos) return

                        const { state } = view
                        const nextSel = CellSelection.create(state.doc, anchorCellPos, headCellPos)

                        // selection이 실제로 바뀌는 경우에만 dispatch
                        const s = state.selection
                        const isSame =
                            s instanceof CellSelection &&
                            s.from === nextSel.from &&
                            s.to === nextSel.to

                        if (!isSame) {
                            view.dispatch(state.tr.setSelection(nextSel))
                        }

                        e.preventDefault()
                    }

                    const endDrag = () => {
                        dragging = false
                        anchorCellPos = null
                    }

                    view.dom.addEventListener('mousedown', onMouseDown, true)
                    window.addEventListener('mousemove', onMouseMove, true)
                    window.addEventListener('mouseup', endDrag, true)
                    window.addEventListener('blur', endDrag, true)

                    return {
                        destroy() {
                            view.dom.removeEventListener('mousedown', onMouseDown, true)
                            window.removeEventListener('mousemove', onMouseMove, true)
                            window.removeEventListener('mouseup', endDrag, true)
                            window.removeEventListener('blur', endDrag, true)
                        },
                    }
                },
            }),
        ]
    },
})

function bindCellSelectionHighlight(editor) {
    const pm = editor?.view?.dom
    if (!pm) return

    const clear = () => {
        pm.querySelectorAll('td.pm-cell-selected, th.pm-cell-selected')
            .forEach(el => el.classList.remove('pm-cell-selected'))
    }

    const sync = () => {
        clear()

        const sel = editor.view.state.selection
        if (!(sel instanceof CellSelection)) return

        // CellSelection 범위 안에 걸린 cell node들을 찾아서 DOM에 표시
        const { state } = editor.view
        const { from, to } = sel

        const cellPosList = []
        state.doc.nodesBetween(from, to, (node, pos) => {
            if (node.type?.name === 'tableCell' || node.type?.name === 'tableHeader') {
                cellPosList.push(pos)
            }
        })

        for (const pos of cellPosList) {
            try {
                const dom = editor.view.nodeDOM(pos)
                if (dom && (dom.tagName === 'TD' || dom.tagName === 'TH')) {
                    dom.classList.add('pm-cell-selected')
                }
            } catch (_) {}
        }
    }

    editor.on('selectionUpdate', sync)
    editor.on('transaction', sync)
    sync()
}

// ===== TipTap boot =====
function bootEditor(initialJson) {
    const editor = new Editor({
        element: editorMount,
        extensions: [
            StarterKit.configure({ history: true }),
            FontSizeCompat,
            RichTextStyle,
            Underline,
            TextAlign.configure({ types: ['heading', 'paragraph'] }),

            // Table (merge/split 포함)
            Table.configure({ resizable: true, lastColumnResizable: true }),
            TableRow,
            TableHeader,
            TableCell,
            // inputField 포함
            InputField,
            // 드래그 셀선택
            GoogleDocsCellDragSelection,
        ],
        content: initialJson || { type: 'doc', content: [{ type: 'paragraph' }] },
    })
    // 셀 선택 하이라이트
    bindCellSelectionHighlight(editor)
    return editor
}

// ===== toolbar =====
let _fontSizeBusy = false

function bindMergeSplitButtonsState(editor) {
    const btnMerge = toolbar.querySelector('button[data-act="mergeCells"]')
    const btnSplit = toolbar.querySelector('button[data-act="splitCell"]')
    if (!btnMerge && !btnSplit) return

    const sync = () => {
        try {
            if (btnMerge) btnMerge.disabled = !editor.can().mergeCells()
            if (btnSplit) btnSplit.disabled = !editor.can().splitCell()
        } catch (_) {
            if (btnMerge) btnMerge.disabled = true
            if (btnSplit) btnSplit.disabled = true
        }
    }

    editor.on('selectionUpdate', sync)
    editor.on('transaction', sync)
    sync()
}

function bindToolbar(editor) {
    toolbar.addEventListener('click', async (e) => {
        const btn = e.target.closest('button[data-act]')
        if (!btn) return
        const act = btn.dataset.act

        const c = editor.chain().focus()

        switch (act) {
            case 'h1':
                c.toggleHeading({ level: 1 }).run()
                break
            case 'h2':
                c.toggleHeading({ level: 2 }).run()
                break
            case 'h3':
                c.toggleHeading({ level: 3 }).run()
                break
            case 'p':
                c.setParagraph().run()
                break

            case 'bold':
                c.toggleBold().run()
                break
            case 'italic':
                c.toggleItalic().run()
                break
            case 'underline':
                c.toggleUnderline().run()
                break
            case 'strike':
                c.toggleStrike().run()
                break

            case 'alignLeft':
                c.setTextAlign('left').run()
                break
            case 'alignCenter':
                c.setTextAlign('center').run()
                break
            case 'alignRight':
                c.setTextAlign('right').run()
                break

            case 'bullet':
                c.toggleBulletList().run()
                break
            case 'ordered':
                c.toggleOrderedList().run()
                break

            case 'table': {
                const rows = await swalPromptNumber({
                    title: '행(rows) 개수를 입력하세요',
                    inputLabel: '1~20',
                    value: 3,
                    min: 1,
                    max: 20,
                })
                if (rows == null) break

                const cols = await swalPromptNumber({
                    title: '열(cols) 개수를 입력하세요',
                    inputLabel: '1~20',
                    value: 3,
                    min: 1,
                    max: 20,
                })
                if (cols == null) break

                editor.chain().focus().insertTable({ rows, cols, withHeaderRow: false }).createParagraphNear().run()
                break
            }

            case 'addRowAfter':
                c.addRowAfter().run()
                break
            case 'deleteRow':
                c.deleteRow().run()
                break
            case 'addColumnAfter':
                c.addColumnAfter().run()
                break
            case 'deleteColumn':
                c.deleteColumn().run()
                break
            case 'tableDelete':
                c.deleteTable().run()
                break

            // ✅ 셀 병합/분할
            case 'mergeCells':
                c.mergeCells().run()
                break
            case 'splitCell':
                c.splitCell().run()
                break

            case 'undo':
                c.undo().run()
                break
            case 'redo':
                c.redo().run()
                break
        }
    })

    // 버튼 활성/비활성 자동 동기화
    bindMergeSplitButtonsState(editor)

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
    setTimeout(() => {
        if (!document.hidden) history.back()
    }, 80)
}

// ===== API calls =====
async function fetchDetail() {
    const res = await apiFetchOrThrow(`${API_BASE}/${encodeURIComponent(docfoNo)}`)
    return await unwrapJson(res)
}

async function buildPayload({ editor, baseDetail }) {
    const docfoName = elTitle.value.trim()
    if (!docfoName) {
        await swalError('제목을 입력하세요', '양식 제목(docfo_name)은 필수입니다.')
        elTitle.focus()
        return null
    }

    const categories = getAllCategoryNames()

    const rawJson = editor.getJSON()
    const withTableFont = applyDefaultFontSizeInTables(rawJson, DEFAULT_TABLE_FONT_SIZE)
    const templateJson = markEditablePolicyForTemplate(withTableFont)

    const cnttJson = JSON.stringify(templateJson)
    const bodyHtml = editor.getHTML()

    // ✅ 헤더 프리셋 제거: categories + body만 합침
    const cnttHtml = buildFullHtml({ docfoName, categories, bodyHtml })

    const payload = { docfoName, cnttJson, cnttHtml, categories }

    const meComId = getMeComId()
    const meWriterId = getMeEmpId()

    if (meComId) payload.comId = meComId
    else if (baseDetail?.comId || baseDetail?.com_id) payload.comId = baseDetail.comId ?? baseDetail.com_id

    if (meWriterId) payload.writerId = meWriterId
    else if (baseDetail?.writerId || baseDetail?.writer_id) payload.writerId = baseDetail.writerId ?? baseDetail.writer_id

    return payload
}

async function saveUpdate({ editor, baseDetail }) {
    const payload = await buildPayload({ editor, baseDetail })
    if (!payload) return null

    const res = await apiFetchOrThrow(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })

    const out = await unwrapJson(res)
    return out ?? {}
}

async function saveTempUpdate({ editor, baseDetail }) {
    const payload = await buildPayload({ editor, baseDetail })
    if (!payload) return null

    await apiFetchOrThrow(`${API_BASE}/${encodeURIComponent(docfoNo)}/temp`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })

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
                swalLoading('저장 중...')

                await saveUpdate({ editor, baseDetail: detail })

                swalClose()
                markFormListDirty()

                await successAndReturn({
                    title: '저장되었습니다',
                    text: '문서양식이 정상적으로 저장되었습니다.',
                    openerUrl: '/form/pending',
                    fallbackUrl: '/form/pending',
                })
            } catch (e) {
                console.error(e)
                swalClose()
                await swalError('저장 실패', e?.message || String(e))
            } finally {
                btnSave.disabled = false
            }
        })

        btnTempSave.addEventListener('click', async () => {
            try {
                btnTempSave.disabled = true
                swalLoading('임시저장 중...')

                await saveTempUpdate({ editor, baseDetail: detail })

                swalClose()
                markFormListDirty()

                await successAndReturn({
                    title: '임시저장되었습니다',
                    text: '임시저장이 완료되었습니다.',
                    openerUrl: '/form/temp',
                    fallbackUrl: '/form/temp',
                })
            } catch (e) {
                console.error(e)
                swalClose()
                await swalError('임시저장 실패', e?.message || String(e))
            } finally {
                btnTempSave.disabled = false
            }
        })
    } catch (e) {
        console.error(e)
        await swalError('로드 실패', e?.message || String(e))
    }
})()