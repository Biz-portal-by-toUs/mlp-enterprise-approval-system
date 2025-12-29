(() => {
    const API_BASE  = '/api/v1/forms';
    // ✅ ViewDocumentFormController 기준: 상세 /form/{docfoNo}
    const VIEW_BASE = '/form';

    const elApiLabel = document.getElementById('apiLabel');
    if (elApiLabel) elApiLabel.textContent = API_BASE;

    const elTbody = document.getElementById('tbody');
    const elInfo  = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');

    const elSize  = document.getElementById('size');
    const elMode  = document.getElementById('statMode');
    const elToast = document.getElementById('toast');

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
        if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

        if (token) {
            const hasBearer = /^Bearer\s+/i.test(token);
            headers.set('Authorization', hasBearer ? token : `Bearer ${token}`);
        }
        return _fetch(url, { ...options, headers });
    }

    function openDetail(docfoNo){
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(`${VIEW_BASE}/${encodeURIComponent(id)}`, '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
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
        return { items: [], page: 0, size: Number(elSize?.value || 15), totalPages: 1, totalElements: 0 };
    }

    function buildUrl(stat, pageNo, sizeNo){
        const params = new URLSearchParams();
        params.set('page', String(pageNo));
        params.set('size', String(sizeNo));
        params.set('stat', stat);
        return `${API_BASE}?${params.toString()}`;
    }

    function getWriterName(writer){
        if (!writer) return '-';
        if (typeof writer === 'string') return writer;
        return writer.empName ?? writer.name ?? writer.username ?? writer.empNm ?? writer.emp_id ?? writer.empId ?? '-';
    }

    function getRejectReason(r){
        return r?.rejectReason
            ?? r?.rejReason
            ?? r?.reject_reason
            ?? r?.rej_reason
            ?? r?.reason
            ?? r?.rejectMsg
            ?? r?.reject_message
            ?? null;
    }

    function getStatPill(stat){
        const s = String(stat ?? '').trim() || '-';
        const cls = (s === 'P' || s === 'R' || s === 'A') ? s : '';
        const label = (s === 'P') ? '대기(P)' : (s === 'R') ? '반려(R)' : (s === 'A') ? '승인(A)' : s;
        return `<span class="statPill ${cls}">${esc(label)}</span>`;
    }

    async function showRejectReason(docfoNo){
        try{
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method:'GET' });
            if(!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            const reason = getRejectReason(data) ?? '반려 사유가 저장되어 있지 않아요.';
            alert(`반려 사유:\n${reason}`);
        }catch(e){
            alert('반려사유 조회 실패: ' + (e?.message || e));
        }
    }

    function render(rows){
        if(!rows || rows.length === 0){
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * (Number(elSize?.value || 15));

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo   = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            const stat      = r.docfoStat ?? r.docfo_stat ?? '-';

            const writerObj  = r.writer ?? r.employee ?? r.writerInfo ?? null;
            const writerName = getWriterName(writerObj);

            const idStr = String(docfoNo ?? '');
            const rowNo = startNo + idx + 1;

            const isRejected = (stat === 'R');
            const canApprove = (stat === 'P'); // ✅ 대기만 승인 가능
            const canReject  = (stat === 'P'); // ✅ 대기만 반려 가능

            return `
        <tr>
          <td>${rowNo}</td>
          <td>
            <a class="titleLink" href="javascript:void(0)" onclick="window.openDetail('${esc(idStr)}')">
              ${esc(docfoName)}
            </a>
          </td>
          <td>${esc(writerName)}</td>
          <td>${getStatPill(stat)}</td>
          <td style="text-align:right;">
            <div class="row-actions">
              ${
                isRejected
                    ? `<button class="btn sm warn" onclick="window.showRejectReason('${esc(idStr)}')">반려사유</button>`
                    : `
                    <button class="btn sm ok" ${canApprove ? '' : 'disabled'} onclick="window.approveOne('${esc(idStr)}')">승인</button>
                    <button class="btn sm danger" ${canReject ? '' : 'disabled'} onclick="window.rejectOne('${esc(idStr)}')">반려</button>
                  `
            }
            </div>
          </td>
        </tr>
      `;
        }).join('');
    }

    function renderPager(){
        if(!elPager) return;

        const tp = Math.max(1, Number(totalPages) || 1);
        const p  = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        if(tp <= 1){
            elPager.innerHTML = '';
            return;
        }

        let start = p - 2;
        let end   = p + 2;

        if(start < 0){
            end += (0 - start);
            start = 0;
        }
        if(end > tp - 1){
            start -= (end - (tp - 1));
            end = tp - 1;
        }
        start = Math.max(0, start);

        const nums = [];
        for(let i = start; i <= end; i++) nums.push(i);

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
        elTbody.innerHTML = `<tr><td colspan="5" class="muted">로딩 중...</td></tr>`;

        try{
            const sizeNo = Number(elSize?.value || 15);
            const mode = elMode?.value || 'PR'; // P / R / PR

            if(mode === 'P' || mode === 'R'){
                const url = buildUrl(mode, page, sizeNo);
                const res = await apiFetch(url, { method:'GET' });
                if(!res.ok){
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
                return;
            }

            // PR: P와 R을 합쳐서 클라 페이징 (BIG 한번씩)
            const BIG = 1000;
            const [pRes, rRes] = await Promise.all([
                apiFetch(buildUrl('P', 0, BIG), { method:'GET' }),
                apiFetch(buildUrl('R', 0, BIG), { method:'GET' }),
            ]);

            if(!pRes.ok) throw new Error(`P 조회 실패 HTTP ${pRes.status} ${(await pRes.text().catch(()=>''))}`);
            if(!rRes.ok) throw new Error(`R 조회 실패 HTTP ${rRes.status} ${(await rRes.text().catch(()=>''))}`);

            const pJson = normalizePage(await pRes.json());
            const rJson = normalizePage(await rRes.json());

            const mergedMap = new Map();
            [...pJson.items, ...rJson.items].forEach(it => {
                const key = String(it.docfoNo ?? it.id ?? it.docfo_no ?? '');
                if(!key) return;
                mergedMap.set(key, it);
            });

            const merged = Array.from(mergedMap.values()).sort((a,b) => {
                const ax = Number(a.docfoNo ?? a.id ?? a.docfo_no ?? 0);
                const bx = Number(b.docfoNo ?? b.id ?? b.docfo_no ?? 0);
                return ax - bx;
            });

            totalElements = merged.length;
            totalPages = Math.max(1, Math.ceil(totalElements / sizeNo));
            page = Math.min(Math.max(0, page), totalPages - 1);

            const start = page * sizeNo;
            const rows = merged.slice(start, start + sizeNo);

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${totalPages}  ·  total ${totalElements}`;
            renderPager();
            render(rows);

        }catch(err){
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            renderPager();
        }
    }

    // =======================
    // 승인/반려 처리
    // PATCH /api/v1/forms/{docfoNo}/status
    // body: { "docfoStat":"A" } or { "docfoStat":"R", "rejectReason":"..." }
    // =======================

    async function updateStatus(docfoNo, docfoStat, rejectReason){
        const payload = { docfoStat };
        if (rejectReason != null && String(rejectReason).trim() !== '') {
            payload.rejectReason = String(rejectReason).trim();
        }

        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: 'PATCH',
            body: JSON.stringify(payload),
        });

        if(!res.ok){
            const t = await res.text().catch(()=> '');
            throw new Error(`상태 변경 실패(docfoNo=${docfoNo}) HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function approveOne(docfoNo){
        if(!docfoNo) return;
        try{
            await updateStatus(docfoNo, 'A');
            toast('승인 처리 완료');
            markFormListDirty();
            await load();
        }catch(err){
            console.error(err);
            alert('승인 실패: ' + (err?.message || err));
        }
    }

    async function rejectOne(docfoNo){
        if(!docfoNo) return;

        const reason = prompt('반려 사유(필수):', '');
        if(reason === null) return; // 취소
        if(String(reason).trim() === ''){
            alert('반려 사유를 입력해주세요.');
            return;
        }

        try{
            await updateStatus(docfoNo, 'R', reason);
            toast('반려 처리 완료');
            markFormListDirty();
            await load();
        }catch(err){
            console.error(err);
            alert('반려 실패: ' + (err?.message || err));
        }
    }

    // 전역 노출(HTML onclick에서 사용)
    window.openDetail = openDetail;
    window.showRejectReason = showRejectReason;
    window.approveOne = approveOne;
    window.rejectOne = rejectOne;

    // pager click
    elPager?.addEventListener('click', (e) => {
        const t = e.target;
        if(!(t instanceof HTMLElement)) return;

        const pageAttr = t.getAttribute('data-page');
        if(pageAttr != null){
            const nextPage = Number(pageAttr);
            if(Number.isFinite(nextPage)){
                page = nextPage;
                load();
            }
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

    // controls
    document.getElementById('btnRefresh')?.addEventListener('click', () => load());
    elSize?.addEventListener('change', () => { page = 0; load(); });
    elMode?.addEventListener('change', () => { page = 0; load(); });

    // storage listener
    window.addEventListener('storage', (e) => {
        if(e.key === 'list:dirty' && e.newValue === 'true'){
            consumeDirtyAndReload();
        }
    });

    function consumeDirtyAndReload(){
        try{
            const v = localStorage.getItem('list:dirty');
            if (v === 'true') {
                localStorage.removeItem('list:dirty');
                load();
            }
        }catch(_){}
    }

    window.addEventListener('focus', consumeDirtyAndReload);

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) consumeDirtyAndReload();
    });

    load();
})();