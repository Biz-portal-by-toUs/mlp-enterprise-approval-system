// static/src/make-form.js
// TipTap/ProseMirror는 esm.sh로만 로드 + ProseMirror는 external로 고정(단일 인스턴스)
// 저장: meta + uiState(상단표/라디오) + templateJson
// 불러오기: update-form.html(서버 주입 docfoNo)에서 자동 복원
// 테이블 "행 높이" 드래그 리사이즈 + attrs(height)로 저장
// 행/열 리사이즈 hover 커서 통일(row-resize / col-resize)
// 새 표 생성 시 기본 열 폭(colwidth) 설정
// 문서가 table로 끝나지 않도록 끝 문단 보장(표가 마지막처럼 인식되는 문제 방지)
// preset-no-col-resize 테이블: 열 리사이즈(드래그/커서) 완전 금지 + 프리셋 좌/우 폭 기본값 적용
// presetRightTemplate은 왼쪽 셀을 더 작게(LeftTemplate보다 작게) 고정 + 잠금 유지
// FontSize: 드롭다운으로 글자크기 적용 + JSON 저장/복원 + 커서 이동 시 드롭다운 실시간 동기화
//
// ✅ ViewController(pathVariable) 기준
// - 생성 화면: /form/new  (window.__DOCFO_NO__ = null)
// - 수정 화면: /form/{docfoNo}/edit (window.__DOCFO_NO__ = <number>)
// -> querystring(docfoNo/id) 기반 복원/저장 로직 제거, 서버 주입 값 사용

const TIPTAP_V = '2.11.2'

/** fetch 재귀 방지용: 원본 fetch 고정 */
const _fetch = window.fetch.bind(window)

/** 토큰 키 고정 (accessToken) */
function getAccessToken() {
    return (localStorage.getItem('accessToken') || '').trim()
}

/** Authorization Bearer 자동 처리 */
async function apiFetch(url, options = {}) {
    const token = getAccessToken()

    const headers = new Headers(options.headers || {})
    if (!headers.has('Accept')) headers.set('Accept', 'application/json')

    // FormData면 content-type 자동
    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
    if (options.body && !isFormData && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json')
    }

    if (token) {
        const hasBearer = /^Bearer\s+/i.test(token)
        headers.set('Authorization', hasBearer ? token : `Bearer ${token}`)
    }

    return _fetch(url, { ...options, headers, credentials: 'same-origin' })
}

// ✅ ViewController에서 주입된 docfoNo 사용 (수정 화면)
// - make-form(생성)에서는 null
function getDocfoNoFromServerInjected() {
    const v = window.__DOCFO_NO__
    if (v === null || v === undefined) return null
    const n = Number(v)
    return Number.isFinite(n) ? n : null
}

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

function isResponseDto(obj) {
    return obj && typeof obj === 'object' && ('data' in obj) && (('status' in obj) || ('message' in obj));
}

async function unwrapJson(res) {
    const body = await res.json().catch(() => null);
    if (!body) return null;
    return isResponseDto(body) ? body.data : body;
}

// 새 표 기본 열 폭(px)
const DEFAULT_COL_WIDTH = 160

// 왼쪽 프리셋(2열) 고정 폭(px)
const PRESET_LEFT_COL_W = 120
const PRESET_LEFT_RIGHT_COL_W = 270

// ---------- DOM ----------
const elEditor = document.getElementById('editor')
const elToolbar = document.getElementById('toolbar')
const elDocTitle = document.getElementById('docTitle')

const elTypeRadios = document.getElementById('typeRadios')
const elAddRadioBtn = document.getElementById('addRadioBtn')

const elSaveBtn = document.getElementById('saveBtn')
const elCloseBtn = document.getElementById('closeBtn')
const elTempSaveBtn = document.getElementById('tempSaveBtn')

const fontSizeSelect = document.getElementById('fontSizeSelect')

if (!elEditor) throw new Error('#editor not found')

// ---------- helpers ----------
function deepClone(obj) {
    if (typeof structuredClone === 'function') return structuredClone(obj)
    return JSON.parse(JSON.stringify(obj))
}

const DEFAULT_TABLE_FONT_SIZE = '16px'

function getEditorDefaultTextStyle() {
    // create-docform 화면에서는 CSS에 의존하지 않고, 기본 폰트/크기를 meta로 저장해 render에서 재현한다.
    const pm = document.querySelector('#editor .ProseMirror') || document.querySelector('.ProseMirror')
    if (!pm) return { fontFamily: null, fontSize: null }
    const cs = window.getComputedStyle(pm)
    return {
        fontFamily: cs?.fontFamily || null,
        fontSize: cs?.fontSize || null,
    }
}

