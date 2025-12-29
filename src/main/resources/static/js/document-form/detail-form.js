// /js/document-form/detail-form.js
// 문서양식 상세(뷰) 페이지: TipTap JSON 렌더 + preset header + categories + 수정/삭제/닫기
// ✅ ViewDocumentFormController 기준
//   - 목록: /form/forms
//   - 상세: /form/{docfoNo}
//   - 생성: /form/new
//   - 수정: /form/{docfoNo}/edit

import { Editor, Node } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'

const InputField = Node.create({
    name: 'inputField',
    group: 'inline',
    inline: true,
    atom: true,

    addAttributes() {
        return {
            placeholder: { default: '입력란' },
            value: { default: '' },
            width: { default: null },
        }
    },

    parseHTML() {
        return [{ tag: 'input[data-input-field]' }]
    },

    renderHTML({ HTMLAttributes }) {
        const widthStyle = HTMLAttributes.width ? `width:${HTMLAttributes.width}px;` : ''
        return [
            'input',
            {
                ...HTMLAttributes,
                'data-input-field': 'true',
                type: 'text',
                value: HTMLAttributes.value || '',
                placeholder: HTMLAttributes.placeholder || '',
                disabled: true,
                readonly: true,
                tabindex: '-1',
                style: `
          pointer-events: none;
          background-color: #f5f5f5;
          border: 1px dashed #bbb;
          padding: 4px 6px;
          border-radius: 6px;
          ${widthStyle}
        `.replace(/\s+/g, ' ').trim(),
            },
        ]
    },
})

const API_BASE = '/api/v1/forms'
const VIEW_BASE = '/form'

function markFormListDirty() {
    try { localStorage.setItem('list:dirty', 'true') } catch (_) {}
}

function parseJwtPayload(token) {
    try {
        const t = token.replace(/^Bearer\s+/i, '')
        const base64 = t.split('.')[1]
        if (!base64) return null
        const json = decodeURIComponent(
            atob(base64.replace(/-/g, '+').replace(/_/g, '/'))
                .split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        )
        return JSON.parse(json)
    } catch (_) {
        return null
    }
}

function getUserRole() {
    const token = (localStorage.getItem('accessToken') || '').trim()
    const p = parseJwtPayload(token) || {}
    const role =
        p.role ||
        p.auth ||
        (Array.isArray(p.authorities) ? p.authorities[0] : null) ||
        (Array.isArray(p.roles) ? p.roles[0] : null) ||
        ''
    return String(role).replace(/^ROLE_/, '')
}

const ROLE = getUserRole()

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

function mustEl(id) {
    const el = document.getElementById(id)
    if (!el) throw new Error(`필수 엘리먼트 없음: #${id}`)
    return el
}

