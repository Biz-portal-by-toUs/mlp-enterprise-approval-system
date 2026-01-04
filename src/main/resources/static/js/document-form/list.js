// /js/document-form/list.js
(() => {
    const API_BASE = '/api/v1/forms';
    const VIEW_BASE = '/form';

    // ✅ 승인된 것만 보이게(원하면 false로)
    const ONLY_APPROVED = true;
    const APPROVED_STATS = ['A', 'X'];

    // ===== DOM =====
    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');
    const elQ = document.getElementById('q');
    const elSize = document.getElementById('size');
    const elThSelect = document.getElementById('thSelect');

    const btnDeleteSelected = document.getElementById('btnDeleteSelected');
    const btnCreate = document.getElementById('btnNew');
    const btnSearch = document.getElementById('btnSearch');

    const elCountPill = document.getElementById('countPill');

    if (!elTbody || !elPager || !elQ || !elSize || !btnSearch) {
        console.error('list page DOM missing');
        return;
    }

    // ===== role (JWT -> html.class) =====
    function parseJwtPayload(token) {
        try {
            const t = token.replace(/^Bearer\s+/i, '');
            const base64 = t.split('.')[1];
            if (!base64) return null;
            const json = decodeURIComponent(
                atob(base64.replace(/-/g, '+').replace(/_/g, '/'))
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            return JSON.parse(json);
        } catch (_) {
            return null;
        }
    }

    function getUserRoleFromToken() {
        const token = (localStorage.getItem('accessToken') || '').trim();
        const p = parseJwtPayload(token) || {};
        const role =
            p.role ||
            p.auth ||
            (Array.isArray(p.authorities) ? p.authorities[0] : null) ||
            (Array.isArray(p.roles) ? p.roles[0] : null) ||
            '';
        return String(role).replace(/^ROLE_/, '');
    }

    // 서버에서 th:classappend로 role-employee를 이미 넣었다면 이건 중복돼도 OK
    (function ensureRoleClass() {
        const r = getUserRoleFromToken();
        if (r === 'EMPLOYEE') document.documentElement.classList.add('role-employee');
    })();

    function isEmployee() {
        return document.documentElement.classList.contains('role-employee');
    }

    // ===== state =====
    const selected = new Set();
    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    // ===== util =====
    function esc(s) {
        return String(s ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function markFormListDirty() {
        try { localStorage.setItem('list:dirty', 'true'); } catch (_) {}
    }

    function getAccessToken() {
        return (localStorage.getItem('accessToken') || '').trim();
    }

    async function apiFetch(url, options = {}) {
        const token = getAccessToken();
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) headers.set('Accept', 'application/json');

        const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
        if (options.body && !isFormData && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        if (token) headers.set('Authorization', /^Bearer\s+/i.test(token) ? token : `Bearer ${token}`);

        return fetch(url, { ...options, headers, credentials: 'same-origin' });
    }

    function normalizePage(data) {
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize.value || 15),
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length,
            };
        }
        if (Array.isArray(data)) {
            return { items: data, page: 0, size: data.length, totalPages: 1, totalElements: data.length };
        }
        if (data && data.data && Array.isArray(data.data.content)) {
            return {
                items: data.data.content,
                page: data.data.number ?? 0,
                size: data.data.size ?? Number(elSize.value || 15),
                totalPages: data.data.totalPages ?? 1,
                totalElements: data.data.totalElements ?? data.data.content.length,
            };
        }
        return { items: [], page: 0, size: Number(elSize.value || 15), totalPages: 1, totalElements: 0 };
    }

    function buildUrl(statsCsvOrNull) {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize.value || 15)));

        const q = (elQ.value || '').trim();
        if (q) {
            // 서버가 어떤 파라미터를 보든 잡히게 “다 넣기”
            params.set('q', q);
            params.set('docfoName', q);
            params.set('keyword', q);
        }

        if (statsCsvOrNull) {
            // Spring에서 List 파라미터 받는 방식에 따라
            // 1) stat=A&stat=X 형태가 제일 안전하지만
            // 너 코드에선 params.set('stat', ...)로 이미 쓰고 있어서 유지.
            // (만약 서버에서 List로 못 받으면 아래를 stat 반복으로 바꿔줘야 함)
            params.set('stat', statsCsvOrNull);
            params.set('docfoStat', statsCsvOrNull);
        }

        return `${API_BASE}?${params.toString()}`;
    }

    // ===== UI helpers =====
    function openDetail(docfoNo) {
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(
            `${VIEW_BASE}/${encodeURIComponent(id)}`,
            '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    }
    window.openDetail = openDetail;

    function clearSelection() {
        selected.clear();
        updateBulkDeleteUI();
    }

    function updateHeaderLabel() {
        if (!elThSelect) return;
        elThSelect.textContent = isEmployee() ? '번호' : '선택/번호';
    }

    function updateBulkDeleteUI() {
        if (!btnDeleteSelected) return;

        if (isEmployee()) {
            btnDeleteSelected.style.display = 'none';
            btnDeleteSelected.disabled = true;
            return;
        }

        btnDeleteSelected.style.display = '';
        const n = selected.size;
        btnDeleteSelected.textContent = n > 0 ? `선택 삭제 (${n})` : '선택 삭제';
        btnDeleteSelected.disabled = (n === 0);
        btnDeleteSelected.title = (n === 0) ? '삭제할 항목을 선택하세요.' : '';
    }

    function render(rows) {
        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const size = Number(elSize.value || 15);
        const employee = isEmployee();

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            const idStr = String(docfoNo ?? '');

            // ✅ “오름차순 번호” (페이지 기준)
            const rowNo = (page * size) + idx + 1;

            // ✅ Employee면 체크박스 DOM 자체 생성 안 함
            const firstCol = employee
                ? `<span>${rowNo}</span>`
                : `
          <label class="selWrap">
            <input type="checkbox"
                   data-role="rowCheck"
                   data-id="${esc(idStr)}"
                   ${selected.has(idStr) ? 'checked' : ''}/>
            <span>${rowNo}</span>
          </label>
        `;

            return `
        <tr>
          <td class="col-no">${firstCol}</td>
          <td class="col-title">
            <a class="titleLink" href="javascript:void(0)" data-open="${esc(idStr)}">
              ${esc(docfoName)}
            </a>
          </td>
        </tr>
      `;
        }).join('');

        updateBulkDeleteUI();
    }

    function renderPager() {
        elPager.innerHTML = "";

        const tp = Math.max(1, Number(totalPages) || 1); // ✅ 최소 1
        const cur = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        const blockSize = 5;
        const currentBlock = Math.floor(cur / blockSize);
        const startPage = currentBlock * blockSize;
        let endPage = startPage + blockSize - 1;
        if (endPage > tp - 1) endPage = tp - 1;

        const createBtn = (txt, target, active = false, disabled = false) => {
            const b = document.createElement("button");
            b.className = `pageBtn ${active ? "active" : ""}`;
            b.textContent = txt;
            b.disabled = disabled;
            b.type = 'button';
            b.onclick = () => {
                page = target;
                clearSelection();
                load();
            };
            return b;
        };

        elPager.appendChild(createBtn("<", cur - 1, false, cur <= 0));

        // ✅ tp=1이어도 1 버튼 생성됨
        for (let i = startPage; i <= endPage; i++) {
            elPager.appendChild(createBtn(String(i + 1), i, i === cur, i === cur));
        }

        elPager.appendChild(createBtn(">", cur + 1, false, cur >= tp - 1));
    }

    // ===== data load =====
    async function load() {
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try {
            const statsCsv = ONLY_APPROVED ? APPROVED_STATS.join(',') : null;

            const res = await apiFetch(buildUrl(statsCsv), { method: 'GET' });
            if (!res.ok) {
                const t = await res.text().catch(() => '');
                throw new Error(`HTTP ${res.status} ${t}`);
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
    // row click: link open
    elTbody.addEventListener('click', (e) => {
        const a = e.target.closest('a[data-open]');
        if (a) {
            const id = a.getAttribute('data-open');
            openDetail(id);
            return;
        }
    });

    // checkbox select
    elTbody.addEventListener('change', (e) => {
        const cb = e.target;
        if (!(cb instanceof HTMLInputElement)) return;
        if (cb.dataset.role !== 'rowCheck') return;

        const id = cb.dataset.id;
        if (!id) return;

        if (cb.checked) selected.add(id);
        else selected.delete(id);

        updateBulkDeleteUI();
    });

    async function softDelete(docfoNo) {
        // 1) status patch(D) 시도
        const patchRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ docfoStat: 'D' }),
        });

        if (patchRes.ok) return true;

        // 2) fallback DELETE
        const delRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' });
        if (!delRes.ok) {
            const t = await delRes.text().catch(() => '');
            throw new Error(`삭제 실패(docfoNo=${docfoNo}) HTTP ${delRes.status} ${t}`);
        }
        return true;
    }

    btnDeleteSelected?.addEventListener('click', async () => {
        if (isEmployee()) { alert('권한이 없습니다.'); return; }
        if (selected.size === 0) return;

        const ids = Array.from(selected);
        if (!confirm(`선택한 ${ids.length}개를 삭제 처리할까요?`)) return;

        try {
            btnDeleteSelected.disabled = true;
            for (const docfoNo of ids) {
                await softDelete(docfoNo);
            }
            selected.clear();
            markFormListDirty();
            await load();
            alert('선택 삭제 완료');
        } catch (err) {
            console.error(err);
            alert('선택 삭제 실패: ' + (err?.message || err));
        } finally {
            updateBulkDeleteUI();
        }
    });

    btnCreate?.addEventListener('click', () => {
        if (isEmployee()) { alert('권한이 없습니다.'); return; }

        window.open(
            `${VIEW_BASE}/new`,
            '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    });

    btnSearch.addEventListener('click', () => {
        page = 0;
        clearSelection();
        load();
    });

    elQ.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            page = 0;
            clearSelection();
            load();
        }
    });

    elSize.addEventListener('change', () => {
        page = 0;
        clearSelection();
        load();
    });

    // list dirty refresh (팝업에서 수정/삭제 후)
    window.addEventListener('storage', (e) => {
        if (e.key === 'list:dirty' && e.newValue === 'true') {
            try { localStorage.removeItem('list:dirty'); } catch (_) {}
            load();
        }
    });

    // init: Employee면 버튼 숨김 + 헤더 텍스트 변경
    if (isEmployee() && btnCreate) btnCreate.style.display = 'none';
    updateBulkDeleteUI();
    updateHeaderLabel();
    load();
})();