function applyDefaultFontSizeInTables(json, fontSize = DEFAULT_TABLE_FONT_SIZE) {
    const cloned = deepClone(json)

    const ensureTextStyleFontSize = (marks = []) => {
        const out = Array.isArray(marks) ? [...marks] : []

        // textStyle mark 찾기
        const idx = out.findIndex((m) => m?.type === 'textStyle')
        if (idx >= 0) {
            const m = out[idx] || {}
            const attrs = { ...(m.attrs || {}) }
            // 이미 fontSize 있으면 건드리지 않음
            if (!attrs.fontSize) attrs.fontSize = fontSize
            out[idx] = { ...m, type: 'textStyle', attrs }
            return out
        }
        // 없으면 새로 추가
        out.push({ type: 'textStyle', attrs: { fontSize } })
        return out
    }

    const walk = (node, inTable = false) => {
        if (!node || typeof node !== 'object') return
        const t = node.type
        const nowInTable =
            inTable || t === 'table' || t === 'tableRow' || t === 'tableCell' || t === 'tableHeader'
        // table 내부 text에 기본 fontSize 주입
        if (nowInTable && t === 'text') {
            node.marks = ensureTextStyleFontSize(node.marks)
        }
        if (Array.isArray(node.content)) {
            node.content.forEach((child) => walk(child, nowInTable))
        }
    }
    walk(cloned, false)
    return cloned
}

function escapeHtml(s) {
    return String(s)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
}

// ---------- document_form 통합 저장용 helpers ----------
function getCategoriesFromRadios() {
    return [...elTypeRadios.querySelectorAll('.radio-item')]
        .map((item) => item.querySelector('.radio-value-input')?.value?.trim())
        .filter(Boolean)
}