function safeText(v, fallback = '-') {
    const s = String(v ?? '').trim()
    return s ? s : fallback
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

function getDocfoNo() {
    // 1) ViewController 상세 경로: /form/{docfoNo}
    const pathParts = location.pathname.split('/').filter(Boolean)
    const last = pathParts[pathParts.length - 1]
    if (/^\d+$/.test(last)) return last

    // 2) (예외) meta 주입값
    const meta = document.querySelector('meta[name="template-id"]')
    if (meta?.content) return String(meta.content).trim()

    // 3) (레거시) querystring
    const sp = new URLSearchParams(location.search)
    const q = sp.get('docfoNo')
    return q ? String(q).trim() : ''
}

function closeOrBack() {
    window.close()
    setTimeout(() => {
        if (!document.hidden) history.back()
    }, 80)
}

const elTplTitle = mustEl('tplTitle')
const elTplCats = mustEl('tplCats')
const elTemplateMount = mustEl('templateMount')

const elPresetLeftMount = mustEl('presetLeftMount')
const elPresetRightMount = mustEl('presetRightMount')

const btnEdit = mustEl('tplEditBtn')
const btnDelete = mustEl('tplDeleteBtn')
const btnClose = mustEl('tplCloseBtn')

const presetLeftTemplate = document.getElementById('presetLeftTemplate')
const presetRightTemplate = document.getElementById('presetRightTemplate')

const RichTextStyle = TextStyle.extend({
    addAttributes() {
        return {
            ...(this.parent?.() ?? {}),

            color: {
                default: null,
                parseHTML: el => el.style.color?.replace(/['"]/g, '') || null,
                renderHTML: attrs => (attrs.color ? { style: `color: ${attrs.color}` } : {}),
            },

            fontFamily: {
                default: null,
                parseHTML: el => el.style.fontFamily?.replace(/['"]/g, '') || null,
                renderHTML: attrs => (attrs.fontFamily ? { style: `font-family: ${attrs.fontFamily}` } : {}),
            },

            fontSize: {
                default: null,
                parseHTML: el => el.style.fontSize || null,
                renderHTML: attrs => (attrs.fontSize ? { style: `font-size: ${attrs.fontSize}` } : {}),
            },
        }
    },
})

function mountPresetTables(detail) {
    if (presetLeftTemplate?.content) {
        elPresetLeftMount.innerHTML = ''
        elPresetLeftMount.appendChild(presetLeftTemplate.content.cloneNode(true))

        const tds = elPresetLeftMount.querySelectorAll('table td:last-child')
        if (tds[0]) tds[0].textContent = safeText(detail.deptName ?? detail.writerDeptName ?? detail.departmentName, '-')
        if (tds[1]) tds[1].textContent = safeText(detail.writerName ?? detail.writerId ?? detail.empName, '-')
        if (tds[2]) tds[2].textContent = safeText(detail.docfoName, '-')
        if (tds[3]) tds[3].textContent = safeText(detail.createdAt ?? detail.updatedAt ?? detail.createdDate, '-')
    }

    if (presetRightTemplate?.content) {
        elPresetRightMount.innerHTML = ''
        elPresetRightMount.appendChild(presetRightTemplate.content.cloneNode(true))
    }
}

function renderCategories(detail) {
    const raw = Array.isArray(detail.categories)
        ? detail.categories.map(c => (typeof c === 'string' ? c : c?.name)).filter(Boolean)
        : []
    const cats = uniqStrings(raw)

    elTplCats.innerHTML = ''
    if (cats.length === 0) {
        const span = document.createElement('span')
        span.className = 'tpl-muted'
        span.textContent = '카테고리 없음'
        elTplCats.appendChild(span)
        return
    }

    cats.forEach((name, idx) => {
        const label = document.createElement('label')
        label.className = 'tpl-catItem tpl-catRadio'

        const radio = document.createElement('input')
        radio.type = 'radio'
        radio.name = 'templateType'
        radio.disabled = true
        radio.checked = idx === 0

        const pill = document.createElement('span')
        pill.className = 'tpl-pill'
        pill.textContent = name

        label.appendChild(radio)
        label.appendChild(pill)
        elTplCats.appendChild(label)
    })
}

let viewer = null

function bootViewer(mountEl, json) {
    if (viewer) {
        try { viewer.destroy() } catch (_) {}
        viewer = null
    }

    viewer = new Editor({
        element: mountEl,
        editable: false,
        extensions: [
            StarterKit,
            Underline,
            RichTextStyle,
            TextAlign.configure({ types: ['heading', 'paragraph'] }),
            Table.configure({ resizable: false }),
            TableRow,
            TableHeader,
            TableCell,
            InputField,
        ],
        content: json || { type: 'doc', content: [{ type: 'paragraph' }] },
    })

    return viewer
}

async function fetchDetail(docfoNo) {
    const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`)
    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`상세 조회 실패: HTTP ${res.status} ${t}`)
    }
    return res.json()
}

async function deleteForm(docfoNo) {
    const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' })
    if (!res.ok) {
        const t = await res.text().catch(() => '')
        throw new Error(`삭제 실패: HTTP ${res.status} ${t}`)
    }
    return true
}

;(async function main() {
    const docfoNo = getDocfoNo()
    if (!docfoNo) {
        alert('docfoNo가 없습니다.')
        return
    }

    try {
        elTplTitle.textContent = '로딩 중...'

        const detail = await fetchDetail(docfoNo)

        elTplTitle.textContent = safeText(detail.docfoName, '-')
        mountPresetTables(detail)
        renderCategories(detail)

        const json = safeParseJsonMaybe(detail.cnttJson)

        elTemplateMount.innerHTML = ''
        const bodyBox = document.createElement('div')
        bodyBox.className = 'tpl-bodyBox'
        elTemplateMount.appendChild(bodyBox)

        bootViewer(bodyBox, json)

        // ✅ Employee면 수정/삭제 비활성화 (UI만 막지 말고 서버 권한도 꼭 막아야 안전)
        const isEmployee = (ROLE === 'EMPLOYEE')
        if (isEmployee) {
            btnEdit.disabled = true
            btnDelete.disabled = true
            btnEdit.title = '권한이 없습니다.'
            btnDelete.title = '권한이 없습니다.'
        }

        btnEdit.addEventListener('click', () => {
            if (isEmployee) return
            location.href = `${VIEW_BASE}/${encodeURIComponent(docfoNo)}/edit`
        })

        btnDelete.addEventListener('click', async () => {
            if (isEmployee) return

            const ok = confirm('정말 삭제할까요?')
            if (!ok) return

            try {
                btnDelete.disabled = true
                await deleteForm(docfoNo)
                markFormListDirty()
                alert('삭제되었습니다.')
                closeOrBack()
            } catch (e) {
                console.error(e)
                alert(e?.message || String(e))
            } finally {
                btnDelete.disabled = isEmployee ? true : false
            }
        })

        btnClose.addEventListener('click', closeOrBack)
    } catch (e) {
        console.error(e)
        elTplTitle.textContent = '로드 실패'
        elTemplateMount.textContent = '로드 실패'
        alert(e?.message || String(e))
    }
})()