// /js/document-form/pending-list.js
(() => {
    const API_BASE = "/api/v1/forms";
    const VIEW_BASE = "/form";

    const PAGE_SIZE = 10;

    const elTbody = document.getElementById("tbody");
    const elPager = document.getElementById("pager");
    const elInfo = document.getElementById("pageInfo");
    const elCountPill = document.getElementById("countPill");
    const elMode = document.getElementById("statMode");
    const elToast = document.getElementById("toast"); // layout에 있으면 사용

    if (!elTbody || !elPager) {
        console.warn("[pending-list] required DOM missing, script aborted");
        return;
    }

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    // perms from server (html data-*)
    function readPerms() {
        const d = document.documentElement?.dataset || {};
        const b = (v) => String(v ?? "").trim().toLowerCase() === "true";
        return {
            canApproveForm: b(d.canApproveForm),
            canRejectForm: b(d.canRejectForm),
            canViewReason: b(d.canViewReason),
        };
    }
    const PERM = readPerms();

    // EMPLOYEE가 잘못 들어오면 프론트에서도 방어
    function isEmployeeByClass() {
        const cls = document.documentElement.classList;
        return cls.contains("role-employee") || cls.contains("role-EMPLOYEE");
    }
    if (isEmployeeByClass()) {
        alert("권한이 없습니다. (승인 대기 목록은 관리자만 접근 가능합니다.)");
        location.replace("/form/forms");
        return;
    }

    // ui helpers
    function toast(msg) {
        if (!elToast) return;
        elToast.textContent = msg;
        elToast.classList.add("show");
        clearTimeout(toast._t);
        toast._t = setTimeout(() => elToast.classList.remove("show"), 1400);
    }

    function esc(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function getWriterName(writer) {
        if (!writer) return "-";
        if (typeof writer === "string") return writer;
        return (
            writer.empName ??
            writer.name ??
            writer.username ??
            writer.empNm ??
            writer.emp_id ??
            writer.empId ??
            "-"
        );
    }

    function getRejectReason(r) {
        return (
            r?.rejectReason ??
            r?.rejReason ??
            r?.reject_reason ??
            r?.rej_reason ??
            r?.reason ??
            r?.rejectMsg ??
            r?.reject_message ??
            null
        );
    }

    function getStatPill(stat) {
        const s = String(stat ?? "").trim() || "-";
        const map = {
            P: { cls: "P", label: "대기" },
            W: { cls: "W", label: "삭제대기" },
            R: { cls: "R", label: "반려" },
            X: { cls: "X", label: "삭제반려" },
            A: { cls: "A", label: "승인" },
        };
        const it = map[s] || { cls: "", label: s };
        return `<span class="statPill ${esc(it.cls)}">${esc(it.label)}</span>`;
    }

    function isResponseDto(obj) {
        return obj && typeof obj === 'object' && ('data' in obj) && (('status' in obj) || ('message' in obj));
    }

    async function unwrapJson(res) {
        const body = await res.json().catch(() => null);
        if (!body) return null;
        return isResponseDto(body) ? body.data : body;
    }

    function markFormListDirty() {
        try { localStorage.setItem("list:dirty", "true"); } catch (_) {}
    }

    // fetch (401 refresh 재시도)
    async function refreshAccessTokenIfPossible() {
        const res = await fetch("/auth/refresh", {
            method: "POST",
            credentials: "same-origin",
            headers: { Accept: "application/json" },
        });
        return res.ok;
    }

    async function apiFetch(url, options = {}, _retried = false) {
        const headers = new Headers(options.headers || {});
        if (!headers.has("Accept")) headers.set("Accept", "application/json");

        const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
        if (options.body && !isFormData && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        const res = await fetch(url, { ...options, headers, credentials: "same-origin" });

        if (res.status === 401 && !_retried) {
            const ok = await refreshAccessTokenIfPossible().catch(() => false);
            if (ok) return apiFetch(url, options, true);
        }
        return res;
    }

    // api
    async function updateStatus(docfoNo, docfoStat, rejectReason) {
        const payload = { docfoStat };
        if (rejectReason != null && String(rejectReason).trim() !== "") {
            payload.rejectReason = String(rejectReason).trim();
        }

        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/status`, {
            method: "PATCH",
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const t = await res.text().catch(() => "");
            throw new Error(`상태 변경 실패(docfoNo=${docfoNo}) HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function approveDelete(docfoNo) {
        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-approve`, { method: "PATCH" });
        if (!res.ok) {
            const t = await res.text().catch(() => "");
            throw new Error(`삭제 승인 실패 HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function rejectDelete(docfoNo, rejectReason) {
        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-reject`, {
            method: "PATCH",
            body: JSON.stringify({ rejectReason: String(rejectReason).trim() }),
        });
        if (!res.ok) {
            const t = await res.text().catch(() => "");
            throw new Error(`삭제 반려 실패 HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function showRejectReason(docfoNo) {
        if (!PERM.canViewReason) {
            alert("사유 조회 권한이 없습니다.");
            return;
        }

        try {
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, { method: "GET" });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const detail = await unwrapJson(res);
            const reason = getRejectReason(detail) ?? "사유가 저장되어 있지 않아요.";
            alert(`사유:\n${reason}`);
        } catch (e) {
            alert("사유 조회 실패: " + (e?.message || e));
        }
    }

    function openDetail(docfoNo) {
        if (!docfoNo) return;
        window.open(
            `${VIEW_BASE}/${encodeURIComponent(docfoNo)}`,
            "_blank",
            "width=1100,height=820,resizable=yes,scrollbars=yes"
        );
    }

    function askReason(title) {
        const reason = window.prompt(title, "");
        if (reason == null) return null; // 취소
        const trimmed = String(reason).trim();
        if (!trimmed) return ""; // 빈 입력
        return trimmed;
    }

    // mode -> stats csv (서버 csv 지원 전제)
    function getStatsCsvByMode(mode) {
        if (mode === "P") return "P,W";
        if (mode === "R") return "R,X";
        return "P,W,R,X";
    }

    function buildUrl() {
        const mode = elMode?.value || "ALL";
        const statCsv = getStatsCsvByMode(mode);

        const params = new URLSearchParams();
        params.set("stat", statCsv);
        params.set("page", String(page));
        params.set("size", String(PAGE_SIZE));

        return `${API_BASE}?${params.toString()}`;
    }

    // render
    function render(rows) {
        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * PAGE_SIZE;

        elTbody.innerHTML = rows
            .map((r, idx) => {
                const docfoNo = r.docfoNo ?? r.id ?? r.docfo_no;
                const docfoName = r.docfoName ?? r.name ?? r.docfo_name ?? "-";
                const stat = r.docfoStat ?? r.docfo_stat ?? "-";

                const writerObj = r.writer ?? r.employee ?? r.writerInfo ?? null;
                const writerName = getWriterName(writerObj);

                const idStr = String(docfoNo ?? "");
                const rowNo = startNo + idx + 1;

                const reasonInline = getRejectReason(r);
                const isPendingGroup = stat === "P" || stat === "W";
                const isRejectedGroup = stat === "R" || stat === "X";

                let actionHtml = "";

                if (isPendingGroup) {
                    if (stat === "P" && PERM.canApproveForm && PERM.canRejectForm) {
                        actionHtml = `
              <button class="btn sm ok" data-action="approve" data-id="${esc(idStr)}">승인</button>
              <button class="btn sm danger" data-action="reject" data-id="${esc(idStr)}">반려</button>
            `;
                    } else if (stat === "W" && PERM.canApproveForm && PERM.canRejectForm) {
                        actionHtml = `
              <button class="btn sm ok" data-action="delApprove" data-id="${esc(idStr)}">삭제승인</button>
              <button class="btn sm danger" data-action="delReject" data-id="${esc(idStr)}">삭제반려</button>
            `;
                    } else {
                        actionHtml = `<span class="muted">-</span>`;
                    }
                } else if (isRejectedGroup) {
                    actionHtml =
                        (PERM.canViewReason && reasonInline)
                            ? `<button class="btn sm warn btn-reason" data-action="reason" data-id="${esc(idStr)}">사유</button>`
                            : `<span class="muted">-</span>`;
                } else {
                    actionHtml = `<span class="muted">-</span>`;
                }

                return `
          <tr>
            <td>${rowNo}</td>
            <td class="col-title">
              <a class="titleLink" href="javascript:void(0)" data-action="detail" data-id="${esc(idStr)}">
                ${esc(docfoName)}
              </a>
            </td>
            <td>${esc(writerName)}</td>
            <td>${getStatPill(stat)}</td>
            <td style="text-align:center;">
              <div class="row-actions">
                ${actionHtml}
              </div>
            </td>
          </tr>
        `;
            })
            .join("");
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

        const createBtn = (label, target, active = false, disabled = false) => {
            const b = document.createElement("button");
            b.className = `pageBtn ${active ? "active" : ""}`;
            b.textContent = label;
            b.disabled = disabled;
            b.type = "button";
            b.onclick = () => {
                page = target;
                load();
            };
            return b;
        };

        elPager.appendChild(createBtn("<", cur - 1, false, cur <= 0));

        for (let i = startPage; i <= endPage; i++) {
            // disabled는 false
            elPager.appendChild(createBtn(String(i + 1), i, i === cur, false));
        }

        elPager.appendChild(createBtn(">", cur + 1, false, cur >= tp - 1));
    }

    // load (서버 페이징)
    async function load() {
        elTbody.innerHTML = `<tr><td colspan="5" class="muted">로딩 중...</td></tr>`;

        try {
            const res = await apiFetch(buildUrl(), { method: "GET" });

            if (res.status === 401) {
                elTbody.innerHTML = `<tr><td colspan="5" class="muted">로그인이 만료되었습니다. 다시 로그인 해주세요.</td></tr>`;
                return;
            }
            if (res.status === 403) {
                elTbody.innerHTML = `<tr><td colspan="5" class="muted">권한이 없습니다.</td></tr>`;
                return;
            }

            if (!res.ok) {
                const t = await res.text().catch(() => "");
                throw new Error(`HTTP ${res.status} ${t}`);
            }

            const json = await res.json();
            const items = Array.isArray(json?.content)
                ? json.content
                : Array.isArray(json?.data?.content)
                    ? json.data.content
                    : [];

            const pg = json?.content
                ? {
                    page: json.number ?? 0,
                    totalPages: json.totalPages ?? 1,
                    totalElements: json.totalElements ?? items.length,
                }
                : json?.data?.content
                    ? {
                        page: json.data.number ?? 0,
                        totalPages: json.data.totalPages ?? 1,
                        totalElements: json.data.totalElements ?? items.length,
                    }
                    : { page: 0, totalPages: 1, totalElements: items.length };

            page = pg.page ?? page;
            totalPages = pg.totalPages ?? 1;
            totalElements = pg.totalElements ?? 0;

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${Math.max(totalPages, 1)}`;
            if (elCountPill) elCountPill.textContent = `${totalElements}건`;

            renderPager();
            render(items);
        } catch (err) {
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            totalPages = 1;
            totalElements = 0;
            page = 0;
            if (elInfo) elInfo.textContent = "page 1 / 1";
            if (elCountPill) elCountPill.textContent = "0건";
            renderPager();
        }
    }

    // events
    elMode?.addEventListener("change", () => {
        page = 0;
        load();
    });

    elTbody.addEventListener("click", async (e) => {
        const t = e.target;
        if (!(t instanceof HTMLElement)) return;

        const action = t.getAttribute("data-action");
        const id = t.getAttribute("data-id");

        if (action === "detail" && id) {
            openDetail(id);
            return;
        }
        if (!id) return;

        try {
            if (action === "reason") {
                await showRejectReason(id);
                return;
            }

            if (action === "approve") {
                if (!PERM.canApproveForm) return;
                await updateStatus(id, "A");
                toast("승인 처리 완료");
                markFormListDirty();
                await load();
                return;
            }

            if (action === "reject") {
                if (!PERM.canRejectForm) return;
                const reason = askReason("반려 사유를 입력하세요");
                if (reason == null) return;
                if (!reason) {
                    alert("반려 사유를 입력해주세요.");
                    return;
                }
                await updateStatus(id, "R", reason);
                toast("반려 처리 완료");
                markFormListDirty();
                await load();
                return;
            }

            if (action === "delApprove") {
                if (!PERM.canApproveForm) return;
                if (!confirm("삭제 요청을 승인하시겠습니까?")) return;
                await approveDelete(id);
                toast("삭제 승인 완료");
                markFormListDirty();
                await load();
                return;
            }

            if (action === "delReject") {
                if (!PERM.canRejectForm) return;
                const reason = askReason("삭제 반려 사유를 입력하세요");
                if (reason == null) return;
                if (!reason) {
                    alert("삭제 반려 사유를 입력해주세요.");
                    return;
                }
                await rejectDelete(id, reason);
                toast("삭제 반려 완료");
                markFormListDirty();
                await load();
                return;
            }
        } catch (err) {
            console.error(err);
            alert("처리 실패: " + (err?.message || err));
        }
    });

    function consumeDirtyAndReload() {
        try {
            if (localStorage.getItem('list:dirty') === 'true') {
                localStorage.removeItem('list:dirty');
                load();
            }
        } catch (_) {}
    }

    window.addEventListener('focus', consumeDirtyAndReload);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) consumeDirtyAndReload();
    });

    load();
})();