function extractHeaderHtmlFromTemplates() {
    // 헤더표는 make-form.html의 <template>에 고정되어 있음(사용자는 편집하지 않음)
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

// TODO: 로그인 연동 시 교체
function getComId() { return window.__COM_ID__ || 'C01' }
function getWriterId() { return window.__WRITER_ID__ || 'E000001' }

// ASCII 0~32(공백/개행/탭/제어문자) + NBSP/ZWSP/BOM만 있으면 "의미없는 텍스트"로 간주
function isMeaninglessWhitespace(s) {
    const str = (s ?? '').toString()
    // ASCII 0~32 + NBSP(160) + ZWSP(8203) + BOM(65279)
    for (let i = 0; i < str.length; i++) {
        const c = str.charCodeAt(i)
        if (c <= 32) continue
        if (c === 160 || c === 8203 || c === 65279) continue
        return false
    }
    return true
}

// ---------- radio ui ----------
function getSelectedTypeValue() {
    const checked = elTypeRadios?.querySelector('input.radio-choice:checked')
    if (!checked) return ''
    const item = checked.closest('.radio-item')
    const input = item?.querySelector('input.radio-value-input')
    return (input?.value || '').trim()
}

function normalizeRadioGroup() {
    const radios = [...elTypeRadios.querySelectorAll('input.radio-choice')]
    if (radios.length === 0) return
    radios.forEach((r) => (r.name = 'templateType'))
}

function wireRadioItem(item) {
    const radio = item.querySelector('input.radio-choice')
    const delBtn = item.querySelector('button.radio-del')

    radio.addEventListener('change', normalizeRadioGroup)

    delBtn.addEventListener('click', () => {
        const all = [...elTypeRadios.querySelectorAll('.radio-item')]
        if (all.length <= 1) return
        item.remove()
        normalizeRadioGroup()
    })
}

function addRadio(value = 'type') {
    const item = document.createElement('div')
    item.className = 'radio-item'
    item.innerHTML = `
    <input type="radio" class="radio-choice" name="templateType" />
    <input type="text" class="radio-value-input" value="${escapeHtml(value)}" />
    <button type="button" class="radio-del" title="삭제">✕</button>
  `
    elTypeRadios.appendChild(item)
    wireRadioItem(item)
    normalizeRadioGroup()
}

function setRadioState({ templateTypes = [], selectedType = '' } = {}) {
    elTypeRadios.innerHTML = ''
    const list = templateTypes.length ? templateTypes : ['type1']
    list.forEach((v) => addRadio(v))

    if (selectedType && selectedType.trim().length > 0) {
        const items = [...elTypeRadios.querySelectorAll('.radio-item')]
        for (const item of items) {
            const input = item.querySelector('input.radio-value-input')
            const radio = item.querySelector('input.radio-choice')
            if ((input?.value || '').trim() === selectedType.trim()) {
                radio.checked = true
                break
            }
        }
    }
    normalizeRadioGroup()
}

;[...elTypeRadios.querySelectorAll('.radio-item')].forEach(wireRadioItem)
normalizeRadioGroup()

elAddRadioBtn?.addEventListener('click', () => addRadio(`type${elTypeRadios.children.length + 1}`))
elCloseBtn?.addEventListener('click', () => window.close())

// ---------- preset tables uiState ----------
function getPresetTablesState() {
    const leftTpl = document.getElementById('presetLeftTemplate')
    const rightTpl = document.getElementById('presetRightTemplate')
    return {
        leftHtml: (leftTpl?.innerHTML || '').trim(),
        rightHtml: (rightTpl?.innerHTML || '').trim(),
    }
}

function setPresetTablesState({ leftHtml = '', rightHtml = '' } = {}) {
    if (leftHtml) {
        const cur = document.getElementById('presetTableLeft')
        if (cur) cur.outerHTML = leftHtml
    }
    if (rightHtml) {
        const cur = document.getElementById('presetTableRight')
        if (cur) cur.outerHTML = rightHtml
    }
    applyPresetTableDefaultsInDocument()
}

// ---------- preset table helpers ----------
function isLockedPresetTableEl(tableEl) {
    return !!tableEl?.classList?.contains('preset-no-col-resize')
}

// 오른쪽 표: 열 수 상관없이 균등분배(colgroup %)
function applyEqualColsToDomTable(tableEl) {
    if (!tableEl) return
    const firstRow = tableEl.querySelector('tr')
    if (!firstRow) return

    const cells = [...firstRow.querySelectorAll('th,td')]
    const colCount = cells.length
    if (colCount <= 0) return

    tableEl.style.width = '100%'
    tableEl.style.tableLayout = 'fixed'

    let colgroup = tableEl.querySelector('colgroup')
    if (!colgroup) {
        colgroup = document.createElement('colgroup')
        tableEl.insertBefore(colgroup, tableEl.firstChild)
    }
    colgroup.innerHTML = ''

    const pct = (100 / colCount).toFixed(4)
    for (let i = 0; i < colCount; i++) {
        const col = document.createElement('col')
        col.style.width = `${pct}%`
        colgroup.appendChild(col)
    }

    // 균등 분배 방해할 수 있는 셀 width 제거
    for (const row of tableEl.querySelectorAll('tr')) {
        for (const c of row.querySelectorAll('th,td')) c.style.width = ''
    }
}

// 왼쪽 표: 2열 고정 폭
function applyTwoColWidthsToDomTable(tableEl, leftW, rightW) {
    if (!tableEl) return
    const firstRow = tableEl.querySelector('tr')
    if (!firstRow) return

    const cells = [...firstRow.querySelectorAll('th,td')]
    if (cells.length !== 2) return

    cells[0].style.width = `${leftW}px`
    cells[1].style.width = `${rightW}px`

    let colgroup = tableEl.querySelector('colgroup')
    if (!colgroup) {
        colgroup = document.createElement('colgroup')
        colgroup.appendChild(document.createElement('col'))
        colgroup.appendChild(document.createElement('col'))
        tableEl.insertBefore(colgroup, tableEl.firstChild)
    }
    const cols = [...colgroup.querySelectorAll('col')]
    if (cols.length >= 2) {
        cols[0].style.width = `${leftW}px`
        cols[1].style.width = `${rightW}px`
    }

    const rows = [...tableEl.querySelectorAll('tr')]
    for (const r of rows) {
        const tds = [...r.querySelectorAll('th,td')]
        if (tds.length >= 2) {
            tds[0].style.width = `${leftW}px`
            tds[1].style.width = `${rightW}px`
        }
    }
}

function applyPresetTableDefaultsInDocument() {
    // 1) 숨김 템플릿에 먼저 적용
    const presetLeftTpl = document.getElementById('presetLeftTemplate')
    if (presetLeftTpl) {
        const tmp = document.createElement('div')
        tmp.innerHTML = presetLeftTpl.innerHTML.trim()
        const table = tmp.querySelector('table')
        if (table) {
            applyTwoColWidthsToDomTable(table, PRESET_LEFT_COL_W, PRESET_LEFT_RIGHT_COL_W)
            presetLeftTpl.innerHTML = table.outerHTML
        }
    }

    const presetRightTpl = document.getElementById('presetRightTemplate')
    if (presetRightTpl) {
        const tmp = document.createElement('div')
        tmp.innerHTML = presetRightTpl.innerHTML.trim()
        const table = tmp.querySelector('table')
        if (table) {
            applyEqualColsToDomTable(table)
            presetRightTpl.innerHTML = table.outerHTML
        }
    }

    // 2) 실제 표시용 표에도 동일 적용
    const left = document.getElementById('presetTableLeft')
    if (left) applyTwoColWidthsToDomTable(left, PRESET_LEFT_COL_W, PRESET_LEFT_RIGHT_COL_W)

    const right = document.getElementById('presetTableRight')
    if (right) applyEqualColsToDomTable(right)
}

// ---------- boot ----------
async function bootEditor() {
    const [
        core,
        starterKit,
        table,
        tableRow,
        tableCell,
        tableHeader,
        textStyle,
        color,
        underline,
        textAlign,
        fontFamily,
        link,
        pmState,
    ] = await Promise.all([
        import('@tiptap/core'),
        import('@tiptap/starter-kit'),
        import('@tiptap/extension-table'),
        import('@tiptap/extension-table-row'),
        import('@tiptap/extension-table-cell'),
        import('@tiptap/extension-table-header'),
        import('@tiptap/extension-text-style'),
        import('@tiptap/extension-color'),
        import('@tiptap/extension-underline'),
        import('@tiptap/extension-text-align'),
        import('@tiptap/extension-font-family'),
        import('@tiptap/extension-link'),
        import('@tiptap/pm/state'),
    ])

    const { Editor, Extension, Node, mergeAttributes } = core
    const { StarterKit } = starterKit
    const { Table } = table
    const { TableRow } = tableRow
    const { TableCell } = tableCell
    const { TableHeader } = tableHeader
    const { TextStyle } = textStyle
    const { Color } = color
    const { Underline } = underline
    const { TextAlign } = textAlign
    const { FontFamily } = fontFamily
    const { Link } = link

    // NodeSelection / TextSelection import
    const { Plugin, PluginKey, NodeSelection, TextSelection } = pmState

    const isInputFieldNodeSelection = (state) => {
        const sel = state.selection
        return sel instanceof NodeSelection && sel.node?.type?.name === 'inputField'
    }

    const selectionTouchesInputField = (state) => {
        if (isInputFieldNodeSelection(state)) return true

        const { from, to } = state.selection
        let found = false

        state.doc.nodesBetween(from, to, (node) => {
            if (node.type?.name === 'inputField') {
                found = true
                return false
            }
            return !found
        })

        return found
    }

    const ResizableTableRow = TableRow.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                height: {
                    default: null,
                    parseHTML: (el) => {
                        // 1) data-row-h 우선
                        const data = el.getAttribute('data-row-h')
                        const n1 = data ? parseInt(data, 10) : NaN
                        if (Number.isFinite(n1)) return n1

                        // 2) style height fallback
                        const h = el.style?.height || ''
                        const n2 = h ? parseInt(h, 10) : NaN
                        return Number.isFinite(n2) ? n2 : null
                    },
                    renderHTML: (attrs) => {
                        if (!attrs.height) return {}
                        return {
                            'data-row-h': String(attrs.height),
                            style: `height:${attrs.height}px;`,
                        }
                    },
                },
            }
        },
    })

    const FontSizeCompat = Extension.create({
        name: 'fontSizeCompat',
        addGlobalAttributes() {
            return [
                {
                    types: ['textStyle'],
                    attributes: {
                        fontSize: {
                            default: null,
                            parseHTML: (element) => {
                                const size = element.style?.fontSize || ''
                                return size || null
                            },
                            renderHTML: (attributes) => {
                                if (!attributes.fontSize) return {}
                                return { style: `font-size: ${attributes.fontSize};` }
                            },
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

        // 기존 span[data-input-field] 호환
        parseHTML() {
            return [{ tag: 'span[data-input-field]' }]
        },

        // 저장/SSR용(실제 입력 UI는 NodeView가 담당)
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

        // 실제 편집 가능한 input 렌더링
        addNodeView() {
            return ({ node, editor, getPos }) => {
                const wrap = document.createElement('span')
                wrap.setAttribute('data-input-field', '1')
                wrap.className = `input-field ${node.attrs.locked ? 'locked' : 'editable'}`
                wrap.dataset.locked = node.attrs.locked ? '1' : '0'
                wrap.dataset.placeholder = node.attrs.placeholder || '입력'

                const input = document.createElement('input')
                input.type = 'text'
                input.className = 'input-field__input'
                input.value = (node.attrs.value ?? '').toString()
                input.placeholder = node.attrs.placeholder || '입력'
                input.disabled = !!node.attrs.locked

                input.addEventListener('input', () => {
                    if (node.attrs.locked) return
                    const pos = typeof getPos === 'function' ? getPos() : null
                    if (typeof pos !== 'number') return

                    editor.commands.command(({ tr }) => {
                        tr.setNodeMarkup(pos, undefined, {
                            ...node.attrs,
                            value: input.value,
                        })
                        return true
                    })
                })

                // locked면 삭제/입력만 막고, 방향키 이동 등은 허용
                input.addEventListener('keydown', (e) => {
                    if (!node.attrs.locked) return
                    const block = new Set(['Backspace', 'Delete', 'Enter', 'Tab'])
                    if (block.has(e.key) || e.key.length === 1) {
                        e.preventDefault()
                        e.stopPropagation()
                    }
                })

                wrap.appendChild(input)

                return {
                    dom: wrap,
                    contentDOM: null,

                    update(updatedNode) {
                        if (updatedNode.type.name !== 'inputField') return false
                        node = updatedNode

                        wrap.className = `input-field ${node.attrs.locked ? 'locked' : 'editable'}`
                        wrap.dataset.locked = node.attrs.locked ? '1' : '0'
                        wrap.dataset.placeholder = node.attrs.placeholder || '입력'

                        input.placeholder = node.attrs.placeholder || '입력'
                        input.disabled = !!node.attrs.locked

                        const nextVal = (node.attrs.value ?? '').toString()
                        if (input.value !== nextVal) input.value = nextVal

                        return true
                    },

                    // input 내부 이벤트는 ProseMirror가 가로채지 않게
                    stopEvent(event) {
                        return event.target === input
                    },

                    ignoreMutation() {
                        return true
                    },
                }
            }
        },
    })

    // tableCell/tableHeader에 editable 속성을 저장하기 위한 확장
    const EditableTableCell = TableCell.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                editable: {
                    default: null,
                    parseHTML: (el) => (el.getAttribute('data-editable') === '1' ? true : null),
                    renderHTML: (attrs) => (attrs.editable ? { 'data-editable': '1' } : {}),
                },
            }
        },
    })

    const EditableTableHeader = TableHeader.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                editable: {
                    default: null,
                    parseHTML: (el) => (el.getAttribute('data-editable') === '1' ? true : null),
                    renderHTML: (attrs) => (attrs.editable ? { 'data-editable': '1' } : {}),
                },
            }
        },
    })

    const SelectionExcludeInputField = Extension.create({
        name: 'selectionExcludeInputField',

        addProseMirrorPlugins() {
            return [
                new Plugin({
                    appendTransaction(transactions, oldState, newState) {
                        const sel = newState.selection

                        // 노드 단일 선택(inputField 클릭)은 그대로 둠
                        if (sel instanceof NodeSelection) return null
                        if (!sel || typeof sel.from !== 'number' || typeof sel.to !== 'number') return null
                        if (sel.empty) return null

                        const anchor = sel.anchor
                        const head = sel.head
                        const forward = head >= anchor

                        const from = sel.from
                        const to = sel.to

                        // 범위 안에서 첫 inputField 위치를 찾고, 방향에 따라 선택을 "줄임"
                        let clampPos = null

                        if (forward) {
                            // 앞으로 드래그(좌->우): inputField를 만나면 그 "직전"에서 끊기
                            newState.doc.nodesBetween(from, to, (node, pos) => {
                                if (node.type?.name === 'inputField') {
                                    clampPos = pos // inputField 시작 위치
                                    return false
                                }
                                return true
                            })

                            if (typeof clampPos === 'number' && clampPos > from) {
                                const tr = newState.tr.setSelection(TextSelection.create(newState.doc, from, clampPos))
                                return tr
                            }
                        } else {
                            // 뒤로 드래그(우->좌): inputField를 만나면 그 "직후"에서 끊기
                            newState.doc.nodesBetween(from, to, (node, pos) => {
                                if (node.type?.name === 'inputField') {
                                    // 뒤로 선택일 때는 가장 "마지막" inputField가 기준이 되어야 자연스러움
                                    clampPos = pos + node.nodeSize // inputField 끝 위치
                                }
                                return true
                            })

                            if (typeof clampPos === 'number' && clampPos < to) {
                                const tr = newState.tr.setSelection(TextSelection.create(newState.doc, clampPos, to))
                                return tr
                            }
                        }

                        return null
                    },
                }),
            ]
        },
    })

    const editor = new Editor({
        element: elEditor,
        extensions: [
            StarterKit.configure({
                history: true,
            }),
            FontSizeCompat,
            TextStyle,
            Color,
            Underline,
            TextAlign.configure({ types: ['heading', 'paragraph'] }),
            FontFamily,
            Link.configure({ openOnClick: false }),

            Table.configure({
                resizable: true,
                lastColumnResizable: true,
            }),
            ResizableTableRow,
            EditableTableHeader,
            EditableTableCell,

            InputField,
            SelectionExcludeInputField,
        ],
        content: '',

        // inputField는 기본적으로 수정 가능
        // - locked=true인 inputField만 삭제/입력/붙여넣기를 차단
        editorProps: {
            handleKeyDown(view, event) {
                const { state } = view
                const sel = state.selection

                // 노드 단일 선택 상태에서 locked inputField면 입력/삭제 차단
                if (sel instanceof NodeSelection && sel.node?.type?.name === 'inputField') {
                    if (sel.node.attrs?.locked) {
                        const block = new Set(['Backspace', 'Delete', 'Enter', 'Tab'])
                        if (block.has(event.key) || event.key.length === 1) {
                            event.preventDefault()
                            return true
                        }
                    }
                    return false
                }

                // 범위 선택에 locked inputField가 포함되면 delete 차단
                const { from, to } = sel
                let hasLocked = false
                state.doc.nodesBetween(from, to, (node) => {
                    if (node.type?.name === 'inputField' && node.attrs?.locked) {
                        hasLocked = true
                        return false
                    }
                    return !hasLocked
                })

                if (hasLocked) {
                    const block = new Set(['Backspace', 'Delete'])
                    if (block.has(event.key)) {
                        event.preventDefault()
                        return true
                    }
                }

                return false
            },

            handlePaste(view, event) {
                const { state } = view
                const { from, to } = state.selection
                let hasLocked = false

                state.doc.nodesBetween(from, to, (node) => {
                    if (node.type?.name === 'inputField' && node.attrs?.locked) {
                        hasLocked = true
                        return false
                    }
                    return !hasLocked
                })

                if (hasLocked) {
                    event.preventDefault?.()
                    return true
                }
                return false
            },

            handleDOMEvents: {
                paste(view, event) {
                    const { state } = view
                    const { from, to } = state.selection
                    let hasLocked = false

                    state.doc.nodesBetween(from, to, (node) => {
                        if (node.type?.name === 'inputField' && node.attrs?.locked) {
                            hasLocked = true
                            return false
                        }
                        return !hasLocked
                    })

                    if (hasLocked) {
                        event.preventDefault()
                        return true
                    }
                    return false
                },
            },
        },
    })

    wireToolbar(editor)
    wireFontSizeDropdown(editor)

    enableRowResize(editor)
    enableColResizeHoverCursor(editor)

    wireSave(editor)
    await restoreIfDocfoNoExists(editor)

    applyPresetTableDefaultsInDocument()
    ensureDocEndsWithParagraph(editor)
}

