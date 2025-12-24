// static/js/document-form/detail.js
// ✅ Fixes
// 1) 상세 조회 API: /api/v1/forms/{docfoNo}
// 2) payload 전체를 JSON.parse 하지 않고, payload.cnttJson만 처리
// 3) cnttJson이 JsonNode(객체)로 오든, String(JSON)으로 오든 모두 처리
// 4) cnttJson이 실수로 HTML(<!doctype...)이면 JSON.parse 시도하지 않고 cnttHtml로 폴백
// 5) type 누락 노드 방어(sanitize)로 'Unknown node type: undefined' 가능성 줄임

const TIPTAP_V = '2.11.2'
const cdn = (pkg) => `https://esm.sh/${pkg}@${TIPTAP_V}?bundle&target=es2020`

const meta = document.querySelector('meta[name="template-id"]')
const docfoNo = meta?.content

const mount = document.getElementById('templateMount')
if (!mount) throw new Error('#templateMount not found')

if (!docfoNo) {
    mount.textContent = 'template-id가 없습니다.'
    throw new Error('template-id is missing')
}

/** ✅ fetch 재귀 방지용: 원본 fetch 고정 */
const _fetch = window.fetch.bind(window)

/** ✅ A안: 토큰 키 고정 (네 로그 기준 accessToken) */
function getAccessToken() {
    return (localStorage.getItem('accessToken') || '').trim()
}

/** ✅ Authorization Bearer 자동 처리 */
async function apiFetch(url, options = {}) {
    const token = getAccessToken()

    const headers = new Headers(options.headers || {})
    if (!headers.has('Accept')) headers.set('Accept', 'application/json')

    if (options.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json')
    }

    if (token) {
        const hasBearer = /^Bearer\s+/i.test(token)
        headers.set('Authorization', hasBearer ? token : `Bearer ${token}`)
    }

    return _fetch(url, { ...options, headers })
}

