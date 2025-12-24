// static/src/update-form.js
const API_BASE = '/api/v1/forms'; // 너 프로젝트 REST 기준
const VIEW_BASE = '/document-form/manager/form';

const elName = document.getElementById('docfoName');
const elChecks = document.getElementById('typeChecks');
const elStatus = document.getElementById('statusLine');

const btnCancel = document.getElementById('btnCancel');
const btnSaveTemp = document.getElementById('btnSaveTemp');
const btnSave = document.getElementById('btnSave');
const toolbar = document.getElementById('toolbar');

function getDocfoNo() {
    // 1) ?docfoNo=123 우선
    const sp = new URLSearchParams(location.search);
    const q = sp.get('docfoNo') || sp.get('id');
    if (q) return q;

    // 2) meta[name="docfo-no"]
    const meta = document.querySelector('meta[name="docfo-no"]');
    return meta?.content || '';
}

const docfoNo = getDocfoNo();
if (!docfoNo) {
    alert('docfoNo가 없습니다. update-form?docfoNo=... 형태로 열어주세요.');
    throw new Error('docfoNo missing');
}

function esc(s){
    return String(s ?? '')
        .replaceAll('&','&amp;')
        .replaceAll('<','&lt;')
        .replaceAll('>','&gt;')
        .replaceAll('"','&quot;')
        .replaceAll("'",'&#39;');
}

function uniqStrings(list){
    const out = [];
    const seen = new Set();
    (Array.isArray(list) ? list : []).forEach(v=>{
        const s = String(v ?? '').trim();
        if(!s) return;
        if(seen.has(s)) return;
        seen.add(s);
        out.push(s);
    });
    return out;
}

async function fetchDetail() {
    // 서비스 findDetailById가 반환하는 구조: docfoNo/docfoName/docfoStat/cnttJson/cnttHtml/categories :contentReference[oaicite:1]{index=1}
    const res = await fetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {
        headers: { Accept: 'application/json' }
    });
    if (!res.ok) {
        const t = await res.text().catch(()=> '');
        throw new Error(`상세 조회 실패 HTTP ${res.status} ${t}`);
    }
    return await res.json();
}

/* ---------------- TipTap boot (필요 최소) ---------------- */

async function bootEditor(initialJson){
    const core = await import('https://esm.sh/@tiptap/core@2.11.2?bundle&target=es2020');
    const { Editor } = core;

    const starterKitMod = await import('https://esm.sh/@tiptap/starter-kit@2.11.2?bundle&target=es2020');
    const StarterKit = starterKitMod.default ?? starterKitMod.StarterKit;

    const tableMod = await import('https://esm.sh/@tiptap/extension-table@2.11.2?bundle&target=es2020');
    const Table = tableMod.default ?? tableMod.Table;

    const trMod = await import('https://esm.sh/@tiptap/extension-table-row@2.11.2?bundle&target=es2020');
    const TableRow = trMod.default ?? trMod.TableRow;

    const tcMod = await import('https://esm.sh/@tiptap/extension-table-cell@2.11.2?bundle&target=es2020');
    const TableCell = tcMod.default ?? tcMod.TableCell;

    const thMod = await import('https://esm.sh/@tiptap/extension-table-header@2.11.2?bundle&target=es2020');
    const TableHeader = thMod.default ?? thMod.TableHeader;

    const taMod = await import('https://esm.sh/@tiptap/extension-text-align@2.11.2?bundle&target=es2020');
    const TextAlign = taMod.default ?? taMod.TextAlign;

    const editor = new Editor({
        element: document.getElementById('editor'),
        extensions: [
            StarterKit,
            TextAlign.configure({ types: ['heading', 'paragraph'] }),
            Table.configure({ resizable: false }),
            TableRow, TableHeader, TableCell,
        ],
        content: initialJson || { type:'doc', content: [{ type:'paragraph' }] },
    });

    return editor;
}

/* ---------------- UI helpers ---------------- */

function renderCategoryChecks(allCats, selectedCats){
    const cats = uniqStrings(allCats);
    const sel = new Set(uniqStrings(selectedCats));

    if(cats.length === 0){
        elChecks.innerHTML = `<span class="muted">카테고리 없음</span>`;
        return;
    }

    elChecks.innerHTML = cats.map((c, i) => {
        const id = `cat_${i}_${c.replace(/\W+/g,'_')}`;
        const checked = sel.has(c) ? 'checked' : '';
        return `
      <label for="${esc(id)}">
        <input type="checkbox" id="${esc(id)}" value="${esc(c)}" ${checked} />
        <span>${esc(c)}</span>
      </label>
    `;
    }).join('');
}

function getSelectedCategories(){
    const checks = elChecks.querySelectorAll('input[type="checkbox"]');
    const out = [];
    checks.forEach(ch => { if(ch.checked) out.push(ch.value); });
    return uniqStrings(out);
}