// ---------- toolbar ----------
function wireToolbar(editor) {
    if (!elToolbar) return

    elToolbar.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-act]')
        if (!btn) return

        const act = btn.dataset.act
        const ch = editor.chain().focus()

        switch (act) {
            case 'h1': ch.toggleHeading({ level: 1 }).run(); break
            case 'h2': ch.toggleHeading({ level: 2 }).run(); break
            case 'h3': ch.toggleHeading({ level: 3 }).run(); break
            case 'p':  ch.setParagraph().run(); break

            case 'bold':      ch.toggleBold().run(); break
            case 'italic':    ch.toggleItalic().run(); break
            case 'underline': ch.toggleUnderline().run(); break
            case 'strike':    ch.toggleStrike().run(); break

            case 'alignLeft':   ch.setTextAlign('left').run(); break
            case 'alignCenter': ch.setTextAlign('center').run(); break
            case 'alignRight':  ch.setTextAlign('right').run(); break

            case 'bullet': ch.toggleBulletList().run(); break
            case 'ordered': ch.toggleOrderedList().run(); break

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

                setTimeout(() => {
                    // 기본 열 폭을 에디터 너비에 맞춰 자동 계산(표가 화면 밖으로 튀지 않게)
                    const wrapEl = document.getElementById('editorWrap')
                    const wrapW = wrapEl ? wrapEl.clientWidth : 0
                    const pad = 24 // #editorWrap padding(12px*2)
                    const usable = Math.max(0, wrapW - pad)
                    const autoW = cols > 0 ? Math.floor(usable / cols) : DEFAULT_COL_WIDTH
                    const colW = Math.max(80, Math.min(240, autoW || DEFAULT_COL_WIDTH))
                    setDefaultTableColWidths(editor, colW)
                    ensureDocEndsWithParagraph(editor)
                    focusEndParagraphAndAlignLeft(editor)
                }, 0)

                break
            }

            case 'tableDelete': ch.deleteTable().run(); break
            case 'addRowAfter': ch.addRowAfter().run(); break
            case 'deleteRow': ch.deleteRow().run(); break
            case 'addColumnAfter': ch.addColumnAfter().run(); break
            case 'deleteColumn': ch.deleteColumn().run(); break

            case 'inputField':
                editor.chain().focus().insertContent({
                    type: 'inputField',
                    attrs: { value: '', locked: false, placeholder: '입력' },
                }).run()
                break

            case 'undo': ch.undo().run(); break
            case 'redo': ch.redo().run(); break
            default: break
        }
    })
}

