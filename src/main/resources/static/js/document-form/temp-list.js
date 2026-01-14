// /js/document-form/temp-list.js
(() => {
    const API_BASE = '/api/v1/forms/temp';
    const VIEW_BASE = '/form';

    const blockSize = 5;

    // ===== DOM =====
    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');
    const elQ = document.getElementById('q');
    const elSize = document.getElementById('size');
    const btnSearch = document.getElementById('btnSearch');
    const elCountPill = document.getElementById('countPill');

    if (!elTbody || !elPager || !btnSearch) {
        console.warn('[temp-list] required DOM missing, script aborted');
        return;
    }

    // ===== perms from server (html data-*) =====
    function readPerms() {
        const root = document.documentElement;
        const b = (v) => String(v ?? '').trim().toLowerCase() === 'true';

        return {
            isEmployee: b(root?.dataset?.isEmployee)
        };
    }

    const PERM = readPerms();

    function hasRoleClass(name) {
        const html = document.documentElement?.classList;
        const body = document.body?.classList;
        return (html && html.contains(name)) || (body && body.contains(name));
    }

    function isEmployee() {
        // dataset 우선, 없으면 class로 보조
        if (typeof PERM.isEmployee === 'boolean') return PERM.isEmployee;
        return hasRoleClass('role-employee') || hasRoleClass('role-EMPLOYEE');
    }

    // 정책: 임시저장은 직원 접근 불가
    if (isEmployee()) {
        alert('권한이 없습니다. (임시저장 문서양식은 관리자만 접근 가능합니다.)');
        location.replace('/form/forms');
        return;
    }

    // ===== util =====
    function esc(s) {
        return String(s ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function openDetail(docfoNo) {
        if (docfoNo == null) return;
        const id = String(docfoNo).trim();
        if (!id) return;

        window.open(
            `${VIEW_BASE}/${encodeURIComponent(id)}`,
            '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    }

    // ===== 쿠키 기반 fetch + 401 refresh 재시도 =====
    async function refreshAccessTokenIfPossible() {
        const res = await fetch('/auth/refresh', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' },
        });
        return res.ok;
    }

    function isResponseDto(obj) {
        return obj && typeof obj === 'object' && ('data' in obj) && (('status' in obj) || ('message' in obj));
    }

    async function unwrapJson(res) {
        const body = await res.json().catch(() => null);
        if (!body) return null;
        return isResponseDto(body) ? body.data : body;
    }

    async function extractErrorMessage(res) {
        const text = await res.text().catch(() => '');

        // JSON이면 message만
        try {
            const json = text ? JSON.parse(text) : null;
            if (json && typeof json === 'object') {
                if (typeof json.message === 'string' && json.message.trim()) return json.message;

                // 혹시 다른 구조가 섞여있을 때 대비
                if (json.error && typeof json.error.message === 'string') return json.error.message;
                if (json.data && typeof json.data.message === 'string') return json.data.message;
            }
        } catch (_) {
            // JSON 파싱 실패면 text fallback
        }

        // JSON 아니면 텍스트 그대로(너무 길면 컷)
        const trimmed = (text || '').trim();
        if (trimmed) return trimmed.length > 200 ? trimmed.slice(0, 200) + '…' : trimmed;

        // 최후 fallback
        if (res.status === 401) return '로그인이 필요합니다.';
        if (res.status === 403) return '권한이 없습니다.';
        return `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`;
    }

    async function apiFetch(url, options = {}, _retried = false) {
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) headers.set('Accept', 'application/json');

        const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
        if (options.body && !isFormData && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        const res = await fetch(url, {
            ...options,
            headers,
            credentials: 'same-origin',
        });

        if (res.status === 401 && !_retried) {
            const ok = await refreshAccessTokenIfPossible().catch(() => false);
            if (ok) return apiFetch(url, options, true);
        }

        return res;
    }

    function normalizePage(data) {
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize?.value || 10),
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length,
            };
        }
        if (data && data.data && Array.isArray(data.data.content)) {
            return {
                items: data.data.content,
                page: data.data.number ?? 0,
                size: data.data.size ?? Number(elSize?.value || 10),
                totalPages: data.data.totalPages ?? 1,
                totalElements: data.data.totalElements ?? data.data.content.length,
            };
        }
        if (Array.isArray(data)) {
            return { items: data, page: 0, size: data.length, totalPages: 1, totalElements: data.length };
        }
        return { items: [], page: 0, size: Number(elSize?.value || 10), totalPages: 1, totalElements: 0 };
    }

    // ===== state =====
    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function buildUrl() {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize?.value || 10)));

        const q = (elQ?.value || '').trim();
        if (q) params.set('q', q);

        return `${API_BASE}?${params.toString()}`;
    }

    // ===== render =====
    function render(rows) {
        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const size = Number(elSize?.value || 10);

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            const idStr = String(docfoNo ?? '').trim();

            const rowNo = (page * size) + idx + 1;

            return `
        <tr data-row-id="${esc(idStr)}">
          <td class="col-no">${rowNo}</td>
          <td class="col-title">
            <a class="titleLink" href="javascript:void(0)" data-open="${esc(idStr)}">
              ${esc(docfoName)}
            </a>
          </td>
        </tr>
      `;
        }).join('');
    }

    function renderPager() {
        elPager.innerHTML = '';

        const tp = Math.max(1, Number(totalPages) || 1);
        const cur = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        const currentBlock = Math.floor(cur / blockSize);
        const startPage = currentBlock * blockSize;
        let endPage = startPage + blockSize - 1;
        if (endPage > tp - 1) endPage = tp - 1;

        const createBtn = (txt, target, active = false, disabled = false) => {
            const b = document.createElement('button');
            b.className = `pageBtn ${active ? 'active' : ''}`;
            b.textContent = txt;
            b.disabled = disabled;
            b.type = 'button';
            b.onclick = () => {
                page = target;
                load();
            };
            return b;
        };

        elPager.appendChild(createBtn('<', cur - 1, false, cur <= 0));
        for (let i = startPage; i <= endPage; i++) {
            elPager.appendChild(createBtn(String(i + 1), i, i === cur, false));
        }
        elPager.appendChild(createBtn('>', cur + 1, false, cur >= tp - 1));
    }

    // ===== load =====
    async function load() {
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try {
            const res = await apiFetch(buildUrl(), { method: 'GET' });

            if (res.status === 401) {
                elTbody.innerHTML = `<tr><td colspan="2" class="muted">로그인이 만료되었습니다. 다시 로그인 해주세요.</td></tr>`;
                return;
            }
            if (res.status === 403) {
                elTbody.innerHTML = `<tr><td colspan="2" class="muted">권한이 없습니다.</td></tr>`;
                return;
            }

            if (!res.ok) {
                const msg = await extractErrorMessage(res);
                throw new Error(msg);
            }

            const pg = normalizePage(await res.json());

            totalPages = pg.totalPages ?? 1;
            totalElements = pg.totalElements ?? 0;
            page = pg.page ?? page;

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${Math.max(totalPages, 1)}`;
            if (elCountPill) elCountPill.textContent = `${totalElements}건`;

            renderPager();
            render(pg.items);
        } catch (err) {
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            totalPages = 1;
            renderPager();
        }
    }

    // ===== events =====
    elTbody.addEventListener('click', (e) => {
        const a = e.target.closest('a[data-open]');
        if (a) {
            openDetail(a.getAttribute('data-open'));
        }
    });

    btnSearch.addEventListener('click', () => {
        page = 0;
        load();
    });

    elQ?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            page = 0;
            load();
        }
    });

    elSize?.addEventListener('change', () => {
        page = 0;
        load();
    });

    function consumeDirtyAndReload() {
        try {
            const dirty = localStorage.getItem('list:dirty');
            if (dirty === 'true') {
                localStorage.setItem('list:dirty', 'false');
                load();
            }
        } catch (_) {}
    }

    // (1) 페이지가 다시 포커스될 때(팝업 닫고 돌아오면) 체크
    window.addEventListener("visibilitychange", () => {
        if (!document.hidden) consumeDirtyAndReload();
    });

    // (2) 다른 탭/창에서 localStorage 변경되면 즉시 반영
    window.addEventListener("storage", (e) => {
        if (e.key === "list:dirty" || e.key === "list:dirty:ts") {
            consumeDirtyAndReload();
        }
    });

    window.addEventListener("message", (e) => {
        if (e.origin !== window.location.origin) return;
        if (e.data?.type === "LIST_DIRTY") {
            consumeDirtyAndReload();
        }
    });

    window.addEventListener("focus", () => consumeDirtyAndReload());

    // init
    load();
})();