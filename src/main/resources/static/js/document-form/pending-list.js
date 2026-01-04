// /js/document-form/pending-list.js
(() => {
    const API_BASE = "/api/v1/forms";
    const VIEW_BASE = "/form";

    const PAGE_SIZE = 10;
    const BIG = 1000;

    const elTbody = document.getElementById("tbody");
    const elPager = document.getElementById("pager");
    const elInfo = document.getElementById("pageInfo");
    const elCountPill = document.getElementById("countPill");
    const elMode = document.getElementById("statMode");
    const elToast = document.getElementById("toast"); // layout에 있으면 사용

    let page = 0;
    let totalPages = 1;
    let totalElements = 0;

    // ===============================
    // role
    // ===============================
    function isEmployee() {
        const cls = document.documentElement.classList;
        return cls.contains("role-employee") || cls.contains("role-EMPLOYEE");
    }

    // EMPLOYEE면 pending 페이지 접근 자체를 막고 안내
    if (isEmployee()) {
        alert("권한이 없습니다. (승인 대기 목록은 관리자만 접근 가능합니다.)");
        location.replace("/form/forms");
        return;
    }

    function hasRoleClass(name) {
        const html = document.documentElement?.classList;
        const body = document.body?.classList;
        return (html && html.contains(name)) || (body && body.contains(name));
    }

    function isThrAdmin() {
        return hasRoleClass("role-thr-admin") || hasRoleClass("role-THR_ADMIN") || hasRoleClass("role-thr_admin");
    }

    // ===============================
    // ui helpers
    // ===============================
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
        return writer.empName ?? writer.name ?? writer.username ?? writer.empNm ?? writer.emp_id ?? writer.empId ?? "-";
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
            P: {cls: "P", label: "대기"},
            W: {cls: "W", label: "삭제대기"},
            R: {cls: "R", label: "반려"},
            X: {cls: "X", label: "삭제반려"},
            A: {cls: "A", label: "승인"},
        };
        const it = map[s] || {cls: "", label: s};
        return `<span class="statPill ${esc(it.cls)}">${esc(it.label)}</span>`;
    }

    function markFormListDirty() {
        try {
            localStorage.setItem("list:dirty", "true");
        } catch (_) {
        }
    }

    // ===============================
    // api
    // ===============================
    async function apiFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        if (!headers.has("Accept")) headers.set("Accept", "application/json");

        const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
        if (options.body && !isFormData && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, {...options, headers, credentials: "same-origin"});
    }

    async function updateStatus(docfoNo, docfoStat, rejectReason) {
        const payload = {docfoStat};
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
        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-approve`, {method: "PATCH"});
        if (!res.ok) {
            const t = await res.text().catch(() => "");
            throw new Error(`삭제 승인 실패 HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function rejectDelete(docfoNo, rejectReason) {
        const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}/delete-reject`, {
            method: "PATCH",
            body: JSON.stringify({rejectReason: String(rejectReason).trim()}),
        });
        if (!res.ok) {
            const t = await res.text().catch(() => "");
            throw new Error(`삭제 반려 실패 HTTP ${res.status} ${t}`);
        }
        return true;
    }

    async function showRejectReason(docfoNo) {
        try {
            const res = await apiFetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {method: "GET"});
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            const reason = getRejectReason(data) ?? "사유가 저장되어 있지 않아요.";
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

    // ===============================
    // A 방식: prompt로 사유 입력
    // ===============================
    function askReason(title) {
        const reason = window.prompt(title, "");
        if (reason == null) return null; // 취소
        const trimmed = String(reason).trim();
        if (!trimmed) return ""; // 빈 입력
        return trimmed;
    }

    // ===============================
    // filter: mode -> stats (✅ W/X 유지)
    // ===============================
    function getStatsByMode(mode) {
        if (mode === "P") return ["P", "W"]; // 대기 그룹
        if (mode === "R") return ["R", "X"]; // 반려 그룹
        return ["P", "W", "R", "X"]; // 모두
    }

    // ===============================
    // render
    // ===============================
    function render(rows) {
        if (!elTbody) return;

        if (!rows || rows.length === 0) {
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">조회 결과가 없어요.</td></tr>`;
            return;
        }

        const startNo = page * PAGE_SIZE;
        const thr = isThrAdmin();

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

                // THIRD-ADMIN: 버튼 없음, 대기는 '-', 반려는 사유버튼(있으면)
                if (thr) {
                    if (isPendingGroup) {
                        actionHtml = `<span class="muted">-</span>`;
                    } else if (isRejectedGroup) {
                        actionHtml = reasonInline
                            ? `<button class="btn sm warn btn-reason" data-action="reason" data-id="${esc(idStr)}">사유</button>`
                            : `<span class="muted">-</span>`;
                    } else {
                        actionHtml = `<span class="muted">-</span>`;
                    }
                } else {
                    // 승인/반려 가능 권한자
                    if (stat === "P") {
                        actionHtml = `
              <button class="btn sm ok" data-action="approve" data-id="${esc(idStr)}">승인</button>
              <button class="btn sm danger" data-action="reject" data-id="${esc(idStr)}">반려</button>
            `;
                    } else if (stat === "W") {
                        actionHtml = `
              <button class="btn sm ok" data-action="delApprove" data-id="${esc(idStr)}">삭제승인</button>
              <button class="btn sm danger" data-action="delReject" data-id="${esc(idStr)}">삭제반려</button>
            `;
                    } else if (isRejectedGroup) {
                        actionHtml = reasonInline
                            ? `<button class="btn sm warn" data-action="reason" data-id="${esc(idStr)}">사유</button>`
                            : `<span class="muted">-</span>`;
                    } else {
                        actionHtml = `<span class="muted">-</span>`;
                    }
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
        if (!elPager) return;
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

    // ===============================
    // load (묶음 조회 -> 병합 -> 클라페이징)  ✅ W/X 포함 유지
    // ===============================
    async function load() {
        if (!elTbody) return;
        elTbody.innerHTML = `<tr><td colspan="5" class="muted">로딩 중...</td></tr>`;

        try {
            const mode = elMode?.value || "ALL";
            const stats = getStatsByMode(mode);

            const results = await Promise.all(
                stats.map((s) =>
                    apiFetch(`${API_BASE}?stat=${encodeURIComponent(s)}&page=0&size=${BIG}`, {method: "GET"}).then(async (res) => {
                        if (!res.ok) {
                            const t = await res.text().catch(() => "");
                            throw new Error(`${s} 조회 실패 HTTP ${res.status} ${t}`);
                        }
                        const json = await res.json();
                        const items =
                            json?.content && Array.isArray(json.content)
                                ? json.content
                                : json?.data?.content && Array.isArray(json.data.content)
                                    ? json.data.content
                                    : [];
                        return items;
                    })
                )
            );

            const map = new Map();
            results.flat().forEach((it) => {
                const key = String(it.docfoNo ?? it.id ?? it.docfo_no ?? "");
                if (!key) return;
                map.set(key, it);
            });

            const merged = Array.from(map.values()).sort((a, b) => {
                const ax = Number(a.docfoNo ?? a.id ?? a.docfo_no ?? 0);
                const bx = Number(b.docfoNo ?? b.id ?? b.docfo_no ?? 0);
                return ax - bx;
            });

            totalElements = merged.length;
            totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
            page = Math.min(Math.max(0, page), totalPages - 1);

            const start = page * PAGE_SIZE;
            const rows = merged.slice(start, start + PAGE_SIZE);

            if (elInfo) elInfo.textContent = `page ${page + 1} / ${Math.max(totalPages, 1)}`;
            if (elCountPill) elCountPill.textContent = `${totalElements}건`;

            renderPager();
            render(rows);
        } catch (err) {
            console.error(err);
            elTbody.innerHTML = `<tr><td colspan="5" class="muted">불러오기 실패: ${esc(err?.message || err)}</td></tr>`;
            totalPages = 1;
            if (elInfo) elInfo.textContent = "page 1 / 1";
            if (elCountPill) elCountPill.textContent = "0건";
            renderPager();
        }
    }

    // ===============================
    // events
    // ===============================
    elMode?.addEventListener("change", () => {
        page = 0;
        load();
    });

    // tbody 이벤트 위임(버튼/링크)
    elTbody?.addEventListener("click", async (e) => {
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

            // THIRD-ADMIN은 버튼 자체가 없지만 혹시 남아도 무시
            if (isThrAdmin()) return;

            if (action === "approve") {
                await updateStatus(id, "A");
                toast("승인 처리 완료");
                markFormListDirty();
                await load();
                return;
            }

            if (action === "reject") {
                const reason = askReason("반려 사유를 입력하세요");
                if (reason == null) return; // 취소
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
                if (!confirm("삭제 요청을 승인하시겠습니까?")) return;
                await approveDelete(id);
                toast("삭제 승인 완료");
                markFormListDirty();
                await load();
                return;
            }

            if (action === "delReject") {
                const reason = askReason("삭제 반려 사유를 입력하세요");
                if (reason == null) return; // 취소
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

    // init
    window.openDetail = openDetail;
    load();
})();