// ---------- font size dropdown (live) ----------
let _fontSizeBusy = false

function wireFontSizeDropdown(editor) {
    if (!fontSizeSelect) return

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

function syncFontSizeSelectFromEditor(editor) {
    if (!fontSizeSelect || _fontSizeBusy) return
    const cur = editor.getAttributes('textStyle')?.fontSize || ''
    const num = String(cur).replace('px', '').trim()
    const has = [...fontSizeSelect.options].some((o) => o.value === num)
    fontSizeSelect.value = has ? num : ''
}

// 템플릿 JSON에 "작성 단계에서 입력 허용" 정책 표시
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

function wireSave(editor) {
    async function submit(mode) {
        const docTitle = (elDocTitle?.value || '').trim();

        const rawJson = editor.getJSON();
        const withTableFont = applyDefaultFontSizeInTables(rawJson, DEFAULT_TABLE_FONT_SIZE);
        const templateJson = markEditablePolicyForTemplate(withTableFont);

        const categories = getCategoriesFromRadios();
        const cnttJson = JSON.stringify(templateJson);
        const bodyHtml = editor.getHTML();

        const headerHtml = extractHeaderHtmlFromTemplates();
        const cnttHtml = buildFullHtml({ docfoName: docTitle, categories, headerHtml, bodyHtml });

        const payload = { docfoName: docTitle, cnttJson, cnttHtml, categories };

        const docfoNo = getDocfoNoFromServerInjected();
        const isEdit = Boolean(docfoNo);

        let url, method;

        if (mode === 'TEMP') {
            if (isEdit) { url = `/api/v1/forms/${encodeURIComponent(docfoNo)}/temp`; method = 'PUT'; }
            else { url = `/api/v1/forms/temp`; method = 'POST'; }
        } else { // mode === 'SAVE'
            if (isEdit) { url = `/api/v1/forms/${encodeURIComponent(docfoNo)}`; method = 'PUT'; }
            else { url = `/api/v1/forms`; method = 'POST'; }
        }

        const res = await apiFetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const t = await res.text().catch(() => '');
            alert((mode === 'TEMP' ? '임시저장 실패: ' : '저장 실패: ') + res.status + '\n' + t);
            return null;
        }

        // POST는 id가 올 수 있음
        if (method === 'POST') {
            const id = await unwrapJson(res);
            return id;
        }
        return true;
    }

    // 등록 버튼: 성공 시 pending 페이지로 이동
    elSaveBtn?.addEventListener('click', async () => {
        const ok = await submit('SAVE');
        if (!ok) return;

        markFormListDirty();

        const targetUrl = '/form/pending';

        try {
            if (window.opener && !window.opener.closed) {
                window.opener.location.href = targetUrl;
                window.opener.focus?.();
                window.close();
                return;
            }
        } catch (_) {}

        location.href = targetUrl;
    });

    // 임시저장 버튼: 성공 시 temp 페이지로 이동
    const elTempBtn = document.getElementById('tempSaveBtn');
    elTempBtn?.addEventListener('click', async () => {
        const ok = await submit('TEMP');
        if (!ok) return;

        markFormListDirty();

        const targetUrl = '/form/temp';

        try {
            if (window.opener && !window.opener.closed) {
                window.opener.location.href = targetUrl;
                window.opener.focus?.();
                window.close();
                return;
            }
        } catch (_) {}

        location.href = targetUrl;
    });
}