function escapeHtml(s) {
    return String(s ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
}

function normalizeTypes(types) {
    const out = []
    const seen = new Set()
    for (const t of Array.isArray(types) ? types : []) {
        const v = String(t ?? '').trim()
        if (!v) continue
        if (seen.has(v)) continue
        seen.add(v)
        out.push(v)
    }
    return out
}

/**
 * payload(백엔드 ResDocumentFormDetailDto) 기준
 * - docfoName
 * - cnttJson (JsonNode or String)
 * - cnttHtml (String)
 * - categories: [{name: "..."}] or ["..."] (혼용 대비)
 */
function renderHeaderFromFormDetail(payload) {
    const title = payload?.docfoName || payload?.title || payload?.name || ''

    const rawCats = payload?.categories || []
    const types = normalizeTypes(
        Array.isArray(rawCats)
            ? rawCats.map((c) => (typeof c === 'string' ? c : c?.name))
            : [],
    )

    const elTitle = document.getElementById('tplTitle')
    const elLeft = document.getElementById('tplLeftSide')
    const elRight = document.getElementById('tplRightSide')
    const elCats = document.getElementById('tplCats')

    if (elTitle) elTitle.textContent = title || '-'
    if (elLeft) elLeft.innerHTML = `<span class="tpl-muted">-</span>`
    if (elRight) elRight.innerHTML = `<span class="tpl-muted">-</span>`

    if (elCats) {
        elCats.innerHTML =
            types.length > 0
                ? types
                    .map((v) => {
                        const vv = String(v).trim()
                        return `
                <span class="tpl-catItem">
                  <input class="tpl-catRadio" type="radio" />
                  <span class="tpl-pill" data-type="${escapeHtml(vv)}">${escapeHtml(vv)}</span>
                </span>
              `
                    })
                    .join('')
                : `<span class="tpl-muted">카테고리 없음</span>`
    }
}

/**
 * cnttJson normalize
 * - JsonNode(객체)면 그대로 사용
 * - String이면 JSON.parse 시도
 * - String이 '<!doctype' / '<html' 등 HTML이면 JSON 아님 → null 반환
 * - { doc: {...} } 래핑 제거
 * - content 배열만 온 케이스 → doc로 감싸기
 */
function normalizeTiptapJson(raw) {
    if (raw == null) return null

    // String
    if (typeof raw === 'string') {
        const s = raw.trim()
        if (!s) return null

        // HTML이 들어온 경우(JSON.parse 금지)
        if (s.startsWith('<') || /^<!doctype/i.test(s)) {
            console.warn('[cnttJson] looks like HTML string, skip JSON.parse')
            return null
        }

        try {
            raw = JSON.parse(s)
        } catch (e) {
            console.warn('[cnttJson] JSON.parse failed', e)
            return null
        }
    }

    // { doc: {...} } 래핑 제거
    if (raw && typeof raw === 'object' && raw.doc && typeof raw.doc === 'object') {
        raw = raw.doc
    }

    // content 배열만 오면 doc로 감싸기
    if (Array.isArray(raw)) {
        raw = { type: 'doc', content: raw }
    }

    if (!raw || typeof raw !== 'object' || !raw.type) return null

    // 최종 sanitize
    const sanitized = sanitizeTiptapNode(raw, 'doc')
    if (!sanitized?.type) return null
    return sanitized
}

// ✅ type 없는 노드가 섞여 들어오는 케이스 방어
function sanitizeTiptapNode(node, path = 'root') {
    if (node == null) return null

    // 배열이면 각 요소 sanitize
    if (Array.isArray(node)) {
        const out = node
            .map((n, i) => sanitizeTiptapNode(n, `${path}[${i}]`))
            .filter(Boolean)
        return out
    }

    if (typeof node !== 'object') return null

    // type 누락 + text만 있으면 text 노드로 보정
    if (!node.type) {
        if (typeof node.text === 'string') {
            node = { type: 'text', text: node.text, marks: node.marks }
        } else {
            console.warn('[sanitize] drop node (missing type):', path, node)
            return null
        }
    }

    // marks 정리
    if (Array.isArray(node.marks)) {
        node.marks = node.marks.filter((m) => m && m.type)
    }

    // content 재귀 정리
    if (Array.isArray(node.content)) {
        node.content = node.content
            .map((c, i) => sanitizeTiptapNode(c, `${path}.content[${i}]`))
            .filter(Boolean)
    }

    return node
}

function extractFirstTableColWidthsFromJson(docJson) {
    const table = docJson?.content?.find((n) => n?.type === 'table')
    if (!table) return []
    const firstRow = table.content?.[0]
    if (!firstRow || !Array.isArray(firstRow.content)) return []
    return firstRow.content.map((cell) => {
        const cw = cell?.attrs?.colwidth
        const w = Array.isArray(cw) ? cw[0] : cw
        return Number.isFinite(w) ? w : null
    })
}

function applyColgroupToBodyTables(rootEl, widths) {
    if (!widths || widths.every((w) => w == null)) return
    const bodyTables = [...rootEl.querySelectorAll('.tpl-bodyBox table')]
    for (const table of bodyTables) {
        let colgroup = table.querySelector('colgroup')
        if (!colgroup) {
            colgroup = document.createElement('colgroup')
            table.insertBefore(colgroup, table.firstChild)
        }
        colgroup.innerHTML = ''
        widths.forEach((w) => {
            const col = document.createElement('col')
            if (w != null) col.style.width = `${w}px`
            colgroup.appendChild(col)
        })

        const sum = widths.reduce((a, b) => a + (b || 0), 0)
        table.style.tableLayout = 'fixed'
        table.style.width = 'max-content'
        table.style.minWidth = `${sum}px`
    }
}

function decorateOutsideInputFields(rootEl) {
    if (!rootEl) return
    const spans = [...rootEl.querySelectorAll('span[data-input-field="1"], span[data-input-field]')]
    for (const el of spans) {
        if (el.closest('table')) continue
        const txt = (el.textContent || '').trim()
        if (txt.length === 0) {
            el.classList.add('input-field-outside-empty')
            el.innerHTML = '&nbsp;'
        }
    }
}

function enforceFontSizeFromDataFs(rootEl) {
    if (!rootEl) return
    const nodes = rootEl.querySelectorAll('[data-fs]')
    nodes.forEach((el) => {
        const fs = (el.getAttribute('data-fs') || '').trim()
        if (!fs) return
        el.style.fontSize = fs
    })
}

function closeSafely() {
    window.close()
    setTimeout(() => {
        if (!document.hidden) history.back()
    }, 50)
}

function bindFooterActions(docfoNo) {
    document.getElementById('tplEditBtn')?.addEventListener('click', () => {
        const url = `/document-form/manager/form/update-form?docfoNo=${encodeURIComponent(docfoNo)}`
        location.href = url
    })

    document.getElementById('tplCloseBtn')?.addEventListener('click', closeSafely)
}

async function run() {
    bindFooterActions(docfoNo)

    const core = await import(cdn('@tiptap/core'))
    const { Node, Extension, mergeAttributes } = core

    const [
        { generateHTML },
        { default: Document },
        { default: Paragraph },
        { default: Text },
        { default: Bold },
        { default: Italic },
        { default: Strike },
        { default: Heading },
        { default: OrderedList },
        { default: BulletList },
        { default: ListItem },
        { default: Table },
        { default: TableRow },
        { default: TableCell },
        { default: TableHeader },
        { default: TextStyle },
        { default: Color },
        { default: Underline },
        { default: TextAlign },
        { default: FontFamily },
        { default: Link },
    ] = await Promise.all([
        import(cdn('@tiptap/html')),
        import(cdn('@tiptap/extension-document')),
        import(cdn('@tiptap/extension-paragraph')),
        import(cdn('@tiptap/extension-text')),
        import(cdn('@tiptap/extension-bold')),
        import(cdn('@tiptap/extension-italic')),
        import(cdn('@tiptap/extension-strike')),
        import(cdn('@tiptap/extension-heading')),
        import(cdn('@tiptap/extension-ordered-list')),
        import(cdn('@tiptap/extension-bullet-list')),
        import(cdn('@tiptap/extension-list-item')),
        import(cdn('@tiptap/extension-table')),
        import(cdn('@tiptap/extension-table-row')),
        import(cdn('@tiptap/extension-table-cell')),
        import(cdn('@tiptap/extension-table-header')),
        import(cdn('@tiptap/extension-text-style')),
        import(cdn('@tiptap/extension-color')),
        import(cdn('@tiptap/extension-underline')),
        import(cdn('@tiptap/extension-text-align')),
        import(cdn('@tiptap/extension-font-family')),
        import(cdn('@tiptap/extension-link')),
    ])

    const FontSizeCompat = Extension.create({
        name: 'fontSizeCompat',
        addGlobalAttributes() {
            return [
                {
                    types: ['textStyle'],
                    attributes: {
                        fontSize: {
                            default: null,
                            parseHTML: (el) => el.style?.fontSize || el.getAttribute('data-fs') || null,
                            renderHTML: (attrs) => {
                                if (!attrs.fontSize) return {}
                                return { style: `font-size: ${attrs.fontSize};`, 'data-fs': attrs.fontSize }
                            },
                        },
                    },
                },
            ]
        },
    })

    const NodeFontSize = Extension.create({
        name: 'nodeFontSize',
        addGlobalAttributes() {
            return [
                {
                    types: ['paragraph', 'heading', 'tableCell', 'tableHeader'],
                    attributes: {
                        fontSize: {
                            default: null,
                            parseHTML: (el) => el.style?.fontSize || el.getAttribute('data-fs') || null,
                            renderHTML: (attrs) => {
                                if (!attrs.fontSize) return {}
                                return { style: `font-size: ${attrs.fontSize};`, 'data-fs': attrs.fontSize }
                            },
                        },
                    },
                },
            ]
        },
    })

    const DataTextAlign = Extension.create({
        name: 'dataTextAlign',
        addGlobalAttributes() {
            return [
                {
                    types: ['heading', 'paragraph', 'tableCell', 'tableHeader'],
                    attributes: {
                        textAlign: {
                            default: null,
                            parseHTML: (el) => el.getAttribute('data-ta') || null,
                            renderHTML: (attrs) => (attrs.textAlign ? { 'data-ta': attrs.textAlign } : {}),
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
        addAttributes() {
            return { value: { default: '' }, locked: { default: false }, placeholder: { default: '' } }
        },
        parseHTML() {
            return [{ tag: 'span[data-input-field="1"]' }]
        },
        renderHTML({ HTMLAttributes }) {
            const value = (HTMLAttributes.value ?? '').toString()
            const locked = !!HTMLAttributes.locked
            return [
                'span',
                mergeAttributes(HTMLAttributes, {
                    'data-input-field': '1',
                    'data-locked': locked ? '1' : '0',
                    contenteditable: 'false',
                    tabindex: '-1',
                }),
                value,
            ]
        },
    })

    const HasTextCellAttr = Extension.create({
        name: 'hasTextCellAttr',
        addGlobalAttributes() {
            return [
                {
                    types: ['tableCell', 'tableHeader'],
                    attributes: {
                        hasText: {
                            default: null,
                            parseHTML: (el) => el.getAttribute('data-has-text') || null,
                            renderHTML: (attrs) => (attrs.hasText ? { 'data-has-text': String(attrs.hasText) } : {}),
                        },
                    },
                },
            ]
        },
    })

    const ResizableTableRow = TableRow.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                height: {
                    default: null,
                    parseHTML: (el) => {
                        const data = el.getAttribute('data-row-h')
                        const n1 = data ? parseInt(data, 10) : NaN
                        if (Number.isFinite(n1)) return n1

                        const h = el.style?.height || ''
                        const n2 = h ? parseInt(h, 10) : NaN
                        return Number.isFinite(n2) ? n2 : null
                    },
                    renderHTML: (attrs) =>
                        attrs.height ? { 'data-row-h': String(attrs.height), style: `height:${attrs.height}px;` } : {},
                },
            }
        },
    })

    const FixedWidthTableCell = TableCell.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                colwidth: {
                    default: null,
                    parseHTML: (el) => {
                        const w = el.style?.width || ''
                        const n = parseInt(String(w).replace('px', '').trim(), 10)
                        return Number.isFinite(n) ? [n] : null
                    },
                    renderHTML: (attrs) => {
                        const w = Array.isArray(attrs.colwidth) ? attrs.colwidth[0] : attrs.colwidth
                        return w ? { style: `width:${w}px;` } : {}
                    },
                },
            }
        },
    })

    const FixedWidthTableHeader = TableHeader.extend({
        addAttributes() {
            return {
                ...this.parent?.(),
                colwidth: {
                    default: null,
                    parseHTML: (el) => {
                        const w = el.style?.width || ''
                        const n = parseInt(String(w).replace('px', '').trim(), 10)
                        return Number.isFinite(n) ? [n] : null
                    },
                    renderHTML: (attrs) => {
                        const w = Array.isArray(attrs.colwidth) ? attrs.colwidth[0] : attrs.colwidth
                        return w ? { style: `width:${w}px;` } : {}
                    },
                },
            }
        },
    })

    const extensions = [
        Document,
        Paragraph,

        TextStyle,
        Color,
        Underline,
        Link.configure({ openOnClick: false }),
        FontFamily,
        TextAlign.configure({ types: ['heading', 'paragraph', 'tableCell', 'tableHeader'] }),
        FontSizeCompat,
        NodeFontSize,
        DataTextAlign,

        Text,
        Bold,
        Italic,
        Strike,
        Heading,
        OrderedList,
        BulletList,
        ListItem,

        Table.configure({ resizable: false }),
        ResizableTableRow,
        FixedWidthTableHeader,
        FixedWidthTableCell,

        InputField,
        HasTextCellAttr,
    ]

    // ✅ 백엔드 상세 API
    const res = await apiFetch(`/api/v1/forms/${encodeURIComponent(docfoNo)}`, { method: 'GET' })
    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`JSON fetch failed: ${res.status} ${t}`)
    }

    const payload = await res.json()

    // ✅ 헤더 렌더링
    renderHeaderFromFormDetail(payload)

    // ✅ cnttJson만 처리
    const json = normalizeTiptapJson(payload?.cnttJson)

    if (!json) {
        const html = (payload?.cnttHtml || '').trim()
        if (html) {
            mount.innerHTML = `<div class="tpl-bodyBox">${html}</div>`
            return
        }
        mount.textContent = 'cnttJson/cnttHtml이 없습니다.'
        return
    }

    const bodyHtml = generateHTML(json, extensions)
    mount.innerHTML = `<div class="tpl-bodyBox">${bodyHtml}</div>`

    const widths = extractFirstTableColWidthsFromJson(json)
    applyColgroupToBodyTables(mount, widths)
    enforceFontSizeFromDataFs(mount)
    decorateOutsideInputFields(mount)
}

// 디버그: 토큰 확인
console.log('[accessToken]', getAccessToken())

run().catch((e) => {
    console.error(e)
    mount.textContent = '렌더링 실패: ' + (e?.message || e)
})