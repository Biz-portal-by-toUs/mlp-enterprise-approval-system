// /js/document-form/list.js
(() => {
    const API_BASE = '/api/v1/forms';
    const VIEW_BASE = '/form';

    const PAGE_SIZE = 10;

    // ✅ 목록에서 보여줄 상태 필터(동작은 기존과 동일)
    //   - true면 DEFAULT_STATS만 조회
    //   - false면 stat 파라미터 없이 조회(백 기본값/전체 정책에 따름)
    const USE_DEFAULT_STATS_FILTER = true;
    const DEFAULT_STATS = ['A', 'X', 'W']; // (기존 APPROVED_STATS 그대로)

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
        const d = document.documentElement?.dataset || {};
        const b = (v) => String(v ?? "").trim().toLowerCase() === "true";

        return {
            isEmployee: b(d.isEmployee),

            canCreate: b(d.canCreate),
            canBulkDelete: b(d.canBulkDelete),

            // (있으면 읽고, 없어도 무관)
            canApprove: b(d.canApprove),
            canUseTemp: b(d.canUseTemp),
        };
    }

    const PERM = readPerms();

    // ✅ employee 판단은 서버 flag / class 기반만 사용 (권한 조합 추정 제거)
    function isEmployeeByClass() {
        const byClass = document.documentElement?.classList?.contains("role-employee") === true;
        return PERM.isEmployee || byClass;
    }

    function isEmployee() {
        return isEmployeeByClass();
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
        const items = ids.map((id) => {
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
                <div style="color:#868e96;">삭제 처리</div>
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
                선택한 문서양식을 삭제 처리합니다.<br/>
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
            allowOutsideClick: () => !Swal.isLoading(),
            allowEscapeKey: () => !Swal.isLoading(),

            preConfirm: async () => {
                Swal.showLoading();

                const results = await batchAllSettled(ids, 5, softDelete);

                const okIds = [];
                const failed = [];

                results.forEach((r, i) => {
                    const id = ids[i];
                    if (r.status === 'fulfilled') okIds.push(id);
                    else failed.push({ id, reason: r.reason });
                });

                // validationMessage는 confirm을 막는 케이스가 있어서,
                // 여기서는 "return 값"으로만 실패를 전달하고 confirm 이후 별도 안내로 처리
                Swal.hideLoading();
                return { okIds, failed };
            }
        });

        if (!result.isConfirmed) return null;
        return result.value || { okIds: [], failed: [] };
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

        try {
            const json = text ? JSON.parse(text) : null;
            if (json && typeof json === 'object') {
                if (typeof json.message === 'string' && json.message.trim()) return json.message;
                if (json.error && typeof json.error.message === 'string') return json.error.message;
                if (json.data && typeof json.data.message === 'string') return json.data.message;
            }
        } catch (_) {}

        const trimmed = (text || '').trim();
        if (trimmed) return trimmed.length > 200 ? trimmed.slice(0, 200) + '…' : trimmed;

        if (res.status === 401) return '로그인이 필요합니다.';
        if (res.status === 403) return '권한이 없습니다.';
        return `요청 처리 중 오류가 발생했습니다. (HTTP ${res.status})`;
    }

    // ===== data load =====
    async function load() {
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try {
            const statsCsv = USE_DEFAULT_STATS_FILTER ? DEFAULT_STATS.join(',') : null;

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
        if (e.target?.closest?.('label.selWrap')) return;

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
        const delRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' });
        if (!delRes.ok) {
            const msg = await extractErrorMessage(delRes);
            throw new Error(msg);
        }
        return true;
    }

    btnDeleteSelected?.addEventListener('click', async () => {
        if (!PERM.canBulkDelete || isEmployee()) {
            alert('권한이 없습니다.');
            return;
        }
        if (selected.size === 0) return;

        try {
            btnDeleteSelected.disabled = true;

            while (true) {
                const ids = Array.from(selected);
                if (ids.length === 0) break;

                const res = await runBulkDeleteRejectedStyle(ids);
                if (!res) break;

                const { okIds, failed } = res;
                const failedIds = failed.map(f => String(f.id));

                selected.clear();
                failedIds.forEach(id => selected.add(id));

                if (okIds.length > 0) {
                    await load();
                } else {
                    updateBulkDeleteUI();
                }

                if (failedIds.length === 0) {
                    Swal.fire({
                        icon: 'success',
                        title: '완료',
                        text: `선택 삭제가 완료되었습니다. (${okIds.length}건)`,
                        confirmButtonColor: '#339af0'
                    });
                    break;
                }

                const failedLines = failed
                    .slice(0, 10)
                    .map(f => `#${f.id}: ${f.reason?.message || f.reason}`)
                    .join('\n');

                const more = failed.length > 10 ? `\n…외 ${failed.length - 10}건` : '';

                const retryPopup = await Swal.fire({
                    icon: 'warning',
                    title: '삭제 실패',
                    html: `
                          <div style="text-align:left; font-size:13px; line-height:1.5; white-space:pre-wrap;">
                            성공: <b>${okIds.length}</b>건<br/>
                            실패: <b>${failedIds.length}</b>건<br/>
                            <hr style="margin:10px 0; border:0; border-top:1px solid #e9ecef;"/>
                            <b>실패 목록</b>\n${esc(failedLines + more)}
                            <div style="margin-top:8px; color:#868e96;">
                              재시도하시겠습니까?
                            </div>
                          </div>
                        `,
                    showCancelButton: true,
                    showDenyButton: true,
                    confirmButtonText: '닫기',
                    denyButtonText: `재시도 (${failedIds.length})`,
                    cancelButtonText: '중단',
                    confirmButtonColor: '#339af0',
                    denyButtonColor: '#fa5252',
                    cancelButtonColor: '#adb5bd',
                    reverseButtons: true
                });

                if (retryPopup.isDenied) {
                    continue;
                }
                break;
            }
        } catch (err) {
            console.error(err);
            Swal.fire({
                icon: 'error',
                title: '삭제 처리 중 오류',
                text: err?.message || String(err),
                confirmButtonColor: '#339af0'
            });
        } finally {
            btnDeleteSelected.disabled = false;
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