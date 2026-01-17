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

// ===== perms from server (html data-*) =====
function readPerms() {
    const root = document.documentElement

    const raw = {
        isEmployee: root.dataset.isEmployee,
        canEdit: root.dataset.canEdit,
        canDelete: root.dataset.canDelete,
        canApprove: root.dataset.canApprove,
    }

    const b = (v) => String(v ?? '').trim().toLowerCase() === 'true'

    const out = {
        isEmployee: b(raw.isEmployee),
        canEdit: b(raw.canEdit),
        canDelete: b(raw.canDelete),
        canApprove: b(raw.canApprove),
    }

    console.info('[detail-form perms]', { raw, out })
    return out
}

const PERM = readPerms()

function markFormListDirty() {
    // temp-list / forms-list 등 "목록 화면"들이 공통으로 감지할 키
    try {
        localStorage.setItem('list:dirty', 'true');
        localStorage.setItem('list:dirty:ts', String(Date.now())); // 같은 탭에서도 변경 보장
    } catch (_) {}

    // opener(목록창) 즉시 갱신 유도(같은 origin일 때만)
    try {
        if (window.opener && !window.opener.closed) {
            window.opener.postMessage(
                { type: 'LIST_DIRTY', at: Date.now() },
                window.location.origin
            );
        }
    } catch (_) {}
}

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
        // 필요하면 커스텀 class도 여기서 통일 가능
        // customClass: { popup: 'swal-popup', confirmButton: 'swal-confirm', cancelButton: 'swal-cancel' },
    })
}

async function swalInfo(title, text) {
    const swal = getSwal()
    if (!swal) { alert(`${title}\n${text || ''}`.trim()); return { isConfirmed: true } }
    return swal.fire({ icon: 'info', title, text: text || undefined })
}

async function swalSuccess(title, text) {
    const swal = getSwal()
    if (!swal) { alert(`${title}\n${text || ''}`.trim()); return { isConfirmed: true } }
    return swal.fire({ icon: 'success', title, text: text || undefined })
}

async function swalError(title, text) {
    const swal = getSwal()
    if (!swal) { alert(`${title}\n${text || ''}`.trim()); return }
    return swal.fire({ icon: 'error', title, text: text || undefined })
}

async function swalConfirm(title, text, confirmText = '확인', cancelText = '취소') {
    const swal = getSwal()
    if (!swal) return { isConfirmed: confirm(`${title}\n${text || ''}`.trim()) }

    return swal.fire({
        icon: 'warning',
        title,
        text: text || undefined,
        showCancelButton: true,
        confirmButtonText: confirmText,
        cancelButtonText: cancelText,
        reverseButtons: true,
    })
}

function swalLoading(title = '처리 중...') {
    const swal = getSwal()
    if (!swal) return

    swal.fire({
        title,
        allowOutsideClick: false,
        showConfirmButton: false,
        didOpen: () => window.Swal.showLoading(),
        heightAuto: false,
    })
}

function swalClose() {
    if (hasSwal()) window.Swal.close()
}

// 쿠키 기반 fetch (Authorization/localStorage 사용 X)
async function refreshAccessTokenIfPossible() {
    const res = await fetch('/auth/refresh', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Accept': 'application/json' },
    })
    return res.ok
}

