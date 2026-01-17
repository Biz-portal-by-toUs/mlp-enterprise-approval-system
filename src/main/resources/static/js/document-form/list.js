// /js/document-form/list.js
(() => {
    const API_BASE = '/api/v1/forms';
    const VIEW_BASE = '/form';

    const PAGE_SIZE = 10;

    // 승인된 것만 보이게(원하면 false로)
    const ONLY_APPROVED = true;
    const APPROVED_STATS = ['A', 'X', 'W'];

    // ===== DOM =====
    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');
    const elQ = document.getElementById('q');
    const elThSelect = document.getElementById('thSelect');

    const btnDeleteSelected = document.getElementById('btnDeleteSelected'); // th:if로 없을 수도 있음
    const btnCreate = document.getElementById('btnNew');                    // th:if로 없을 수도 있음
    const btnSearch = document.getElementById('btnSearch');

    const elCountPill = document.getElementById('countPill');

    if (!elTbody || !elPager || !elQ || !btnSearch) {
        console.warn('[form-list] required DOM missing, script aborted');
        return;
    }

    // ===== perms from server (html data-*) =====
    function readPerms() {
        const root = document.documentElement;

        const raw = {
            isEmployee: root.dataset.isEmployee,
            canCreate: root.dataset.canCreate,
            canBulkDelete: root.dataset.canBulkDelete,

        };

        const b = (v) => String(v ?? '').trim().toLowerCase() === 'true';

        const out = {
            isEmployee: b(raw.isEmployee),
            canCreate: b(raw.canCreate),
            canBulkDelete: b(raw.canBulkDelete),
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

    function buildRejectedStyleDeletePopup(ids) {
        const items = ids.map((id, i) => {
            const title = formTitleMap.get(id) || `양식 #${id}`;

            return `
              <div style="
                display:flex;
                justify-content:space-between;
                align-items:center;
                padding:12px 14px;
                border:1px solid #dee2e6;
                border-radius:6px;
                background:#f8f9fa;
                font-size:13px;
                margin-top:10px;
              ">
                <div>
                  <b>${esc(title)}</b>
                </div>
                <div style="color:#868e96;">DELETE 요청</div>
              </div>
            `;
                }).join('');

        return `
            <div style="
              font-family:'Pretendard',-apple-system,sans-serif;
              text-align:left;
              color:#333;
            ">
              <div style="
                font-size:20px;
                font-weight:700;
                color:#fa5252;
                border-bottom:2px solid #fa5252;
                padding-bottom:12px;
                margin-bottom:16px;
              ">
                선택 삭제
              </div>
        
              <div style="
                font-size:13px;
                color:#868e96;
                margin-bottom:14px;
              ">
                선택한 문서양식을 삭제 요청 처리합니다.<br/>
              </div>
        
              <div style="
                border:1px solid #dee2e6;
                border-radius:8px;
                padding:16px;
                background:#fff;
              ">
                <div style="font-weight:700;font-size:14px;margin-bottom:6px;">
                  삭제 대상 (${ids.length}건)
                </div>
                ${items}
              </div>
            </div>
          `;
    }

    async function batchAllSettled(items, batchSize, taskFn) {
        const results = [];

        for (let i = 0; i < items.length; i += batchSize) {
            const batch = items.slice(i, i + batchSize);

            const batchResults = await Promise.allSettled(
                batch.map(item => taskFn(item))
            );

            results.push(...batchResults);
        }

        return results;
    }

    async function runBulkDeleteRejectedStyle(ids) {
        const result = await Swal.fire({
            html: buildRejectedStyleDeletePopup(ids),
            showCancelButton: true,
            confirmButtonText: `삭제 실행 (${ids.length})`,
            cancelButtonText: '취소',
            confirmButtonColor: '#fa5252',
            cancelButtonColor: '#adb5bd',
            reverseButtons: true,
            width: 760,
            focusConfirm: false,
            preConfirm: async () => {
                Swal.showLoading();

                const results = await batchAllSettled(
                    ids,
                    5,              // ✅ 여기서 "5개씩"
                    softDelete
                );

                const failed = results
                    .map((r, i) => ({ r, id: ids[i] }))
                    .filter(x => x.r.status === 'rejected');

                if (failed.length > 0) {
                    throw new Error(
                        `${failed.length}건 삭제 실패\n` +
                        failed.map(f =>
                            `#${f.id}: ${f.r.reason?.message || f.r.reason}`
                        ).join('\n')
                    );
                }

                return true;
            }
        });

        return result.isConfirmed;
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
        const controller = new AbortController();
        const t = setTimeout(() => controller.abort(), 15000); // 15초

        try {
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
                signal: controller.signal,
            });

            if (res.status === 401 && !_retried) {
                const ok = await refreshAccessTokenIfPossible().catch(() => false);
                if (ok) return apiFetch(url, options, true);
            }

            return res;
        } finally {
            clearTimeout(t);
        }
    }

    function normalizePage(data) {
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: PAGE_SIZE,
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length,
            };
        }
        if (Array.isArray(data)) {
            return { items: data, page: 0, size: PAGE_SIZE, totalPages: 1, totalElements: data.length };
        }
        if (data && data.data && Array.isArray(data.data.content)) {
            return {
                items: data.data.content,
                page: data.data.number ?? 0,
                size: PAGE_SIZE,
                totalPages: data.data.totalPages ?? 1,
                totalElements: data.data.totalElements ?? data.data.content.length,
            };
        }
        return { items: [], page: 0, size: PAGE_SIZE, totalPages: 1, totalElements: 0 };
    }

    // ===== state =====
    const selected = new Set();
    const formTitleMap = new Map();
    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function buildUrl(statsCsvOrNull) {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(PAGE_SIZE));

        const q = (elQ.value || '').trim();
        if (q) params.set('q', q);

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

        const size = PAGE_SIZE;
        const employee = isEmployee();
        const showCheckbox = PERM.canBulkDelete && !employee;

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            if (docfoNo != null) {
                formTitleMap.set(String(docfoNo), docfoName);
            }
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

        const blockSize = 5; // 페이지네이션에서 한 번에 보여줄 버튼 개수
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
            elPager.appendChild(createBtn(String(i + 1), i, i === cur, false));
        }
        elPager.appendChild(createBtn(">", cur + 1, false, cur >= tp - 1));
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

        // JSON이면 message만 뽑기
        try {
            const json = text ? JSON.parse(text) : null;

            // ResponseDto 형태: { status, code, message, data }
            if (json && typeof json === 'object') {
                if (typeof json.message === 'string' && json.message.trim()) return json.message;

                // 혹시 다른 형태가 섞여있을 때 대비
                if (json.error && typeof json.error.message === 'string') return json.error.message;
                if (json.data && typeof json.data.message === 'string') return json.data.message;
            }
        } catch (_) {
            // JSON 아니면 무시하고 텍스트 사용
        }

        // JSON 아니면 텍스트 그대로(너무 길면 잘라내기)
        const trimmed = (text || '').trim();
        if (trimmed) return trimmed.length > 200 ? trimmed.slice(0, 200) + '…' : trimmed;

        // 진짜 아무것도 없으면 상태코드 기반 기본 메시지
        if (res.status === 401) return '로그인이 필요합니다.';
        if (res.status === 403) return '권한이 없습니다.';
        return `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`;
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
                const msg = await extractErrorMessage(res);
                throw new Error(msg);
            }

            const json = await unwrapJson(res);
            const pg = normalizePage(json);

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
            const msg = await extractErrorMessage(delRes);
            throw new Error(msg); // ✅ message만
        }
        return true;
    }

    btnDeleteSelected?.addEventListener('click', async () => {
        if (!PERM.canBulkDelete || isEmployee()) {
            alert('권한이 없습니다.');
            return;
        }
        if (selected.size === 0) return;

        const ids = Array.from(selected);

        try {
            btnDeleteSelected.disabled = true;

            const ok = await runBulkDeleteRejectedStyle(ids);
            if (!ok) return;

            selected.clear();
            await load();

            Swal.fire({
                icon: 'success',
                title: '완료',
                text: '선택 삭제가 완료되었습니다.',
                confirmButtonColor: '#339af0'
            });
        } catch (err) {
            console.error(err);
            Swal.fire({
                icon: 'error',
                title: '삭제 실패',
                text: err?.message || String(err),
                confirmButtonColor: '#339af0'
            });
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

    // list dirty refresh (팝업에서 수정/삭제 후)
    window.addEventListener('storage', (e) => {
        if (e.key === 'list:dirty' && e.newValue === 'true') {
            try { localStorage.removeItem('list:dirty'); } catch (_) {}
            load();
        }
    });

    function consumeDirtyAndReload() {
        try {
            if (localStorage.getItem('list:dirty') === 'true') {
                localStorage.removeItem('list:dirty');
                load();
                return true;
            }
        } catch (_) {}
        return false;
    }

    window.addEventListener('focus', consumeDirtyAndReload);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) consumeDirtyAndReload();
    });

    // init
    (function init() {
        updateBulkDeleteUI();
        updateHeaderLabel();
        load();
    })();
})();