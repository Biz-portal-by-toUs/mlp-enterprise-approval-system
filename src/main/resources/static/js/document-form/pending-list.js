(() => {
    const API_BASE = '/api/v1/forms';
    // ViewDocumentFormController 기준: 상세 /form/{docfoNo}
    const VIEW_BASE = '/form';

    const elApiLabel = document.getElementById('apiLabel');
    if (elApiLabel) elApiLabel.textContent = API_BASE;

    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');

    const elSize = document.getElementById('size');
    const elMode = document.getElementById('statMode'); // ALL / P / R (드롭다운 라벨: 전체/대기/반려)
    const elToast = document.getElementById('toast');

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    // Role 판단: JWT 파싱 ❌
    // Thymeleaf에서 <html class="role-thr-admin"> 같은 방식으로 주입했다고 가정
    function isThrAdmin() {
        return document.documentElement.classList.contains('role-thr-admin');
    }

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
            .replaceAll("'", "&#39;");
    }

    function markFormListDirty() {
        try {
            localStorage.setItem('list:dirty', 'true');
        } catch (_) {
        }
    }

    const _fetch = window.fetch.bind(window);

    // 쿠키 기반 인증 (HttpOnly)
    // - Authorization 헤더로 토큰 넣지 않음
    // - credentials: 'same-origin' 으로 쿠키 자동 전송
    async function apiFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) headers.set('Accept', 'application/json');

        const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
        if (options.body && !isFormData && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        return _fetch(url, {
            ...options,
            headers,
            credentials: 'same-origin',
        });
    }

    function openDetail(docfoNo) {
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(`${VIEW_BASE}/${encodeURIComponent(id)}`, '_blank',
            'width=1100,height=820,resizable=yes,scrollbars=yes'
        );
    }

    function normalizePage(data) {
        if (data && Array.isArray(data.content)) {
            return {
                items: data.content,
                page: data.number ?? 0,
                size: data.size ?? Number(elSize?.value || 15),
                totalPages: data.totalPages ?? 1,
                totalElements: data.totalElements ?? data.content.length
            };
        }
        return {items: [], page: 0, size: Number(elSize?.value || 15), totalPages: 1, totalElements: 0};
    }

    function buildUrl(stat, pageNo, sizeNo) {
        const params = new URLSearchParams();
        params.set('page', String(pageNo));
        params.set('size', String(sizeNo));
        params.set('stat', stat);
        return `${API_BASE}?${params.toString()}`;
    }

    function getWriterName(writer) {
        if (!writer) return '-';
        if (typeof writer === 'string') return writer;
        return writer.empName ?? writer.name ?? writer.username ?? writer.empNm ?? writer.emp_id ?? writer.empId ?? '-';
    }

    function getRejectReason(r) {
        return r?.rejectReason
            ?? r?.rejReason
            ?? r?.reject_reason
            ?? r?.rej_reason
            ?? r?.reason
            ?? r?.rejectMsg
            ?? r?.reject_message
            ?? null;
    }

    // 상태 라벨: 코드 제거 + W/X 추가
    function getStatPill(stat) {
        const s = String(stat ?? '').trim() || '-';

        const map = {
            P: {cls: 'P', label: '대기'},
            R: {cls: 'R', label: '반려'},
            A: {cls: 'A', label: '승인'},
            W: {cls: 'W', label: '삭제대기'},
            X: {cls: 'X', label: '삭제반려'},
            D: {cls: 'D', label: '삭제'},
            T: {cls: 'T', label: '임시저장'},
        };

        const it = map[s] || {cls: '', label: s};
        return `<span class="statPill ${esc(it.cls)}">${esc(it.label)}</span>`;
    }

    async function showRejectReason(docfoNo) {
        try {
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {method: 'GET'});
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            const reason = getRejectReason(data) ?? '사유가 저장되어 있지 않아요.';
            alert(`사유:\n${reason}`);
        } catch (e) {
            alert('사유 조회 실패: ' + (e?.message || e));
        }
    }

    // mode에 맞춰 상태 묶음 반환
    function getStatsByMode(mode) {
        if (mode === 'P') return ['P', 'W'];
        if (mode === 'R') return ['R', 'X'];
        return ['P', 'W', 'R', 'X'];
    }

    function hasNonBlank(v) {
        return v != null && String(v).trim() !== '';
    }

    function render(rows) {
        if (!elTbody) return;

        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * Number(elSize?.value || 15);
        const thr = isThrAdmin();

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';
            const stat = r.docfoStat ?? r.docfo_stat ?? '-';

            const writerObj = r.writer ?? r.employee ?? r.writerInfo ?? null;
            const writerName = getWriterName(writerObj);

            const idStr = String(docfoNo ?? '');
            const rowNo = startNo + idx + 1;

            const reasonInline = getRejectReason(r);
            const isRejectedGroup = (stat === 'R' || stat === 'X');

            // 처리 버튼 정책
            let actionHtml = '';

            if (thr) {
                actionHtml = hasNonBlank(reasonInline)
                    ? `<button class="btn sm warn" onclick="window.showRejectReason('${esc(idStr)}')">사유</button>`
                    : `<span class="muted">-</span>`;
            } else {
                if (stat === 'P') {
                    actionHtml = `
                        <button class="btn sm ok" onclick="window.approveOne('${esc(idStr)}')">승인</button>
                        <button class="btn sm danger" onclick="window.rejectOne('${esc(idStr)}')">반려</button>
                    `;
                } else if (stat === 'W') {
                    actionHtml = `
                        <button class="btn sm ok" onclick="window.approveDeleteOne('${esc(idStr)}')">삭제승인</button>
                        <button class="btn sm danger" onclick="window.rejectDeleteOne('${esc(idStr)}')">삭제반려</button>
                    `;
                } else if (isRejectedGroup) {
                    actionHtml = hasNonBlank(reasonInline)
                        ? `<button class="btn sm warn" onclick="window.showRejectReason('${esc(idStr)}')">사유</button>`
                        : `<span class="muted">-</span>`;
                } else {
                    actionHtml = `<span class="muted">-</span>`;
                }
            }

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
              ${actionHtml}
            </div>
          </td>
        </tr>
      `;
        }).join('');
    }

    function renderPager() {
        if (!elPager) return;

        const tp = Math.max(1, Number(totalPages) || 1);
        const p = Math.min(Math.max(0, Number(page) || 0), tp - 1);

        if (tp <= 1) {
            elPager.innerHTML = '';
            return;
        }

        let start = p - 2;
        let end = p + 2;

        if (start < 0) {
            end += (0 - start);
            start = 0;
        }
        if (end > tp - 1) {
            start -= (end - (tp - 1));
            end = tp - 1;
        }
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
            btn('&raquo;', disableNextLast, 'last')
        ].join('');
    }

    async function load() {
        if (!elTbody) return;
        elTbody.innerHTML = `<tr><td colspan="5" class="muted">로딩 중...</td></tr>`;

        try {
            const sizeNo = Number(elSize?.value || 15);
            const mode = elMode?.value || 'ALL'; // ALL / P / R

            // 항상 "묶음"을 BIG로 가져와서 클라 페이징
            const BIG = 1000;
            const stats = getStatsByMode(mode);

            const results = await Promise.all(
                stats.map(s => apiFetch(buildUrl(s, 0, BIG), {method: 'GET'}).then(async (res) => {
                    if (!res.ok) {
                        const t = await res.text().catch(() => '');
                        throw new Error(`${s} 조회 실패 HTTP ${res.status} ${t}`);
                    }
                    return normalizePage(await res.json()).items;
                }))
            );

            // 합치고 docfoNo 기준 중복 제거 + 정렬
            const mergedMap = new Map();
            results.flat().forEach(it => {
                const key = String(it.docfoNo ?? it.id ?? it.docfo_no ?? '');
                if (!key) return;
                mergedMap.set(key, it);
            });

            const merged = Array.from(mergedMap.values()).sort((a, b) => {
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

        } catch (err) {
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            renderPager();
        }
    }

    // 승인/반려 처리 (P만 가능)
    async function updateStatus(docfoNo, docfoStat, rejectReason) {
        const payload = {docfoStat};
        if (rejectReason != null && String(rejectReason).trim() !== '') {
            payload.rejectReason = String(rejectReason).trim();
        }

        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: 'PATCH',
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const t = await res.text().catch(() => '');
            throw new Error(`상태 변경 실패(docfoNo=${docfoNo}) HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function approveOne(docfoNo) {
        if (!docfoNo) return;
        try {
            await updateStatus(docfoNo, 'A');
            toast('승인 처리 완료');
            markFormListDirty();
            await load();
        } catch (err) {
            console.error(err);
            alert('승인 실패: ' + (err?.message || err));
        }
    }

    async function rejectOne(docfoNo) {
        if (!docfoNo) return;

        const reason = prompt('반려 사유(필수):', '');
        if (reason === null) return; // 취소
        if (String(reason).trim() === '') {
            alert('반려 사유를 입력해주세요.');
            return;
        }

        try {
            await updateStatus(docfoNo, 'R', reason);
            toast('반려 처리 완료');
            markFormListDirty();
            await load();
        } catch (err) {
            console.error(err);
            alert('반려 실패: ' + (err?.message || err));
        }
    }

    async function approveDeleteOne(docfoNo) {
        if (!docfoNo) return;
        if (!confirm('삭제 요청을 승인하시겠습니까?')) return;

        try {
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-approve`, {
                method: 'PATCH',
            });

            if (!res.ok) {
                const t = await res.text().catch(() => '');
                throw new Error(`삭제 승인 실패 HTTP ${res.status} ${t}`);
            }

            toast('삭제 승인 완료');
            markFormListDirty();
            await load();
        } catch (err) {
            console.error(err);
            alert('삭제 승인 실패: ' + (err?.message || err));
        }
    }

    async function rejectDeleteOne(docfoNo) {
        if (!docfoNo) return;

        const reason = prompt('삭제 반려 사유(필수):', '');
        if (reason === null) return;
        if (String(reason).trim() === '') {
            alert('삭제 반려 사유를 입력해주세요.');
            return;
        }

        try {
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-reject`, {
                method: 'PATCH',
                body: JSON.stringify({ rejectReason: String(reason).trim() }),
            });

            if (!res.ok) {
                const t = await res.text().catch(() => '');
                throw new Error(`삭제 반려 실패 HTTP ${res.status} ${t}`);
            }

            toast('삭제 반려 완료');
            markFormListDirty();
            await load();
        } catch (err) {
            console.error(err);
            alert('삭제 반려 실패: ' + (err?.message || err));
        }
    }

    // 전역 노출(HTML onclick에서 사용)
    window.openDetail = openDetail;
    window.showRejectReason = showRejectReason;
    window.approveOne = approveOne;
    window.rejectOne = rejectOne;
    window.approveDeleteOne = approveDeleteOne;
    window.rejectDeleteOne = rejectDeleteOne;

    // pager click
    elPager?.addEventListener('click', (e) => {
        const t = e.target;
        if (!(t instanceof HTMLElement)) return;

        const pageAttr = t.getAttribute('data-page');
        if (pageAttr != null) {
            const nextPage = Number(pageAttr);
            if (Number.isFinite(nextPage)) {
                page = nextPage;
                load();
            }
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

    // controls
    document.getElementById('btnRefresh')?.addEventListener('click', () => load());
    elSize?.addEventListener('change', () => {
        page = 0;
        load();
    });
    elMode?.addEventListener('change', () => {
        page = 0;
        load();
    });

    // storage listener
    window.addEventListener('storage', (e) => {
        if (e.key === 'list:dirty' && e.newValue === 'true') {
            consumeDirtyAndReload();
        }
    });

    function consumeDirtyAndReload() {
        try {
            const v = localStorage.getItem('list:dirty');
            if (v === 'true') {
                localStorage.removeItem('list:dirty');
                load();
            }
        } catch (_) {
        }
    }

    window.addEventListener('focus', consumeDirtyAndReload);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) consumeDirtyAndReload();
    });

    load();
})();