async function apiFetch(url, options = {}, _retried = false) {
    const headers = new Headers(options.headers || {})
    if (!headers.has('Accept')) headers.set('Accept', 'application/json')

    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
    if (options.body && !isFormData && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

    const res = await fetch(url, { ...options, headers, credentials: 'same-origin' })

    if (res.status === 401 && !_retried) {
        const ok = await refreshAccessTokenIfPossible().catch(() => false)
        if (ok) return apiFetch(url, options, true)
    }
    return res
}

async function apiFetchOrThrow(url, options = {}) {
    const res = await apiFetch(url, options);
    if (!res.ok) {
        const msg = await extractErrorMessage(res);
        throw new Error(msg);
    }
    return res;
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
    const pathParts = location.pathname.split('/').filter(Boolean)
    const last = pathParts[pathParts.length - 1]
    if (/^\d+$/.test(last)) return last

    const meta = document.querySelector('meta[name="template-id"]')
    if (meta?.content) return String(meta.content).trim()

    const sp = new URLSearchParams(location.search)
    const q = sp.get('docfoNo')
    return q ? String(q).trim() : ''
}

function closeOrBack() {
    window.close()
    setTimeout(() => { if (!document.hidden) history.back() }, 80)
}

const elTplTitle = mustEl('tplTitle')
const elTplCats = mustEl('tplCats')
const elTemplateMount = mustEl('templateMount')

const btnApprove = mustEl('tplApproveBtn')
const btnEdit = mustEl('tplEditBtn')
const btnDelete = mustEl('tplDeleteBtn')
const btnClose = mustEl('tplCloseBtn')

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

function isResponseDto(obj) {
    return obj && typeof obj === 'object' && ('data' in obj) && ('message' in obj || 'status' in obj)
}

async function unwrapResponseDto(res) {
    const body = await res.json().catch(() => null)
    if (!body) return null
    return isResponseDto(body) ? body.data : body
}

async function extractErrorMessage(res) {
    const text = await res.text().catch(() => '')

    // JSON이면 message만
    try {
        const json = text ? JSON.parse(text) : null
        if (json && typeof json === 'object') {
            if (typeof json.message === 'string' && json.message.trim()) return json.message

            // 혹시 다른 구조가 섞여있을 때 대비
            if (json.error && typeof json.error.message === 'string') return json.error.message
            if (json.data && typeof json.data.message === 'string') return json.data.message
        }
    } catch (_) {
        // JSON 파싱 실패면 text fallback
    }

    // JSON 아니면 텍스트 그대로(너무 길면 컷)
    const trimmed = (text || '').trim()
    if (trimmed) return trimmed.length > 200 ? trimmed.slice(0, 200) + '…' : trimmed

    // 최후 fallback
    if (res.status === 401) return '로그인이 필요합니다.'
    if (res.status === 403) return '권한이 없습니다.'
    return `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`
}

async function fetchDetail(docfoNo) {
    const res = await apiFetchOrThrow(`${API_BASE}/${encodeURIComponent(docfoNo)}`)

    const detail = await unwrapResponseDto(res)
    if (!detail) throw new Error('상세 조회 응답이 비어있습니다.')
    return detail
}

// 삭제 동작 분기
// - 임시(T): DELETE /api/v1/forms/temp/{docfoNo}
// - 그 외 : DELETE /api/v1/forms/{docfoNo}

async function deleteForm(docfoNo, stat) {
    const s = String(stat ?? '').trim().toUpperCase()
    const url = (s === 'T')
        ? `${API_BASE}/temp/${encodeURIComponent(docfoNo)}`
        : `${API_BASE}/${encodeURIComponent(docfoNo)}`

    // 에러 응답 전체(text) 대신 message만 throw
    await apiFetchOrThrow(url, { method: 'DELETE' })
    return true
}

// ===== UI perms apply =====
function applyPermsUI({ stat }) {
    const s = String(stat ?? '').trim().toUpperCase()
    const isTemp = (s === 'T')

    // 수정
    if (!PERM.canEdit) {
        btnEdit.style.display = 'none'
        btnEdit.disabled = true
    } else {
        btnEdit.style.display = ''
        btnEdit.disabled = false
        btnEdit.textContent = isTemp ? '계속 작성' : '수정'
    }

    // 삭제
    if (!PERM.canDelete) {
        btnDelete.style.display = 'none'
        btnDelete.disabled = true
    } else {
        btnDelete.style.display = ''
        btnDelete.disabled = false
    }

    // 결재 버튼: 승인된(A) 또는 (X) 상태에서만 + 임시는 숨김
    const canGoWriteDoc = !isTemp && (s === 'A' || s === 'X')

    btnApprove.style.display = canGoWriteDoc ? '' : 'none'
    btnApprove.disabled = !canGoWriteDoc
    btnApprove.title = canGoWriteDoc ? '' : '승인된(A) 또는 (X) 상태에서만 문서를 작성할 수 있습니다.'
}

;(async function main() {
    const docfoNo = getDocfoNo()
    if (!docfoNo) {
        await swalError('잘못된 접근', 'docfoNo가 없습니다.')
        return
    }

    try {
        elTplTitle.textContent = '로딩 중...'

        const detail = await fetchDetail(docfoNo)

        const stat = String(detail.docfoStat ?? detail.stat ?? '').trim().toUpperCase()
        applyPermsUI({ stat })

        // ===== 결재 버튼 동작(기존 그대로) =====
        btnApprove.addEventListener('click', () => {
            if (btnApprove.disabled) return

            const targetUrl = `/documents/create?docfoNo=${encodeURIComponent(docfoNo)}`
            try {
                if (window.opener && !window.opener.closed) {
                    window.opener.location.href = targetUrl
                    window.opener.focus?.()
                    window.close()
                    return
                }
            } catch (e) {
                console.error('opener navigation failed:', e)
            }
            location.href = targetUrl
        })

        elTplTitle.textContent = safeText(detail.docfoName, '-')
        renderCategories(detail)

        const json = safeParseJsonMaybe(detail.cnttJson)

        elTemplateMount.innerHTML = ''
        const bodyBox = document.createElement('div')
        bodyBox.className = 'tpl-bodyBox'
        elTemplateMount.appendChild(bodyBox)

        bootViewer(bodyBox, json)

        // ===== 수정/삭제 =====
        btnEdit.addEventListener('click', async () => {
            if (!PERM.canEdit) { await swalError('권한이 없습니다.', '수정 권한이 없습니다.'); return }
            location.href = `${VIEW_BASE}/${encodeURIComponent(docfoNo)}/edit`
        })

        btnDelete.addEventListener('click', async () => {
            if (!PERM.canDelete) {
                await swalError('권한이 없습니다.', '삭제 권한이 없습니다.')
                return
            }

            const s = String(stat).toUpperCase()

            const isTemp = (s === 'T')
            const title = isTemp ? '임시 문서를 삭제할까요?' : '정말 삭제할까요?'
            const text = isTemp
                ? '임시 문서는 완전히 삭제되며 복구할 수 없습니다.'
                : '삭제요청 상태로 변경됩니다.'

            const { isConfirmed } = await swalConfirm(title, text, '삭제', '취소')
            if (!isConfirmed) return

            try {
                btnDelete.disabled = true
                await swalLoading('삭제 중...')

                await deleteForm(docfoNo, s)

                swalClose()
                markFormListDirty()
                await swalSuccess('삭제되었습니다.', isTemp ? '임시 문서를 삭제했습니다.' : '삭제요청이 등록되었습니다.')

                closeOrBack()
            } catch (e) {
                swalClose()
                console.error(e)
                await swalError('삭제 실패', e?.message || String(e))
            } finally {
                btnDelete.disabled = false
            }
        })

        btnClose.addEventListener('click', closeOrBack)
    } catch (e) {
        console.error(e)
        elTplTitle.textContent = '로드 실패'
        elTemplateMount.textContent = '로드 실패'
        await swalError('로드 실패', e?.message || String(e))
    }
})()