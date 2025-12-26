// static/src/detail.js

const TIPTAP_V = '2.11.2'

const PM_EXTERNAL = [
    'prosemirror-model',
    'prosemirror-state',
    'prosemirror-view',
    'prosemirror-transform',
    'prosemirror-commands',
    'prosemirror-keymap',
    'prosemirror-history',
    'prosemirror-schema-list',
    'prosemirror-dropcursor',
    'prosemirror-gapcursor',
].join(',')

const cdn = (pkg) =>
    `https://esm.sh/${pkg}@${TIPTAP_V}?bundle&target=es2020&external=${encodeURIComponent(PM_EXTERNAL)}`

const meta = document.querySelector('meta[name="template-id"]')
const templateId = meta?.content

const mount = document.getElementById('templateMount')
if (!mount) throw new Error('#templateMount not found')

if (!templateId) {
    mount.textContent = 'template-id가 없습니다.'
    throw new Error('template-id is missing')
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

function renderHeader(payload) {
    const title = payload?.meta?.docTitle || payload?.docTitle || payload?.title || ''
    const preset = payload?.uiState?.presetTables || payload?.presetTables || {}
    const leftHtml = preset.leftHtml || ''
    const rightHtml = preset.rightHtml || ''
    const types = normalizeTypes(payload?.uiState?.templateTypes || [])

    const elTitle = document.getElementById('tplTitle')
    const elLeft = document.getElementById('tplLeftSide')
    const elRight = document.getElementById('tplRightSide')
    const elCats = document.getElementById('tplCats')

    if (elTitle) elTitle.textContent = title || '-'
    if (elLeft) elLeft.innerHTML = leftHtml || `<span class="tpl-muted">-</span>`
    if (elRight) elRight.innerHTML = rightHtml || `<span class="tpl-muted">-</span>`

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

function openWriteDocPage(templateId) {
    const w = 1100
    const h = 800
    const l = Math.floor((window.screen.width - w) / 2)
    const t = Math.floor((window.screen.height - h) / 2)

    const url = new URL('/templates/doc-write.html', window.location.origin)
    url.searchParams.set('id', templateId)

    window.open(
        url.toString(),
        'docWrite',
        `width=${w},height=${h},left=${l},top=${t},resizable=yes,scrollbars=yes`,
    )
}

function bindFooterActions(templateId) {
    document.getElementById('tplApproveBtn')?.addEventListener('click', () => openWriteDocPage(templateId))

    document.getElementById('tplEditBtn')?.addEventListener('click', () => {
        const url = `/document-form/manager/form/update-form?docfoNo=${encodeURIComponent(templateId)}`
        location.href = url
    })

    document.getElementById('tplCloseBtn')?.addEventListener('click', closeSafely)
}

async function run() {
    bindFooterActions(templateId)

    await Promise.all([
        import(`https://esm.sh/prosemirror-model?target=es2020`),
        import(`https://esm.sh/prosemirror-state?target=es2020`),
        import(`https://esm.sh/prosemirror-view?target=es2020`),
        import(`https://esm.sh/prosemirror-transform?target=es2020`),
        import(`https://esm.sh/prosemirror-commands?target=es2020`),
        import(`https://esm.sh/prosemirror-keymap?target=es2020`),
        import(`https://esm.sh/prosemirror-history?target=es2020`),
        import(`https://esm.sh/prosemirror-schema-list?target=es2020`),
        import(`https://esm.sh/prosemirror-dropcursor?target=es2020`),
        import(`https://esm.sh/prosemirror-gapcursor?target=es2020`),
    ])

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

    const res = await fetch(`/api/tiptap/templates/${encodeURIComponent(templateId)}`, {
        headers: { Accept: 'application/json' },
    })
    if (!res.ok) throw new Error(`JSON fetch failed: ${res.status}`)

    const payload = await res.json()

    renderHeader(payload)

    const json = payload?.templateJson
    if (!json) {
        mount.textContent = 'templateJson이 없습니다.'
        return
    }

    const bodyHtml = generateHTML(json, extensions)
    const dts = payload?.meta?.defaultTextStyle || {}
    const ff = dts.fontFamily ? `font-family:${dts.fontFamily};` : ''
    const fs = dts.fontSize ? `font-size:${dts.fontSize};` : ''
    mount.innerHTML = `<div class="tpl-bodyBox" style="${ff}${fs}">${bodyHtml}</div>`

    const widths = extractFirstTableColWidthsFromJson(json)
    applyColgroupToBodyTables(mount, widths)
    enforceFontSizeFromDataFs(mount)
    decorateOutsideInputFields(mount)
}

run().catch((e) => {
    console.error(e)
    mount.textContent = '렌더링 실패: ' + (e?.message || e)
})