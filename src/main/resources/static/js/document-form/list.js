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

    const btnDeleteSelected = document.getElementById('btnDeleteSelected'); // th:if로 없을 수도 있음
    const btnCreate = document.getElementById('btnNew');                    // th:if로 없을 수도 있음
    const btnSearch = document.getElementById('btnSearch');

    const elCountPill = document.getElementById('countPill');

    if (!elTbody || !elPager || !elQ || !elSize || !btnSearch) {
        console.error('list page DOM missing');
        return;
    }

    // ===== perms from server (html data-*) =====
    function readPerms() {
        const root = document.documentElement;

        const raw = {
            isEmployee: root.dataset.isEmployee,
            canCreate: root.dataset.canCreate,
            canBulkDelete: root.dataset.canBulkDelete,
            canApprove: root.dataset.canApprove,
            canUseTemp: root.dataset.canUseTemp,
        };

        const b = (v) => String(v ?? '').trim().toLowerCase() === 'true';

        const out = {
            isEmployee: b(raw.isEmployee),
            canCreate: b(raw.canCreate),
            canBulkDelete: b(raw.canBulkDelete),
            canApprove: b(raw.canApprove),
            canUseTemp: b(raw.canUseTemp),
        };

        // 필요하면 디버깅
        // console.info('[form-list perms]', { raw, out });

        return out;
    }

    const PERM = readPerms();

    function isEmployee() {
        return PERM.isEmployee;
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

    function markFormListDirty() {
        try { localStorage.setItem('list:dirty', 'true'); } catch (_) {}
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
        // Spring Page 그대로
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize.value || 15),
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length,
            };
        }
        // 배열만 올 수도 있음
        if (Array.isArray(data)) {
            return { items: data, page: 0, size: data.length, totalPages: 1, totalElements: data.length };
        }
        // data.data.page 같은 래핑
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

    // ===== state =====
    const selected = new Set();
    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function buildUrl(statsCsvOrNull) {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize.value || 15)));

        const q = (elQ.value || '').trim();
        if (q) params.set('q', q);

        // ✅ 서버 컨트롤러가 받는 건 stat 하나면 충분
        if (statsCsvOrNull) params.set('stat', statsCsvOrNull);

        return `${API_BASE}?${params.toString()}`;
    }

    // ===== UI helpers =====
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

        // 서버 플래그 기준 + employee는 무조건 숨김
        if (!PERM.canBulkDelete || isEmployee()) {
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
        const showCheckbox = PERM.canBulkDelete && !employee;

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            const idStr = String(docfoNo ?? '').trim();

            const rowNo = (page * size) + idx + 1;

            const firstCol = showCheckbox
                ? `
          <label class="selWrap">
            <input type="checkbox"
                   data-role="rowCheck"
                   data-id="${esc(idStr)}"
                   ${selected.has(idStr) ? 'checked' : ''}/>
            <span>${rowNo}</span>
          </label>
        `
                : `<span>${rowNo}</span>`;

            return `
        <tr data-row-id="${esc(idStr)}">
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

        const tp = Math.max(1, Number(totalPages) || 1);
        const cur = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        const blockSize = 5; // ✅ 사이트 컨셉이 10 고정이면 여기를 10으로 바꾸면 됨
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

            if (res.status === 401) {
                elTbody.innerHTML = `<tr><td colspan="2" class="muted">로그인이 만료되었습니다. 다시 로그인 해주세요.</td></tr>`;
                return;
            }
            if (res.status === 403) {
                elTbody.innerHTML = `<tr><td colspan="2" class="muted">권한이 없습니다.</td></tr>`;
                return;
            }

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
    elTbody.addEventListener('click', (e) => {
        // ✅ 체크박스 클릭은 상세 열지 않기
        if (e.target instanceof HTMLInputElement && e.target.type === 'checkbox') return;

        const a = e.target.closest('a[data-open]');
        if (a) {
            openDetail(a.getAttribute('data-open'));
            return;
        }
    });

    elTbody.addEventListener('change', (e) => {
        const cb = e.target;
        if (!(cb instanceof HTMLInputElement)) return;
        if (cb.dataset.role !== 'rowCheck') return;

        const id = String(cb.dataset.id ?? '').trim();
        if (!id) return;

        if (cb.checked) selected.add(id);
        else selected.delete(id);

        updateBulkDeleteUI();
    });

    async function softDelete(docfoNo) {
        // 지금 백 정책이 "삭제요청"이면 DELETE만 쓰는게 제일 깔끔해
        const delRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' });
        if (!delRes.ok) {
            const t = await delRes.text().catch(() => '');
            throw new Error(`삭제 실패(docfoNo=${docfoNo}) HTTP ${delRes.status} ${t}`);
        }
        return true;
    }

    btnDeleteSelected?.addEventListener('click', async () => {
        if (!PERM.canBulkDelete || isEmployee()) { alert('권한이 없습니다.'); return; }
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
        if (!PERM.canCreate || isEmployee()) { alert('권한이 없습니다.'); return; }

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

    // init
    (function init() {
        updateBulkDeleteUI();
        updateHeaderLabel();
        load();
    })();
})();