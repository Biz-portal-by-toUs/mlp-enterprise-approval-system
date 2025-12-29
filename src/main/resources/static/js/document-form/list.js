(() => {
    // =======================
    // ViewDocumentFormController 기준
    //  - 목록: /form/forms
    //  - 상세: /form/{docfoNo}
    //  - 생성: /form/new
    //  - 수정: /form/{docfoNo}/edit (상세에서 이동)
    // =======================
    const API_BASE  = '/api/v1/forms';
    const VIEW_BASE = '/form';

    // "목록은 승인(A)만" 정책이면 true
    const ONLY_APPROVED = true;

    // -----------------------
    // ROLE (JWT payload) - 필요 시 생성/삭제 UI 제한
    // -----------------------
    function parseJwtPayload(token){
        try{
            const t = token.replace(/^Bearer\s+/i,'');
            const base64 = t.split('.')[1];
            if(!base64) return null;
            const json = decodeURIComponent(atob(base64.replace(/-/g,'+').replace(/_/g,'/'))
                .split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
            return JSON.parse(json);
        }catch(_){ return null; }
    }
    function getUserRole(){
        const token = (localStorage.getItem('accessToken') || '').trim();
        const p = parseJwtPayload(token) || {};
        const role =
            p.role ||
            p.auth ||
            (Array.isArray(p.authorities) ? p.authorities[0] : null) ||
            (Array.isArray(p.roles) ? p.roles[0] : null) ||
            '';
        return String(role).replace(/^ROLE_/,''); // ROLE_EMPLOYEE -> EMPLOYEE
    }
    const ROLE = getUserRole();

    const elApiLabel = document.getElementById('apiLabel');
    if (elApiLabel) elApiLabel.textContent = API_BASE;

    const elTbody   = document.getElementById('tbody');
    const elInfo    = document.getElementById('pageInfo');
    const elPager   = document.getElementById('pagerControls');

    const elQ       = document.getElementById('q');
    const elSize    = document.getElementById('size');

    const elBulkDel  = document.getElementById('btnBulkDelete');
    const elToast    = document.getElementById('toast');
    const btnCreate  = document.getElementById('btnCreate');
    const btnRefresh = document.getElementById('btnRefresh');
    const btnSearch  = document.getElementById('btnSearch');

    const selected = new Set();

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function toast(msg){
        if (!elToast) return;
        elToast.textContent = msg;
        elToast.classList.add('show');
        window.clearTimeout(toast._t);
        toast._t = window.setTimeout(() => elToast.classList.remove('show'), 1400);
    }

    function esc(s){
        return String(s ?? '')
            .replaceAll('&','&amp;')
            .replaceAll('<','&lt;')
            .replaceAll('>','&gt;')
            .replaceAll('"','&quot;')
            .replaceAll("'",'&#39;');
    }

    function markFormListDirty(){
        try { localStorage.setItem('list:dirty', 'true'); } catch (_) {}
    }

    const _fetch = window.fetch.bind(window);

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
        return _fetch(url, { ...options, headers, credentials: 'same-origin' });
    }

    function openDetail(docfoNo){
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(`${VIEW_BASE}/${encodeURIComponent(id)}`, '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    }

    function updateBulkDeleteUI(){
        if (!elBulkDel) return;
        if (selected.size > 0) {
            elBulkDel.style.display = '';
            elBulkDel.textContent = `선택 삭제 (${selected.size})`;
        } else {
            elBulkDel.style.display = 'none';
            elBulkDel.textContent = '선택 삭제';
        }
    }

    function normalizePage(data){
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize?.value || 15),
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length
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
                totalElements: data.data.totalElements ?? data.data.content.length
            };
        }
        return { items: [], page: 0, size: Number(elSize?.value || 15), totalPages: 1, totalElements: 0 };
    }

    function buildUrl(){
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize?.value || 15)));

        const q = (elQ?.value || '').trim();
        if (q) {
            // 백엔드가 뭐로 받든 최대한 맞춰주기
            params.set('q', q);
            params.set('docfoName', q);
            params.set('keyword', q);
        }

        // ✅ "승인(A)만" 고정이면 안전하게 stat 관련 파라미터를 같이 보냄
        if (ONLY_APPROVED) {
            params.set('stat', 'A');
            params.set('docfoStat', 'A');
        }

        return `${API_BASE}?${params.toString()}`;
    }

    function render(rows){
        if (!elTbody) return;

        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * (Number(elSize?.value || 15));

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo   = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';

            const idStr = String(docfoNo ?? '');
            const checked = selected.has(idStr) ? 'checked' : '';
            const rowNo = startNo + idx + 1;

            const disableDeleteUi = (ROLE === 'EMPLOYEE');

            return `
        <tr>
          <td>
            <label style="display:flex; gap:10px; align-items:center;">
              <input type="checkbox"
                     data-role="rowCheck"
                     data-id="${esc(idStr)}"
                     ${checked}
                     ${disableDeleteUi ? 'disabled' : ''}/>
              <span>${rowNo}</span>
            </label>
          </td>
          <td>
            <a class="titleLink" href="javascript:void(0)" onclick="window.openDetail('${esc(idStr)}')">
              ${esc(docfoName)}
            </a>
          </td>
        </tr>
      `;
        }).join('');

        updateBulkDeleteUI();
    }

    function renderPager(){
        if (!elPager) return;

        const tp = Math.max(1, Number(totalPages) || 1);
        const p  = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        if (tp <= 1) { elPager.innerHTML = ''; return; }

        let start = p - 2;
        let end   = p + 2;

        if (start < 0) { end += (0 - start); start = 0; }
        if (end > tp - 1) { start -= (end - (tp - 1)); end = tp - 1; }
        start = Math.max(0, start);

        const nums = [];
        for (let i = start; i <= end; i++) nums.push(i);

        const disableFirstPrev = (p <= 0);
        const disableNextLast  = (p >= tp - 1);

        const btn = (label, disabled, go, extra='') =>
            `<button class="btn ${extra}" ${disabled ? 'disabled' : ''} data-go="${go}">${label}</button>`;

        const numBtn = (i) => {
            const active = i === p ? 'active' : '';
            return `<button class="btn num ${active}" ${i===p?'disabled':''} data-page="${i}">${i+1}</button>`;
        };

        elPager.innerHTML = [
            btn('&laquo;', disableFirstPrev, 'first'),
            btn('&lsaquo;', disableFirstPrev, 'prev'),
            ...nums.map(numBtn),
            btn('&rsaquo;', disableNextLast, 'next'),
            btn('&raquo;', disableNextLast, 'last')
        ].join('');
    }

    async function load(){
        if (!elTbody) return;
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try{
            const url = buildUrl();
            const res = await apiFetch(url, { method:'GET' });

            if (!res.ok){
                const t = await res.text().catch(()=> '');
                throw new Error(`HTTP ${res.status} ${t}`);
            }

            const json = await res.json();
            const pg = normalizePage(json);

            totalPages = pg.totalPages ?? 1;
            totalElements = pg.totalElements ?? 0;
            page = pg.page ?? page;

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${totalPages}  ·  total ${totalElements}`;

            renderPager();
            render(pg.items);

        }catch(err){
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            renderPager();
        }
    }

    // -------- checkbox select
    elTbody?.addEventListener('change', (e) => {
        const cb = e.target;
        if(!(cb instanceof HTMLInputElement)) return;
        if(cb.dataset.role !== 'rowCheck') return;

        const id = cb.dataset.id;
        if(!id) return;

        if(cb.checked) selected.add(id);
        else selected.delete(id);

        updateBulkDeleteUI();
    });

    // -------- pager click
    elPager?.addEventListener('click', (e) => {
        const t = e.target;
        if(!(t instanceof HTMLElement)) return;

        const pageAttr = t.getAttribute('data-page');
        if(pageAttr != null){
            const nextPage = Number(pageAttr);
            if(Number.isFinite(nextPage)){ page = nextPage; load(); }
            return;
        }

        const go = t.getAttribute('data-go');
        if(!go) return;

        if(go === 'first') page = 0;
        else if(go === 'prev') page = Math.max(0, page - 1);
        else if(go === 'next') page = Math.min(totalPages - 1, page + 1);
        else if(go === 'last') page = Math.max(0, totalPages - 1);

        load();
    });

    // =======================
    // 삭제 처리:
    // 1) PATCH /api/v1/forms/{id}/status  body: { docfoStat:"D" }
    // 2) (fallback) DELETE /api/v1/forms/{id}
    // =======================
    async function softDelete(docfoNo){
        // 1) PATCH status(D)
        const patchRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ docfoStat: 'D' }),
        });

        if (patchRes.ok) return true;

        // 405/404 등일 수 있으니 fallback
        const delRes = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: 'DELETE' });
        if (!delRes.ok) {
            const t = await delRes.text().catch(()=> '');
            throw new Error(`삭제 실패(docfoNo=${docfoNo}) HTTP ${delRes.status} ${t}`);
        }
        return true;
    }

    // -------- bulk delete (Employee면 막기)
    elBulkDel?.addEventListener('click', async () => {
        if (ROLE === 'EMPLOYEE') {
            toast('권한이 없습니다.');
            return;
        }
        if(selected.size === 0) return;

        const ids = Array.from(selected);
        if(!confirm(`선택한 ${ids.length}개를 삭제 처리할까요? (stat=D)`)) return;

        try{
            elBulkDel.disabled = true;

            for(const docfoNo of ids){
                await softDelete(docfoNo);
            }

            toast('선택 삭제 완료');
            selected.clear();
            updateBulkDeleteUI();
            markFormListDirty();
            await load();

        }catch(err){
            console.error(err);
            alert('선택 삭제 실패: ' + (err?.message || err));
        }finally{
            elBulkDel.disabled = false;
        }
    });

    // -------- create (Employee면 비활성화)
    btnCreate?.addEventListener('click', () => {
        if (ROLE === 'EMPLOYEE') {
            toast('권한이 없습니다.');
            return;
        }
        window.open(`${VIEW_BASE}/new`, '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    });

    btnRefresh?.addEventListener('click', () => load());
    btnSearch?.addEventListener('click', () => { page = 0; load(); });
    elQ?.addEventListener('keydown', (e) => { if(e.key === 'Enter'){ page = 0; load(); } });
    elSize?.addEventListener('change', () => { page = 0; load(); });

    window.openDetail = openDetail;

    // -------- storage listener (list:dirty)
    window.addEventListener('storage', (e) => {
        if (e.key === 'list:dirty' && e.newValue === 'true') {
            try { localStorage.removeItem('list:dirty'); } catch (_) {}
            load();
        }
    });

    // -------- 초기 권한 기반 UI 처리
    if (ROLE === 'EMPLOYEE') {
        if (btnCreate) btnCreate.disabled = true;
        if (elBulkDel) elBulkDel.style.display = 'none';
    }

    load();
})();