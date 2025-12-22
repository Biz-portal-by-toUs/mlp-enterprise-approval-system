// static/src/render.js

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
    return String(s)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
}

/**
 * View 전용 레이아웃 헬퍼 (HeaderRoot 내부만 건드리도록 격리)
 */
const TemplateViewLayout = (() => {
    function afterRender(headerRoot) {
        if (!headerRoot || headerRoot.id !== 'tplHeaderRoot') return
    }
    return { afterRender }
})()

function ensurePresetHeaderCss() {
    if (document.getElementById('presetHeaderCss')) return
    const link = document.createElement('link')
    link.id = 'presetHeaderCss'
    link.rel = 'stylesheet'
    link.href = '/css/tiptap-preset-header.css'
    document.head.appendChild(link)
}

function ensureStyles() {
    if (document.getElementById('templateViewStyle')) return

    ensurePresetHeaderCss()

    const style = document.createElement('style')
    style.id = 'templateViewStyle'
    style.textContent = `
    .tpl-header { max-width: 980px; margin: 0 auto 18px; }
    .tpl-catRadio { pointer-events: none; }

    .tpl-side{ background:transparent; border:0; padding:0; min-height:0; }

    /* ===== 제목: 완전 단독 줄 ===== */
    .tpl-titleLine{
      display:flex;
      justify-content:center;
      margin: 6px 0 18px;
    }
    .tpl-title{
      margin:0;
      font-size:36px;
      font-weight:900;
      line-height:1.2;
      text-align:center;
      white-space:nowrap;
    }

    /* ===== 상단: 좌표 / (빈공간) / 우결재 ===== */
    .tpl-top{
      display:grid;
      grid-template-columns: 1fr 0.4fr 1.6fr;
      gap:28px;
      align-items:flex-start;
    }
    .tpl-spacer{ }

    .tpl-catsBox{
      margin-top:12px;
      border:1px solid #e6e6e6;
      border-radius:14px;
      background:#fff;
      padding:10px 12px;
    }
    .tpl-cats{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }

    .tpl-pill{ border:0; border-radius:0; padding:0; background:transparent; font-size:13px; }

    .tpl-catItem{ display:inline-flex; align-items:center; gap:8px; padding:4px 2px; }
    .tpl-catItem input[type="radio"]{ transform: translateY(1px); }

    .tpl-muted{ color:#777; font-size:13px; }

    #templateMount{ max-width:980px; margin:0 auto; }
    #templateMount .tpl-bodyBox [data-ta="left"]   { text-align:left !important; }
    #templateMount .tpl-bodyBox [data-ta="center"] { text-align:center !important; }
    #templateMount .tpl-bodyBox [data-ta="right"]  { text-align:right !important; }
    #templateMount .tpl-bodyBox [data-ta="justify"]{ text-align:justify !important; }

    .tpl-bodyBox p,
    .tpl-bodyBox h1,
    .tpl-bodyBox h2,
    .tpl-bodyBox h3 { margin:8px 0; }

    .tpl-bodyBox table{ border-collapse:collapse; width:100%; table-layout:fixed; }

    #templateMount .tpl-bodyBox{ overflow-x:auto; }

    .tpl-bodyBox{ font-family: inherit; }

    body{ padding-bottom:92px; }

    .tpl-footer{
      position:fixed;
      left:0; right:0; bottom:0;
      background:rgba(255,255,255,0.92);
      backdrop-filter: blur(6px);
      border-top:1px solid #e6e6e6;
      padding:12px 0;
      z-index:9999;
    }

    .tpl-footerInner{
      max-width:980px;
      margin:0 auto;
      padding:0 16px;
      display:flex;
      justify-content:flex-end;
      gap:10px;
    }

    .tpl-footerBtn{
      padding:10px 16px;
      border-radius:12px;
      border:1px solid #e6e6e6;
      background:#fff;
      cursor:pointer;
      font-size:14px;
      font-weight:700;
    }
    .tpl-footerBtn:hover{ background:#f7f7f7; }
    
    #templateMount .tpl-bodyBox .input-field-outside-empty {
      display: inline-block;
      min-width: 90px;
      padding: 6px 10px;
      border: 1px solid #e0e0e0;
      border-radius: 10px;
      background: #fafafa;
      vertical-align: baseline;
    }
    
    
  `
    document.head.appendChild(style)
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

function ensureHeaderRoot() {
    let root = document.getElementById('tplHeaderRoot')
    if (root) return root
    root = document.createElement('div')
    root.id = 'tplHeaderRoot'
    root.className = 'tpl-header'
    mount.parentElement?.insertBefore(root, mount)
    return root
}

function renderHeader(payload) {
    ensureStyles()

    const root = ensureHeaderRoot()

    const title = payload?.meta?.docTitle || payload?.docTitle || payload?.title || ''
    const preset = payload?.uiState?.presetTables || payload?.presetTables || {}
    const leftHtml = preset.leftHtml || ''
    const rightHtml = preset.rightHtml || ''
    const types = normalizeTypes(payload?.uiState?.templateTypes || [])

    const catsInner =
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

    root.innerHTML = `
      <div class="tpl-titleLine">
        <h1 class="tpl-title">${escapeHtml(title)}</h1>
      </div>

      <div class="tpl-top">
        <div class="tpl-side" data-tpl-left>
          ${leftHtml || `<span class="tpl-muted">-</span>`}
        </div>

        <div class="tpl-spacer"></div>

        <div class="tpl-side" data-tpl-right>
          ${rightHtml || `<span class="tpl-muted">-</span>`}
        </div>
      </div>

      <div class="tpl-catsBox">
        <div class="tpl-cats">
          ${catsInner}
        </div>
      </div>
    `

    TemplateViewLayout.afterRender(root)
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

function decorateOutsideInputFields(rootEl) {
    if (!rootEl) return

    const spans = [...rootEl.querySelectorAll('span[data-input-field="1"], span[data-input-field]')]
    for (const el of spans) {
        // 표 안이면 건드리지 않음
        if (el.closest('table')) continue

        // 내용이 비어있으면 박스 표시
        const txt = (el.textContent || '').trim()
        if (txt.length === 0) {
            el.classList.add('input-field-outside-empty')

            // 빈 span은 높이가 0이 되기 쉬워서 시각적으로 박스가 보이게 NBSP 주입
            el.innerHTML = '&nbsp;'
        }
    }
}

function ensureFooterActions(templateId) {
    ensureStyles()

    let footer = document.getElementById('tplFooterRoot')
    if (footer) return footer

    footer = document.createElement('div')
    footer.id = 'tplFooterRoot'
    footer.className = 'tpl-footer'
    footer.innerHTML = `
    <div class="tpl-footerInner">
      <!-- 결재하기: doc-write.html -->
      <button type="button" class="tpl-footerBtn" id="tplApproveBtn">결재하기</button>

      <!-- 수정하기: update-docform-->
      <button type="button" class="tpl-footerBtn" id="tplEditBtn">수정하기</button>

      <button type="button" class="tpl-footerBtn" id="tplCloseBtn">닫기</button>
    </div>
  `
    document.body.appendChild(footer)

    // 결재하기 → doc-write.html 열기
    footer.querySelector('#tplApproveBtn')?.addEventListener('click', () => openWriteDocPage(templateId))

    // 수정하기 → update-docform으로 연결
    footer.querySelector('#tplEditBtn')?.addEventListener('click', () => {
        alert('수정하기는 추후 양식 전체 수정(update-docform)으로 연결될 예정입니다.')
    })

    footer.querySelector('#tplCloseBtn')?.addEventListener('click', closeSafely)

    return footer
}

function enforceFontSizeFromDataFs(rootEl) {
    if (!rootEl) return;

    const nodes = rootEl.querySelectorAll('[data-fs]');
    nodes.forEach((el) => {
        const fs = (el.getAttribute('data-fs') || '').trim();
        if (!fs) return;

        // style 병합이 깨진 경우에도 font-size를 확실히 넣어줌
        el.style.fontSize = fs;
    });
}

async function run() {
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

    // View 전용 InputField: "표시하지 않기"
    // - value가 있으면 value만 보여주고
    // - value가 없으면 아무것도 표시하지 않음(placeholder 숨김)
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
                value, // 빈 값이면 그냥 빈 텍스트(표시 없음)
            ]
        },
    })

    // table 셀에 "텍스트 있음" 표시를 정식 attrs로 등록 (렌더링에 포함되게)
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
                        attrs.height
                            ? { 'data-row-h': String(attrs.height), style: `height:${attrs.height}px;` }
                            : {},
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
    ensureFooterActions(templateId)

    const json = payload?.templateJson
    if (!json) {
        mount.textContent = 'templateJson이 없습니다.'
        return
    }

    const bodyHtml = generateHTML(json, extensions)
    const dts = payload?.meta?.defaultTextStyle || {};
    const ff = dts.fontFamily ? `font-family:${dts.fontFamily};` : '';
    const fs = dts.fontSize ? `font-size:${dts.fontSize};` : '';
    mount.innerHTML = `<div class="tpl-bodyBox" style="${ff}${fs}">${bodyHtml}</div>`;

    const widths = extractFirstTableColWidthsFromJson(json)
    applyColgroupToBodyTables(mount, widths)
    enforceFontSizeFromDataFs(mount)

    decorateOutsideInputFields(mount)
}

run().catch((e) => {
    console.error(e)
    mount.textContent = '렌더링 실패: ' + (e?.message || e)
})