// ---------- table helpers ----------
function setDefaultTableColWidths(editor, defaultWidth) {
    const baseWidth = Number.isFinite(defaultWidth) && defaultWidth > 0 ? defaultWidth : DEFAULT_COL_WIDTH
    const view = editor.view
    const { from } = view.state.selection

    const domAt = view.domAtPos(from)
    const base = domAt.node.nodeType === 1 ? domAt.node : domAt.node.parentElement
    const tableEl = base?.closest?.('table')
    if (!tableEl) return

    if (tableEl.classList.contains('preset-no-col-resize')) return

    const colEls = [...tableEl.querySelectorAll('colgroup col')]
    if (colEls.length === 0) return

    tableEl.style.width = '100%'
    tableEl.style.tableLayout = 'fixed'

    const colCount = colEls.length

    let widthPerCol = defaultWidth
    if (!Number.isFinite(widthPerCol)) {
        const wrap = view.dom.closest('#editorWrap') || view.dom.parentElement
        const wrapW = wrap?.getBoundingClientRect?.().width || 0
        const usable = Math.max(0, Math.floor(wrapW - 24))
        const minCol = 60
        widthPerCol = Math.max(minCol, Math.floor(usable / colCount))
    }

    if (!Number.isFinite(widthPerCol) || widthPerCol <= 0) widthPerCol = DEFAULT_COL_WIDTH

    const widths = Array.from({ length: colCount }, () => widthPerCol)

    const { state } = view
    const $pos = state.doc.resolve(from)

    let tablePos = null
    let tableNode = null
    for (let d = $pos.depth; d > 0; d--) {
        const n = $pos.node(d)
        if (n.type.name === 'table') {
            tablePos = $pos.before(d)
            tableNode = n
            break
        }
    }
    if (!tableNode || typeof tablePos !== 'number') return

    const tr = state.tr

    let firstRow = null
    let rowOffset = 0
    tableNode.forEach((child, offset, i) => {
        if (i === 0) {
            firstRow = child
            rowOffset = offset
        }
    })
    if (!firstRow) return

    let colIndex = 0
    firstRow.forEach((cell, cellOffset) => {
        const cellPos = tablePos + 1 + rowOffset + 1 + cellOffset
        tr.setNodeMarkup(cellPos, undefined, {
            ...cell.attrs,
            colwidth: [widths[colIndex]],
        })
        colIndex++
    })

    if (tr.docChanged) view.dispatch(tr)
}

