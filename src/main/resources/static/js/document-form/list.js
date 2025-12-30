(() => {
    // ViewDocumentFormController 기준
    //  - 목록: /form/forms
    //  - 상세: /form/{docfoNo}
    //  - 생성: /form/new
    const API_BASE = '/api/v1/forms';
    const VIEW_BASE = '/form';

    // "목록은 승인(A)만(+ X 포함)" 정책이면 true
    const ONLY_APPROVED = true;

    // ✅ 백엔드가 복수 상태(stat=A,X)를 지원하도록 바뀌었으니 여기서만 관리
    const APPROVED_STATS = ['A', 'X']; // 필요 시 ['A','X','...']로 확장

    // 권한(쿠키 인증일 때는 JS가 JWT를 못 읽으므로, 서버가 붙여준 class로 판단)
    function isEmployee() {
        return document.documentElement.classList.contains('role-employee');
    }

    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');
    const elQ = document.getElementById('q');
    const elSize = document.getElementById('size');

    const btnDeleteSelected = document.getElementById('btnDeleteSelected');
    const btnCreate = document.getElementById('btnCreate');
    const btnRefresh = document.getElementById('btnRefresh');
    const btnSearch = document.getElementById('btnSearch');
    const elToast = document.getElementById('toast');

    const selected = new Set();

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function toast(msg) {
        if (!elToast) return;
        elToast.textContent = msg;
        elToast.classList.add('show');
        window.clearTimeout(toast._t);
        toast._t = window.setTimeout(() => elToast.classList.remove('show'), 1400);
    }

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

    // 쿠키 인증이면 Authorization 헤더 없이도 동작해야 정상.
    // (혹시 혼합구조라 localStorage accessToken도 쓰면 아래를 유지해도 됨)
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

    function openDetail(docfoNo) {
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(
            `${VIEW_BASE}/${encodeURIComponent(id)}`,
            '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
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

    function normalizePage(data) {
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize?.value || 15),
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
                size: data.data.size ?? Number(elSize?.value || 15),
                totalPages: data.data.totalPages ?? 1,
                totalElements: data.data.totalElements ?? data.data.content.length,
            };
        }
        return { items: [], page: 0, size: Number(elSize?.value || 15), totalPages: 1, totalElements: 0 };
    }

    // ✅ 백엔드 복수 상태 지원(stat=A,X) 반영
    function buildUrl(statsCsvOrNull) {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize?.value || 15)));

        const q = (elQ?.value || '').trim();
        if (q) {
            params.set('q', q);
            params.set('docfoName', q);
            params.set('keyword', q);
        }

        if (statsCsvOrNull) {
            params.set('stat', statsCsvOrNull);       // 컨트롤러가 List로 받는 param
            params.set('docfoStat', statsCsvOrNull);  // 혹시 다른 구현/레거시 대비
        }

        return `${API_BASE}?${params.toString()}`;
    }

    function render(rows) {
        if (!elTbody) return;

        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * Number(elSize?.value || 15);
        const employee = isEmployee();

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';

            const idStr = String(docfoNo ?? '');
            const checked = selected.has(idStr) ? 'checked' : '';
            const rowNo = startNo + idx + 1;

            // 1열: EMPLOYEE면 번호만 / 아니면 체크박스+번호
            const firstCol = employee
                ? `<span>${rowNo}</span>`
                : `
          <label class="selWrap">
            <input type="checkbox"
                   data-role="rowCheck"
                   data-id="${esc(idStr)}"
                   ${checked}/>
            <span>${rowNo}</span>
          </label>
        `;

            return `
        <tr>
          <td class="col-select">${firstCol}</td>
          <td class="col-title">
            <a class="titleLink" href="javascript:void(0)" onclick="window.openDetail('${esc(idStr)}')">
              ${esc(docfoName)}
            </a>
          </td>
        </tr>
      `;
        }).join('');

        updateBulkDeleteUI();
    }

    function renderPager() {
        if (!elPager) return;

        const tp = Math.max(1, Number(totalPages) || 1);
        const p = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        if (tp <= 1) { elPager.innerHTML = ''; return; }

        let start = p - 2;
        let end = p + 2;

        if (start < 0) { end += (0 - start); start = 0; }
        if (end > tp - 1) { start -= (end - (tp - 1)); end = tp - 1; }
        start = Math.max(0, start);

        const nums = [];
        for (let i = start; i <= end; i++) nums.push(i);

        const disableFirstPrev = (p <= 0);
        const disableNextLast = (p >= tp - 1);

        const btn = (label, disabled, go, extra = '') =>
            `<button class="btn ${extra}" ${disabled ? 'disabled' : ''} data-go="${go}">${label}</button>`;

        const numBtn = (i) => {
            const active = i === p ? 'active' : '';
            return `<button class="btn num ${active}" ${i === p ? 'disabled' : ''} data-page="${i}">${i + 1}</button>`;
        };

        elPager.innerHTML = [
            btn('&laquo;', disableFirstPrev, 'first'),
            btn('&lsaquo;', disableFirstPrev, 'prev'),
            ...nums.map(numBtn),
            btn('&rsaquo;', disableNextLast, 'next'),
            btn('&raquo;', disableNextLast, 'last'),
        ].join('');
    }

    async function load() {
        if (!elTbody) return;
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try {
            // ✅ ONLY_APPROVED=true면 A,X를 한 번에 조회(페이징/정렬/총개수 모두 서버 기준으로 정상)
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

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${totalPages}  ·  total ${totalElements}`;

            renderPager();
            render(pg.items);
        } catch (err) {
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            renderPager();
        }
    }

    // checkbox select
    elTbody?.addEventListener('change', (e) => {
        const cb = e.target;
        if (!(cb instanceof HTMLInputElement)) return;
        if (cb.dataset.role !== 'rowCheck') return;

        const id = cb.dataset.id;
        if (!id) return;

        if (cb.checked) selected.add(id);
        else selected.delete(id);

        updateBulkDeleteUI();
    });

    // pager click
    elPager?.addEventListener('click', (e) => {
        const t = e.target;
        if (!(t instanceof HTMLElement)) return;

        const pageAttr = t.getAttribute('data-page');
        if (pageAttr != null) {
            const nextPage = Number(pageAttr);
            if (Number.isFinite(nextPage)) { page = nextPage; load(); }
            return;
        }

        const go = t.getAttribute('data-go');
        if (!go) return;

        if (go === 'first') page = 0;
        else if (go === 'prev') page = Math.max(0, page - 1);
        else if (go === 'next') page = Math.min(totalPages - 1, page + 1);
        else if (go === 'last') page = Math.max(0, totalPages - 1);

        load();
    });

    // 삭제 처리:
    // 1) PATCH /api/v1/forms/{id}/status  body: { docfoStat:"D" }
    // 2) (fallback) DELETE /api/v1/forms/{id}
    async function softDelete(docfoNo) {
        const patchRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ docfoStat: 'D' }),
        });

        if (patchRes.ok) return true;

        const delRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' });
        if (!delRes.ok) {
            const t = await delRes.text().catch(() => '');
            throw new Error(`삭제 실패(docfoNo=${docfoNo}) HTTP ${delRes.status} ${t}`);
        }
        return true;
    }

    // bulk delete
    btnDeleteSelected?.addEventListener('click', async () => {
        if (isEmployee()) { toast('권한이 없습니다.'); return; }
        if (selected.size === 0) return;

        const ids = Array.from(selected);
        if (!confirm(`선택한 ${ids.length}개를 삭제 처리할까요? (stat=D)`)) return;

        try {
            btnDeleteSelected.disabled = true;

            for (const docfoNo of ids) {
                await softDelete(docfoNo);
            }

            toast('선택 삭제 완료');
            selected.clear();
            markFormListDirty();
            await load();
        } catch (err) {
            console.error(err);
            alert('선택 삭제 실패: ' + (err?.message || err));
        } finally {
            updateBulkDeleteUI();
        }
    });

    // create
    btnCreate?.addEventListener('click', () => {
        if (isEmployee()) { toast('권한이 없습니다.'); return; }

        window.open(
            `${VIEW_BASE}/new`,
            '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    });

    btnRefresh?.addEventListener('click', () => load());
    btnSearch?.addEventListener('click', () => { page = 0; load(); });
    elQ?.addEventListener('keydown', (e) => { if (e.key === 'Enter') { page = 0; load(); } });
    elSize?.addEventListener('change', () => { page = 0; load(); });

    window.openDetail = openDetail;

    // storage listener (list:dirty)
    window.addEventListener('storage', (e) => {
        if (e.key === 'list:dirty' && e.newValue === 'true') {
            try { localStorage.removeItem('list:dirty'); } catch (_) {}
            load();
        }
    });

    // init
    if (isEmployee() && btnCreate) btnCreate.disabled = true;
    updateBulkDeleteUI();
    load();
})();