function bindToolbar(editor){
    toolbar.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-cmd]');
        if(!btn) return;
        const cmd = btn.dataset.cmd;

        const c = editor.chain().focus();

        switch(cmd){
            case 'bold': c.toggleBold().run(); break;
            case 'italic': c.toggleItalic().run(); break;
            case 'strike': c.toggleStrike().run(); break;

            case 'h1': c.toggleHeading({ level: 1 }).run(); break;
            case 'h2': c.toggleHeading({ level: 2 }).run(); break;
            case 'p': c.setParagraph().run(); break;

            case 'ul': c.toggleBulletList().run(); break;
            case 'ol': c.toggleOrderedList().run(); break;

            case 'table': c.insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(); break;

            case 'undo': c.undo().run(); break;
            case 'redo': c.redo().run(); break;
        }
    });
}

async function save(editor, stat){
    const name = elName.value.trim();
    if(!name){
        alert('양식명을 입력하세요.');
        elName.focus();
        return;
    }

    // PUT req DTO는 네 프로젝트에서 DocumentFormCreateReqDto를 재사용하는 구조로 보임 :contentReference[oaicite:2]{index=2}
    // 그래서 create와 동일하게 comId/writerId도 넣는 형태로 보내는게 안전함(서버에서 무시/사용).
    const payload = {
        // comId/writerId는 “기존 값 기반”으로 넣기 위해 detail에서 보관해둠
        comId: window.__FORM__.comId || 'COM',          // 없으면 서버쪽에서 처리/검증에 맞춰 수정
        writerId: window.__FORM__.writerId || 'EMP',   // 없으면 서버쪽에서 처리/검증에 맞춰 수정
        docfoName: name,
        cnttJson: editor.getJSON(),
        cnttHtml: editor.getHTML(),
        categories: getSelectedCategories(),
        // 상태까지 저장한다면(임시저장 T 같은) 서버 DTO에 필드가 있어야 함.
        // 현재 CreateReqDto에 stat이 없다면 이 값은 빼야 함.
        // docfoStat: stat,
    };

    const res = await fetch(`${API_BASE}/${encodeURIComponent(docfoNo)}`, {
        method: 'PUT',
        headers: { 'Content-Type':'application/json', Accept:'application/json' },
        body: JSON.stringify(payload)
    });

    if(!res.ok){
        const t = await res.text().catch(()=> '');
        throw new Error(`저장 실패 HTTP ${res.status} ${t}`);
    }

    // 서버가 새 docfoNo(Long)만 반환하는 형태면 number로 올 것
    const data = await res.json().catch(()=> null);
    return data;
}

function closeOrBack(){
    window.close();
    setTimeout(()=>{ if(!document.hidden) history.back(); }, 80);
}

/* ---------------- run ---------------- */

(async function main(){
    try{
        elStatus.textContent = `불러오는 중... docfoNo=${docfoNo}`;

        const detail = await fetchDetail();

        // detail에서 필요한 값들
        const name = detail.docfoName ?? detail.docfo_name ?? '';
        const json = detail.cnttJson ?? detail.cntt_json ?? null;
        const categories = detail.categories ?? [];

        // (선택) 서버가 comId/writerId를 detail에 안 내려주면 따로 detail DTO에 넣어두는게 편함
        // 지금 DetailResDto에는 comId/writerId가 없어보이니(스니펫 기준) 임시로 전역에 비워둠 :contentReference[oaicite:3]{index=3}
        window.__FORM__ = {
            comId: detail.comId ?? detail.com_id ?? null,
            writerId: detail.writerId ?? detail.writer_id ?? null,
        };

        elName.value = name;

        // 카테고리: “가능한 전체 카테고리 목록”은 현재 detail에서 categories만 내려주니
        // 일단은 “기존 카테고리만 체크 박스”로 렌더. (나중에 전체 카테고리 API 붙이면 allCats로 교체)
        renderCategoryChecks(categories, categories);

        const editor = await bootEditor(json);
        bindToolbar(editor);

        btnCancel.addEventListener('click', closeOrBack);

        btnSaveTemp.addEventListener('click', async () => {
            try{
                btnSaveTemp.disabled = true;
                const newId = await save(editor, 'T');
                elStatus.textContent = `임시저장 완료`;
                // newId가 숫자면 새 상세로 이동 가능
                if(typeof newId === 'number'){
                    window.open(`${VIEW_BASE}/${encodeURIComponent(newId)}`, '_blank');
                }
            }catch(e){
                alert('임시저장 실패: ' + (e?.message || e));
            }finally{
                btnSaveTemp.disabled = false;
            }
        });

        btnSave.addEventListener('click', async () => {
            try{
                btnSave.disabled = true;
                const newId = await save(editor, 'P'); // 저장=대기(P)로 간주한다면
                elStatus.textContent = `저장 완료`;
                if(typeof newId === 'number'){
                    // 저장 후 새로 생성된 양식 상세로 이동
                    location.href = `${VIEW_BASE}/${encodeURIComponent(newId)}`;
                }
            }catch(e){
                alert('저장 실패: ' + (e?.message || e));
            }finally{
                btnSave.disabled = false;
            }
        });

        elStatus.textContent = `로드 완료 (docfoNo=${docfoNo})`;
    }catch(e){
        console.error(e);
        elStatus.textContent = '오류: ' + (e?.message || e);
        alert('로드 실패: ' + (e?.message || e));
    }
})();