function ensureDocEndsWithParagraph(editor) {
    const { state, view } = editor
    const last = state.doc.lastChild
    if (!last) return

    if (last.type.name === 'table') {
        const p = state.schema.nodes.paragraph.create()
        const endPos = state.doc.content.size
        const tr = state.tr.insert(endPos, p)
        view.dispatch(tr)

        focusEndParagraphAndAlignLeft(editor)
    }
}

// ---------- row resize (hover cursor + drag) ----------
function enableRowResize(editor) {
    const view = editor.view
    const RESIZE_HIT = 6
    const MIN_H = 24
    const MAX_H = 800

    let resizing = false
    let startY = 0
    let startH = 0
    let rowPos = null

    const getRowPosFromDOM = (trEl) => {
        const posInside = view.posAtDOM(trEl, 0)
        const $pos = view.state.doc.resolve(posInside)
        for (let d = $pos.depth; d > 0; d--) {
            const n = $pos.node(d)
            if (n.type.name === 'tableRow') return $pos.before(d)
        }
        return null
    }

    const isNearRowBottom = (trEl, clientY) => {
        const rect = trEl.getBoundingClientRect()
        return rect.bottom - clientY <= RESIZE_HIT
    }

    const onHoverMove = (e) => {
        if (resizing) return

        const trEl = e.target.closest?.('tr')
        if (!trEl) {
            if (document.body.style.cursor === 'row-resize') document.body.style.cursor = ''
            return
        }

        const tableEl = trEl.closest('table')
        if (!tableEl || !view.dom.contains(tableEl)) return

        if (isNearRowBottom(trEl, e.clientY)) {
            document.body.style.cursor = 'row-resize'
        } else {
            if (document.body.style.cursor === 'row-resize') document.body.style.cursor = ''
        }
    }

    const onMouseDown = (e) => {
        if (e.button !== 0) return
        const trEl = e.target.closest?.('tr')
        if (!trEl) return
        if (!isNearRowBottom(trEl, e.clientY)) return

        const p = getRowPosFromDOM(trEl)
        if (typeof p !== 'number') return

        resizing = true
        startY = e.clientY
        startH = trEl.getBoundingClientRect().height
        rowPos = p

        document.body.style.cursor = 'row-resize'
        e.preventDefault()
    }

    const onMouseMove = (e) => {
        if (!resizing) return
        const diff = e.clientY - startY
        let newH = Math.round(startH + diff)
        newH = Math.max(MIN_H, Math.min(MAX_H, newH))

        editor.commands.command(({ state, tr }) => {
            if (typeof rowPos !== 'number') return false
            const node = state.doc.nodeAt(rowPos)
            if (!node || node.type.name !== 'tableRow') return false
            tr.setNodeMarkup(rowPos, undefined, { ...node.attrs, height: newH })
            return true
        })
    }

    const onMouseUp = () => {
        if (!resizing) return
        resizing = false
        startY = 0
        startH = 0
        rowPos = null
        document.body.style.cursor = ''
    }

    view.dom.addEventListener('mousemove', onHoverMove)
    view.dom.addEventListener('mousedown', onMouseDown)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseup', onMouseUp)
}

