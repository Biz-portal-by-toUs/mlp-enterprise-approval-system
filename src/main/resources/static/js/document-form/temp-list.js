(() => {
    const API_BASE = '/api/v1/forms/temp';
    const VIEW_BASE = '/form';

    const elTbody = document.getElementById('tbody');
    const elInfo = document.getElementById('pageInfo');
    const elPager = document.getElementById('pagerControls');
    const elQ = document.getElementById('q');
    const elSize = document.getElementById('size');
    const btnSearch = document.getElementById('btnSearch');
    const elCountPill = document.getElementById('countPill');

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    function esc(s) {
        return String(s ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
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

    function openDetail(docfoNo) {
        if (docfoNo == null) return;
        const id = String(docfoNo);
        window.open(
            `${VIEW_BASE}/${encodeURIComponent(id)}`,
            '_blank',
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

    function buildUrl() {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(Number(elSize?.value || 15)));

        const q = (elQ?.value || '').trim();
        if (q) {
            params.set('q', q);
            params.set('docfoName', q);
            params.set('keyword', q);
        }

        return `${API_BASE}?${params.toString()}`;
    }

    function render(rows) {
        if (!elTbody) return;

        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="2" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const size = Number(elSize?.value || 15);

        elTbody.innerHTML = rows.map((r, idx) => {
            const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
            const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? '-';

            const idStr = String(docfoNo ?? '');
            const rowNo = (page * size) + idx + 1;

            return `
        <tr>
          <td class="col-no">${rowNo}</td>
          <td class="col-title">
            <a class="titleLink" href="javascript:void(0)" onclick="window.openDetail('${esc(idStr)}')">
              ${esc(docfoName)}
            </a>
          </td>
        </tr>
      `;
        }).join('');
    }

    function renderPager() {
        if (!elPager) return;

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
            b.onclick = () => {
                page = target;
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

    async function load() {
        if (!elTbody) return;
        elTbody.innerHTML = `<tr><td colspan="2" class="muted">로딩 중...</td></tr>`;

        try {
            const res = await apiFetch(buildUrl(), { method: 'GET' });
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
            renderPager();
        }
    }

    btnSearch?.addEventListener('click', () => {
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

    window.openDetail = openDetail;

    load();
})();