// ---------- col resize hover cursor ----------
function enableColResizeHoverCursor(editor) {
    const view = editor.view
    const HIT = 6

    const isNearRowBottom = (trEl, clientY) => {
        const r = trEl.getBoundingClientRect()
        return r.bottom - clientY <= HIT
    }

    const isNearCellRight = (cellEl, clientX) => {
        const r = cellEl.getBoundingClientRect()
        return r.right - clientX <= HIT
    }

    const onMove = (e) => {
        if (document.body.style.cursor === 'row-resize') return

        const cell = e.target.closest?.('td,th')
        if (!cell) {
            if (document.body.style.cursor === 'col-resize') document.body.style.cursor = ''
            return
        }

        const table = cell.closest('table')
        if (!table || !view.dom.contains(table)) return

        if (isLockedPresetTableEl(table)) {
            if (document.body.style.cursor === 'col-resize') document.body.style.cursor = ''
            return
        }

        const tr = cell.closest('tr')
        if (tr && isNearRowBottom(tr, e.clientY)) {
            if (document.body.style.cursor === 'col-resize') document.body.style.cursor = ''
            return
        }

        if (isNearCellRight(cell, e.clientX)) {
            document.body.style.cursor = 'col-resize'
        } else {
            if (document.body.style.cursor === 'col-resize') document.body.style.cursor = ''
        }
    }

    view.dom.addEventListener('mousemove', onMove)
}

function focusEndParagraphAndAlignLeft(editor) {
    editor.commands.focus('end')
    editor.commands.insertContent('\u200B')
    editor.chain().focus().setTextAlign('left').run()
}

// ---------- restore (ViewController path 기반) ----------
async function restoreIfDocfoNoExists(editor) {
    const docfoNo = getDocfoNoFromServerInjected()
    if (!docfoNo) return

    const res = await apiFetch(`/api/v1/forms/${encodeURIComponent(docfoNo)}`, {
        headers: { Accept: 'application/json' },
    })
    if (!res.ok) {
        alert('불러오기 실패: ' + res.status)
        return
    }

    const dto = await unwrapJson(res)

    // 백엔드 Detail DTO 기준: docfoName, cnttJson, categories
    const docfoName = dto?.docfoName || dto?.docfo_name || ''
    const cnttJsonStr = dto?.cnttJson || dto?.cntt_json || ''
    const categories = dto?.categories || []

    if (docfoName) elDocTitle.value = docfoName
    if (Array.isArray(categories) && categories.length) {
        setRadioState({ templateTypes: categories, selectedType: categories[0] })
    }

    if (cnttJsonStr) {
        try {
            const templateJson = JSON.parse(cnttJsonStr)
            editor.commands.setContent(templateJson)
        } catch (e) {
            console.warn('cnttJson parse failed', e)
        }
    }
}

bootEditor().catch((e) => {
    console.error(e)
    alert('에디터 로딩 실패: ' + (e?.message || e))
})