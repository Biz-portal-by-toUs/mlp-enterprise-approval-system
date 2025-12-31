-- ========================================================
-- 더미 데이터 (FK 순서 고려)
-- ========================================================
USE bizportal;
SET FOREIGN_KEY_CHECKS = 0;
-- 1) subscription (최대 3개만 가능)
INSERT INTO subscription (sub_name, sub_desc, sub_price) VALUES
                                                             ('basic',    'Basic plan',    10000),
                                                             ('pro',      'Pro plan',      30000),
                                                             ('ultimate', 'Ultimate plan', 70000);

-- 2) company (sub_no: 1~3 참조)
INSERT INTO company
(com_id, com_name, email, pwd, brn, emp_cnt, addr, role, sub_no, img_url, path)
VALUES
    ('C01', '테스트기업', 'admin@c01.com',
     '$2b$10$Dua3gQf03uEl91WTUjabhuxdk0gl1lq2UcdoPJpINAyV2KovcFtB6',
     '1234567890', 50, '서울특별시 강남구', 'COM_ADMIN', 1, NULL, NULL);

-- 시스템 관리자(개발/운영) : SYS_ADMIN
INSERT INTO company
(com_id, com_name, email, pwd, brn, emp_cnt, addr, role, sub_no, img_url, path)
VALUES
    ('SYS', '시스템운영', 'sysadmin@bizportal.com',
     '$2b$10$xb9h4bboD7Uis5hRrYmdO.rbSuiyGp.e4d2eWk51v0GOy0/bTe9Ru',
     '0000000000', 1, '서울특별시', 'SYS_ADMIN', 1, NULL, NULL);




-- 5) employee (dep_no:1~5, pos_no:1~5)
INSERT INTO employee
(com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
 hire_date, ret_date, addr, role, atte, msg_stat, delegate, is_deleted)
VALUES
-- 회사 관리자(사원 페이지에서도 최상위 관리자 역할로 쓸 수 있음)
('C01', 4, 4, 'E000001',
 '$2b$10$Dua3gQf03uEl91WTUjabhuxdk0gl1lq2UcdoPJpINAyV2KovcFtB6',
 '홍관리', 'comadmin@c01.com', '010-1111-1111', '0212345678', 'M',
 '2025-01-02 09:00:00', NULL, '서울특별시 강남구', 'COM_ADMIN', 'N', 'N', NULL,false),

-- 2차 관리자
('C01', 3, 3, 'E000002',
 '$2b$10$IRGCWkvwSo1QejSQoHuH/.V0TRU69cFAteCJurcoIWODwWmg5zu6G',
 '김보안', 'secadmin@c01.com', '010-2222-2222', '0212345679', 'F',
 '2025-01-02 09:10:00', NULL, '서울특별시 강남구', 'SEC_ADMIN', 'N', 'N', 'E000001',false),

-- 3차 관리자
('C01', 2, 2, 'E000003',
 '$2b$10$D4ZZH6fC11EykcEdVhHUvulXICZ6BRtKd4GeJ9hSMjGiOE0e5PCfS',
 '박삼차', 'thradmin@c01.com', '010-3333-3333', '0212345680', 'M',
 '2025-01-02 09:20:00', NULL, '서울특별시 강남구', 'THR_ADMIN', 'N', 'N', 'E000001',false),

-- 일반 사원
('C01', 1, 1, 'E000004',
 '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
 '이사원', 'employee@c01.com', '010-4444-4444', '0212345681', 'F',
 '2025-01-02 09:30:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', 'E000001',false),

('C01', 1, 5, 'E000005',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '최부장', 'ceo@c01.com', '010-5555-5555', '0212345682', 'M',
    '2020-01-02 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', NULL, false),

-- 과장2 (중간 결재자)
('C01', 1, 3, 'E000006',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '정과장', 'manager@c01.com', '010-6666-6666', '0212345683', 'F',
    '2021-03-15 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', NULL, false),

-- 대리2 (결재자)
('C01', 2, 2, 'E000007',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '강대리', 'assistant@c01.com', '010-7777-7777', '0212345684', 'M',
    '2022-06-01 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', NULL, false),

-- 사원2 (문서 작성자)
('C01', 1, 1, 'E000008',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '윤사원', 'staff2@c01.com', '010-8888-8888', '0212345685', 'F',
    '2023-09-01 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', NULL, false),

-- 사원3 (문서 작성자)
('C01', 3, 1, 'E000009',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '장사원', 'staff3@c01.com', '010-9999-9999', '0212345686', 'M',
    '2024-01-15 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', NULL, false),

-- 휴가중인 차장 (대직자 테스트용)
('C01', 1, 4, 'E000010',
    '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
    '한차장', 'deputy@c01.com', '010-1010-1010', '0212345687', 'M',
    '2019-05-01 09:00:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'V', 'N', 'E000006', false);

-- ========================================================
-- 3) department (dep_no 1~5 고정: employee.dep_no FK 충족)
-- ========================================================
INSERT INTO department (dep_no, dep_id, dep_name, com_id) VALUES
(1, 'D01', '개발팀', 'C01'),
(2, 'D02', '보안팀', 'C01'),
(3, 'D03', '인사팀', 'C01'),
(4, 'D04', '총무팀', 'C01'),
(5, 'D05', '영업팀', 'C01');

-- ========================================================
-- 4) positions (pos_no 1~5 고정: employee.pos_no FK 충족)
-- ========================================================
INSERT INTO positions (pos_no, pos_name, com_id, pos_order) VALUES
(1, '사원', 'C01', 5),
(2, '대리', 'C01', 4),
(3, '과장', 'C01', 3),
(4, '차장', 'C01', 2),
(5, '부장', 'C01', 1);

-- ========================================================
-- 6) schedule (사용자 데이터 기반, FK 정합성 맞춘 버전)
-- ========================================================
INSERT INTO schedule (com_id, dep_no, title, content, start_at, ended_at, color, reg_emp) VALUES
('C01',1,'주간회의','주간업무 공유','2025-12-16 10:00:00','2025-12-16 11:00:00','#FFAA00','E000001'),
('C01',2,'스프린트 계획','백로그 점검','2025-12-17 14:00:00','2025-12-17 15:00:00','#00AAFF','E000002'),
('C01',3,'결산 준비','재무 자료 정리','2025-12-18 09:30:00','2025-12-18 10:30:00','#00CC66','E000003'),
('C01',4,'영업 미팅','고객사 미팅','2025-12-19 16:00:00','2025-12-19 17:00:00','#AA00FF','E000004'),
('C01',5,'운영 점검','장비 점검','2025-12-20 13:00:00','2025-12-20 14:00:00','#666666','E000001');

-- ========================================================
-- emp_schedule (5)
-- ========================================================
INSERT INTO emp_schedule (emp_id, title, content, start_at, ended_at, color) VALUES
('E000001','개인 일정 1','개인 업무 정리','2025-12-17 09:00:00','2025-12-17 09:30:00','#999999'),
('E000002','개인 일정 2','보안 리포트 작성','2025-12-17 12:10:00','2025-12-17 12:40:00','#999999'),
('E000003','개인 일정 3','인사 문서 검토','2025-12-17 16:10:00','2025-12-17 16:40:00','#999999'),
('E000004','개인 일정 4','API 구현','2025-12-18 10:00:00','2025-12-18 12:00:00','#999999'),
('E000001','개인 일정 5','회의 준비','2025-12-18 13:00:00','2025-12-18 14:00:00','#999999');

-- ========================================================
-- todo_list (5)
-- ========================================================
INSERT INTO todo_list (emp_id, title, is_done) VALUES
                                                   ('E000001','요구사항 정리', FALSE),
                                                   ('E000002','보안 정책 문서화', FALSE),
                                                   ('E000003','면접 일정 조율', TRUE),
                                                   ('E000004','API 구현', FALSE),
                                                   ('E000001','코드 리뷰', TRUE);

-- ========================================================
-- document_form (5)
-- ========================================================
INSERT INTO document_form
(com_id, writer_id, docfo_name, cntt_json, cntt_html, docfo_stat, reject_reason)
VALUES
('C01', 'E000001', '기본 양식',
     '{
       "type": "doc",
       "content": [
         {
           "type": "table",
           "content": [
             {
               "type": "tableRow",
               "attrs": { "height": 166 },
               "content": [
                 {
                   "type": "tableCell",
                   "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false },
                   "content": [
                     {
                       "type": "paragraph",
                       "attrs": { "textAlign": null },
                       "content": [
                         { "text": "s", "type": "text", "marks": [{ "type": "textStyle", "attrs": { "fontSize": "16px" } }] },
                         { "text": "adfasdf", "type": "text", "marks": [{ "type": "textStyle", "attrs": { "color": null, "fontSize": "12px", "fontFamily": null } }] }
                       ]
                     }
                   ]
                 },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [71], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] }
               ]
             },
             {
               "type": "tableRow",
               "attrs": { "height": null },
               "content": [
                 {
                   "type": "tableCell",
                   "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false },
                   "content": [
                     {
                       "type": "paragraph",
                       "attrs": { "textAlign": null },
                       "content": [{ "text": "sadfasdf", "type": "text", "marks": [{ "type": "textStyle", "attrs": { "color": null, "fontSize": "24px", "fontFamily": null } }, { "type": "bold" }] }]
                     }
                   ]
                 },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [71], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] }
               ]
             },
             {
               "type": "tableRow",
               "attrs": { "height": null },
               "content": [
                 {
                   "type": "tableCell",
                   "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false },
                   "content": [{ "type": "paragraph", "attrs": { "textAlign": null }, "content": [{ "text": "sdfasd", "type": "text", "marks": [{ "type": "textStyle", "attrs": { "fontSize": "16px" } }] }] }]
                 },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [71], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] },
                 { "type": "tableCell", "attrs": { "colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true }, "content": [{ "type": "paragraph", "attrs": { "textAlign": null } }] }
               ]
             }
           ]
         },
         { "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "​​safsadf:", "type": "text" }, { "type": "inputField", "attrs": { "value": "", "locked": false, "editable": true, "placeholder": "입력" } }] },
         { "type": "paragraph", "attrs": { "textAlign": "center" }, "content": [{ "text": "sdfasf", "type": "text" }] },
         { "type": "paragraph", "attrs": { "textAlign": "right" }, "content": [{ "text": "sfdasdfds", "type": "text" }] },
         { "type": "bulletList", "content": [{ "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "1", "type": "text" }] }] }, { "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "2", "type": "text" }] }] }, { "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "3", "type": "text" }] }] }] },
         { "type": "orderedList", "attrs": { "type": null, "start": 1 }, "content": [{ "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "1", "type": "text" }] }] }, { "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "2", "type": "text" }] }] }, { "type": "listItem", "content": [{ "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [{ "text": "3", "type": "text" }] }] }] }
       ]
     }',
     '', 'A', NULL),
('C01','E000002','휴가 신청서', JSON_OBJECT('type','trip','fields',JSON_ARRAY('목적','장소')), '', 'A', NULL),
('C01','E000003','출장 결의서', JSON_OBJECT('type','expense','fields',JSON_ARRAY('금액','내역')), '', 'A', NULL),
('C01', 'E000004', '사직서',
     '{"type": "doc", "content": [{"type": "paragraph", "attrs": {"textAlign": null}, "content": [{"text": "asdfsadf", "type": "text", "marks": [{"type": "textStyle", "attrs": {"color": null, "fontSize": "28px", "fontFamily": null}}]}]}, {"type": "paragraph", "attrs": {"textAlign": null}}, {"type": "table", "content": [{"type": "tableRow", "attrs": {"height": null}, "content": [{"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}, "content": [{"text": "sadf", "type": "text", "marks": [{"type": "textStyle", "attrs": {"fontSize": "16px"}}]}]}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}]}, {"type": "tableRow", "attrs": {"height": null}, "content": [{"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}, "content": [{"text": "asdf", "type": "text", "marks": [{"type": "textStyle", "attrs": {"fontSize": "16px"}}]}]}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}]}, {"type": "tableRow", "attrs": {"height": null}, "content": [{"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": false}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}, "content": [{"text": "asdf", "type": "text", "marks": [{"type": "textStyle", "attrs": {"fontSize": "16px"}}]}]}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}, {"type": "tableCell", "attrs": {"colspan": 1, "rowspan": 1, "colwidth": [240], "editable": true}, "content": [{"type": "paragraph", "attrs": {"textAlign": null}}]}]}]}, {"type": "paragraph", "attrs": {"textAlign": "left"}, "content": [{"text": "​​asas", "type": "text"}]}]}',
     '<h1>사직서</h1>', 'A', NULL),
('C01','E000001','자진퇴사 신청서', JSON_OBJECT('type','purchase','fields',JSON_ARRAY('품목','수량')), '<h1>자신퇴사 신청서</h1>', 'A', '예산 부족');

-- document_form_category (5)
INSERT INTO document_form_category (com_id, name, docfo_no) VALUES
('C01','기본 카테고리1',1),
('C01','기본 카테고리2',1),
('C01','기본 카테고리3',1),
('C01','기본 카테고리4',1),
('C01','기본 카테고리5',1),
('C01','휴가신청',2),
('C01','휴가취소신청',2),
('C01','출장신청',3),
('C01','출장취소신청',3),
('C01','사직',4),
('C01','자진퇴사',5);

-- attach_box (5)
INSERT INTO attach_box (attach_no, com_id, uploader, title, dscp, path, size) VALUES
                                                                                  (1,'C01','E000001','회사 소개서','PDF','/s3/attach/company_intro.pdf', 102400),
                                                                                  (2,'C01','E000002','보안 정책','DOCX','/s3/attach/security_policy.docx', 204800),
                                                                                  (3,'C01','E000003','채용 공고','PDF','/s3/attach/recruit.pdf', 51200),
                                                                                  (4,'C01','E000004','개발 가이드','MD','/s3/attach/dev_guide.md', 4096),
                                                                                  (5,'C01','E000001','견적서','XLSX','/s3/attach/quote.xlsx', 307200);

-- document (5)
-- ========================================================
-- 문서 & 결재라인 테스트 데이터 (5명 x 15개 = 75개 문서)
-- ========================================================
USE bizportal;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================================
-- E000004 이사원 (사원) - 15개 문서
-- 결재라인: 박삼차(대리) → 김보안(과장) → 홍관리(차장) → 최부장(부장)
-- ========================================================

-- 임시저장 (US) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (1, 'C01', NULL, 6, 2, '(임시) 신년 휴가 계획', '{"기간": "2026-01-02 ~ 2026-01-03", "사유": "", "일수": 2}', '', 'E000004', NULL, TRUE, NULL, '2025-12-24 10:00:00', '2025-12-24 10:00:00', 'US'),
                                                                                                                                                                            (2, 'C01', NULL, 8, 3, '(임시) 제주 출장', '{"장소": "제주", "목적": "", "기간": ""}', '', 'E000004', NULL, TRUE, NULL, '2025-12-24 10:10:00', '2025-12-24 10:10:00', 'US'),
                                                                                                                                                                            (3, 'C01', NULL, 1, 1, '(임시) 비품 구매', '{"품목": "키보드", "수량": 1}', '', 'E000004', NULL, TRUE, NULL, '2025-12-24 10:20:00', '2025-12-24 10:20:00', 'US');

-- 임시저장 결재라인 (모두 W 상태)
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 1, 'E000003', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 1, 'E000002', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 2, 'E000003', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 2, 'E000002', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 3, 'E000003', 1, 'W', NULL, FALSE, NULL, FALSE);

-- 결재중 (AW) 5개 - 다양한 진행 상태
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
-- 1번째 결재자 차례 (아무도 결재 안함) - 상신취소 가능
(4, 'C01', NULL, 6, 2, '연차 휴가 신청 (1/10-1/12)', '{"기간": "2026-01-10 ~ 2026-01-12", "사유": "가족여행", "일수": 3}', '', 'E000004', '연차 3일 신청', FALSE, '2025-12-24 09:00:00', '2025-12-24 08:50:00', '2025-12-24 09:00:00', 'AW'),
-- 1번째 결재자가 승인, 2번째 차례
(5, 'C01', NULL, 8, 3, '서울 출장 신청', '{"장소": "서울 강남", "목적": "고객 미팅", "기간": "2026-01-15"}', '', 'E000004', '서울 출장 1일', FALSE, '2025-12-23 09:00:00', '2025-12-23 08:50:00', '2025-12-23 14:00:00', 'AW'),
-- 2번째 결재자가 승인, 3번째 차례
(6, 'C01', NULL, 6, 2, '오후 반차 신청', '{"기간": "2026-01-20 오후", "사유": "병원", "일수": 0.5}', '', 'E000004', '오후 반차', FALSE, '2025-12-22 09:00:00', '2025-12-22 08:50:00', '2025-12-23 10:00:00', 'AW'),
-- 3번째 결재자가 승인, 4번째(마지막) 차례
(7, 'C01', NULL, 1, 1, '모니터 구매 요청', '{"품목": "32인치 모니터", "수량": 1, "예상금액": 500000}', '', 'E000004', '모니터 구매 50만원', FALSE, '2025-12-21 09:00:00', '2025-12-21 08:50:00', '2025-12-23 16:00:00', 'AW'),
-- 결재자 1명만 (승인하면 바로 최종승인)
(8, 'C01', NULL, 1, 1, '문구류 구매 요청', '{"품목": "볼펜, 노트", "수량": 10, "예상금액": 30000}', '', 'E000004', '문구류 3만원', FALSE, '2025-12-24 11:00:00', '2025-12-24 10:50:00', '2025-12-24 11:00:00', 'AW');

-- 결재중 결재라인
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
-- 문서 4: 1번째 차례
('C01', 4, 'E000003', 1, 'I', NULL, FALSE, NULL, FALSE),
('C01', 4, 'E000002', 2, 'W', NULL, FALSE, NULL, FALSE),
('C01', 4, 'E000001', 3, 'W', NULL, FALSE, NULL, FALSE),
('C01', 4, 'E000005', 4, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 5: 2번째 차례
('C01', 5, 'E000003', 1, 'A', '2025-12-23 14:00:00', TRUE, NULL, FALSE),
('C01', 5, 'E000002', 2, 'I', NULL, FALSE, NULL, FALSE),
('C01', 5, 'E000001', 3, 'W', NULL, FALSE, NULL, FALSE),
('C01', 5, 'E000005', 4, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 6: 3번째 차례
('C01', 6, 'E000003', 1, 'A', '2025-12-22 14:00:00', TRUE, NULL, FALSE),
('C01', 6, 'E000002', 2, 'A', '2025-12-23 10:00:00', TRUE, NULL, FALSE),
('C01', 6, 'E000001', 3, 'I', NULL, FALSE, NULL, FALSE),
('C01', 6, 'E000005', 4, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 7: 4번째(마지막) 차례
('C01', 7, 'E000003', 1, 'A', '2025-12-21 14:00:00', TRUE, NULL, FALSE),
('C01', 7, 'E000002', 2, 'A', '2025-12-22 10:00:00', TRUE, NULL, FALSE),
('C01', 7, 'E000001', 3, 'A', '2025-12-23 10:00:00', TRUE, NULL, FALSE),
('C01', 7, 'E000005', 4, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 8: 결재자 1명만
('C01', 8, 'E000003', 1, 'I', NULL, FALSE, NULL, FALSE);

-- 최종승인 (FI) 4개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (9, 'C01', 'C01D0120250001', 6, 2, '연차 휴가 (12/20-12/22)', '{"기간": "2025-12-20 ~ 2025-12-22", "사유": "가족 여행", "일수": 3}', '', 'E000004', '연차 3일', FALSE, '2025-12-10 09:00:00', '2025-12-10 08:50:00', '2025-12-12 16:30:00', 'FI'),
                                                                                                                                                                            (10, 'C01', 'C01D0120250002', 8, 3, '부산 출장 완료', '{"장소": "부산", "목적": "고객 미팅", "기간": "2025-12-15~16"}', '', 'E000004', '부산 출장 2일', FALSE, '2025-12-05 09:00:00', '2025-12-05 08:50:00', '2025-12-08 14:00:00', 'FI'),
                                                                                                                                                                            (11, 'C01', 'C01D0120250003', 6, 2, '오전 반차 (12/05)', '{"기간": "2025-12-05 오전", "사유": "병원", "일수": 0.5}', '', 'E000004', '오전 반차', FALSE, '2025-12-03 09:00:00', '2025-12-03 08:50:00', '2025-12-04 10:00:00', 'FI'),
                                                                                                                                                                            (12, 'C01', 'C01D0120250004', 1, 1, '사무용품 구매 완료', '{"품목": "A4용지", "수량": 10, "예상금액": 50000}', '', 'E000004', '사무용품 5만원', FALSE, '2025-12-01 09:00:00', '2025-12-01 08:50:00', '2025-12-02 16:00:00', 'FI');

-- 최종승인 결재라인 (모두 A)
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 9, 'E000003', 1, 'A', '2025-12-10 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 9, 'E000002', 2, 'A', '2025-12-11 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 9, 'E000001', 3, 'A', '2025-12-12 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 9, 'E000005', 4, 'A', '2025-12-12 16:30:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 10, 'E000003', 1, 'A', '2025-12-06 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 10, 'E000002', 2, 'A', '2025-12-07 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 10, 'E000005', 3, 'A', '2025-12-08 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 11, 'E000003', 1, 'A', '2025-12-03 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 11, 'E000002', 2, 'A', '2025-12-04 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 12, 'E000003', 1, 'A', '2025-12-01 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 12, 'E000002', 2, 'A', '2025-12-02 16:00:00', TRUE, NULL, FALSE);

-- 반려 (RJ) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
-- 1번째 결재자가 반려
(13, 'C01', NULL, 6, 2, '장기 휴가 신청 (반려)', '{"기간": "2026-01-01 ~ 2026-01-15", "사유": "해외여행", "일수": 15}', '', 'E000004', '장기휴가 15일', FALSE, '2025-12-15 09:00:00', '2025-12-15 08:50:00', '2025-12-15 14:00:00', 'RJ'),
-- 2번째 결재자가 반려
(14, 'C01', NULL, 8, 3, '해외 출장 신청 (반려)', '{"장소": "일본 도쿄", "목적": "전시회", "기간": "2026-02-01~05"}', '', 'E000004', '해외출장 5일', FALSE, '2025-12-14 09:00:00', '2025-12-14 08:50:00', '2025-12-16 10:00:00', 'RJ'),
-- 3번째 결재자가 반려
(15, 'C01', NULL, 1, 1, '고가 장비 구매 (반려)', '{"품목": "맥북 프로", "수량": 1, "예상금액": 4500000}', '', 'E000004', '맥북 450만원', FALSE, '2025-12-13 09:00:00', '2025-12-13 08:50:00', '2025-12-17 16:00:00', 'RJ');

-- 반려 결재라인
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
-- 문서 13: 1번째가 반려
('C01', 13, 'E000003', 1, 'R', '2025-12-15 14:00:00', TRUE, '15일 연속 휴가는 업무 공백이 큽니다.', FALSE),
('C01', 13, 'E000002', 2, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 14: 2번째가 반려
('C01', 14, 'E000003', 1, 'A', '2025-12-15 10:00:00', TRUE, NULL, FALSE),
('C01', 14, 'E000002', 2, 'R', '2025-12-16 10:00:00', TRUE, '해외 출장 예산이 부족합니다.', FALSE),
('C01', 14, 'E000001', 3, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 15: 3번째가 반려
('C01', 15, 'E000003', 1, 'A', '2025-12-14 10:00:00', TRUE, NULL, FALSE),
('C01', 15, 'E000002', 2, 'A', '2025-12-15 10:00:00', TRUE, NULL, FALSE),
('C01', 15, 'E000001', 3, 'R', '2025-12-17 16:00:00', TRUE, '장비 구매 예산이 초과되었습니다.', FALSE),
('C01', 15, 'E000005', 4, 'W', NULL, FALSE, NULL, FALSE);


-- ========================================================
-- E000003 박삼차 (대리) - 15개 문서
-- 결재라인: 김보안(과장) → 홍관리(차장) → 최부장(부장)
-- ========================================================

-- 임시저장 (US) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (16, 'C01', NULL, 6, 2, '(임시) 설 연휴 휴가', '{"기간": "2026-01-27 ~ 2026-01-30", "사유": "귀향"}', '', 'E000003', NULL, TRUE, NULL, '2025-12-24 11:00:00', '2025-12-24 11:00:00', 'US'),
                                                                                                                                                                            (17, 'C01', NULL, 8, 3, '(임시) 대구 출장', '{"장소": "대구", "목적": ""}', '', 'E000003', NULL, TRUE, NULL, '2025-12-24 11:10:00', '2025-12-24 11:10:00', 'US'),
                                                                                                                                                                            (18, 'C01', NULL, 1, 1, '(임시) 의자 구매', '{"품목": "사무용 의자"}', '', 'E000003', NULL, TRUE, NULL, '2025-12-24 11:20:00', '2025-12-24 11:20:00', 'US');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 16, 'E000002', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 16, 'E000001', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 17, 'E000002', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 18, 'E000002', 1, 'W', NULL, FALSE, NULL, FALSE);

-- 결재중 (AW) 5개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (19, 'C01', NULL, 6, 2, '연차 휴가 (2/1-2/3)', '{"기간": "2026-02-01 ~ 2026-02-03", "사유": "개인사유", "일수": 3}', '', 'E000003', '연차 3일', FALSE, '2025-12-24 09:10:00', '2025-12-24 09:00:00', '2025-12-24 09:10:00', 'AW'),
                                                                                                                                                                            (20, 'C01', NULL, 8, 3, '인천 출장 신청', '{"장소": "인천 송도", "목적": "파트너사 미팅"}', '', 'E000003', '인천 출장', FALSE, '2025-12-23 09:10:00', '2025-12-23 09:00:00', '2025-12-23 15:00:00', 'AW'),
                                                                                                                                                                            (21, 'C01', NULL, 6, 2, '오전 반차 (2/10)', '{"기간": "2026-02-10 오전", "사유": "개인사유"}', '', 'E000003', '오전 반차', FALSE, '2025-12-22 09:10:00', '2025-12-22 09:00:00', '2025-12-23 11:00:00', 'AW'),
                                                                                                                                                                            (22, 'C01', NULL, 1, 1, '노트북 거치대 구매', '{"품목": "노트북 거치대", "수량": 2, "예상금액": 80000}', '', 'E000003', '거치대 8만원', FALSE, '2025-12-21 09:10:00', '2025-12-21 09:00:00', '2025-12-23 17:00:00', 'AW'),
                                                                                                                                                                            (23, 'C01', NULL, 1, 1, '마우스 구매', '{"품목": "무선 마우스", "수량": 1, "예상금액": 50000}', '', 'E000003', '마우스 5만원', FALSE, '2025-12-24 12:00:00', '2025-12-24 11:50:00', '2025-12-24 12:00:00', 'AW');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
-- 문서 19: 1번째 차례
('C01', 19, 'E000002', 1, 'I', NULL, FALSE, NULL, FALSE),
('C01', 19, 'E000001', 2, 'W', NULL, FALSE, NULL, FALSE),
('C01', 19, 'E000005', 3, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 20: 2번째 차례
('C01', 20, 'E000002', 1, 'A', '2025-12-23 15:00:00', TRUE, NULL, FALSE),
('C01', 20, 'E000001', 2, 'I', NULL, FALSE, NULL, FALSE),
('C01', 20, 'E000005', 3, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 21: 3번째(마지막) 차례
('C01', 21, 'E000002', 1, 'A', '2025-12-22 15:00:00', TRUE, NULL, FALSE),
('C01', 21, 'E000001', 2, 'A', '2025-12-23 11:00:00', TRUE, NULL, FALSE),
('C01', 21, 'E000005', 3, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 22: 3번째(마지막) 차례
('C01', 22, 'E000002', 1, 'A', '2025-12-21 15:00:00', TRUE, NULL, FALSE),
('C01', 22, 'E000001', 2, 'A', '2025-12-22 15:00:00', TRUE, NULL, FALSE),
('C01', 22, 'E000005', 3, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 23: 1명만
('C01', 23, 'E000002', 1, 'I', NULL, FALSE, NULL, FALSE);

-- 최종승인 (FI) 4개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (24, 'C01', 'C01D0220250001', 6, 2, '연차 휴가 (12/10-12/11)', '{"기간": "2025-12-10 ~ 2025-12-11", "사유": "개인사유", "일수": 2}', '', 'E000003', '연차 2일', FALSE, '2025-12-05 09:00:00', '2025-12-05 08:50:00', '2025-12-07 16:00:00', 'FI'),
                                                                                                                                                                            (25, 'C01', 'C01D0220250002', 8, 3, '광주 출장 완료', '{"장소": "광주", "목적": "세미나 참석"}', '', 'E000003', '광주 출장', FALSE, '2025-12-01 09:00:00', '2025-12-01 08:50:00', '2025-12-03 14:00:00', 'FI'),
                                                                                                                                                                            (26, 'C01', 'C01D0220250003', 6, 2, '경조사 휴가 (11/25)', '{"기간": "2025-11-25", "사유": "결혼식", "일수": 1}', '', 'E000003', '경조사 1일', FALSE, '2025-11-20 09:00:00', '2025-11-20 08:50:00', '2025-11-22 16:00:00', 'FI'),
                                                                                                                                                                            (27, 'C01', 'C01D0220250004', 1, 1, '키보드 구매 완료', '{"품목": "기계식 키보드", "수량": 1, "예상금액": 150000}', '', 'E000003', '키보드 15만원', FALSE, '2025-11-15 09:00:00', '2025-11-15 08:50:00', '2025-11-17 10:00:00', 'FI');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 24, 'E000002', 1, 'A', '2025-12-05 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 24, 'E000001', 2, 'A', '2025-12-06 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 24, 'E000005', 3, 'A', '2025-12-07 16:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 25, 'E000002', 1, 'A', '2025-12-01 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 25, 'E000001', 2, 'A', '2025-12-02 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 25, 'E000005', 3, 'A', '2025-12-03 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 26, 'E000002', 1, 'A', '2025-11-20 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 26, 'E000001', 2, 'A', '2025-11-21 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 26, 'E000005', 3, 'A', '2025-11-22 16:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 27, 'E000002', 1, 'A', '2025-11-15 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 27, 'E000005', 2, 'A', '2025-11-17 10:00:00', TRUE, NULL, FALSE);

-- 반려 (RJ) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (28, 'C01', NULL, 6, 2, '장기 휴가 (반려)', '{"기간": "2026-03-01 ~ 2026-03-10", "사유": "해외여행", "일수": 10}', '', 'E000003', '장기휴가 10일', FALSE, '2025-12-10 09:00:00', '2025-12-10 08:50:00', '2025-12-10 14:00:00', 'RJ'),
                                                                                                                                                                            (29, 'C01', NULL, 8, 3, '유럽 출장 (반려)', '{"장소": "프랑스 파리", "목적": "박람회"}', '', 'E000003', '유럽 출장', FALSE, '2025-12-08 09:00:00', '2025-12-08 08:50:00', '2025-12-10 10:00:00', 'RJ'),
                                                                                                                                                                            (30, 'C01', NULL, 1, 1, '고급 모니터 (반려)', '{"품목": "울트라와이드 모니터", "수량": 1, "예상금액": 2000000}', '', 'E000003', '모니터 200만원', FALSE, '2025-12-05 09:00:00', '2025-12-05 08:50:00', '2025-12-08 16:00:00', 'RJ');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 28, 'E000002', 1, 'R', '2025-12-10 14:00:00', TRUE, '10일 연속 휴가는 승인 어렵습니다.', FALSE),
                                                                                                                          ('C01', 28, 'E000001', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 29, 'E000002', 1, 'A', '2025-12-09 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 29, 'E000001', 2, 'R', '2025-12-10 10:00:00', TRUE, '해외 출장 예산 초과입니다.', FALSE),
                                                                                                                          ('C01', 30, 'E000002', 1, 'A', '2025-12-06 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 30, 'E000001', 2, 'A', '2025-12-07 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 30, 'E000005', 3, 'R', '2025-12-08 16:00:00', TRUE, '장비 예산이 초과되었습니다.', FALSE);


-- ========================================================
-- E000002 김보안 (과장) - 15개 문서
-- 결재라인: 홍관리(차장) → 최부장(부장)
-- ========================================================

-- 임시저장 (US) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (31, 'C01', NULL, 6, 2, '(임시) 추석 휴가', '{"기간": "2026-09-15 ~ 2026-09-18"}', '', 'E000002', NULL, TRUE, NULL, '2025-12-24 12:00:00', '2025-12-24 12:00:00', 'US'),
                                                                                                                                                                            (32, 'C01', NULL, 8, 3, '(임시) 대전 출장', '{"장소": "대전"}', '', 'E000002', NULL, TRUE, NULL, '2025-12-24 12:10:00', '2025-12-24 12:10:00', 'US'),
                                                                                                                                                                            (33, 'C01', NULL, 1, 1, '(임시) 보안장비', '{"품목": "보안 USB"}', '', 'E000002', NULL, TRUE, NULL, '2025-12-24 12:20:00', '2025-12-24 12:20:00', 'US');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 31, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 31, 'E000005', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 32, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 33, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE);

-- 결재중 (AW) 5개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (34, 'C01', NULL, 6, 2, '연차 휴가 (3/1-3/2)', '{"기간": "2026-03-01 ~ 2026-03-02", "일수": 2}', '', 'E000002', '연차 2일', FALSE, '2025-12-24 09:20:00', '2025-12-24 09:10:00', '2025-12-24 09:20:00', 'AW'),
                                                                                                                                                                            (35, 'C01', NULL, 8, 3, '세종 출장 신청', '{"장소": "세종시", "목적": "정부기관 미팅"}', '', 'E000002', '세종 출장', FALSE, '2025-12-23 09:20:00', '2025-12-23 09:10:00', '2025-12-23 16:00:00', 'AW'),
                                                                                                                                                                            (36, 'C01', NULL, 6, 2, '오후 반차 (3/5)', '{"기간": "2026-03-05 오후"}', '', 'E000002', '오후 반차', FALSE, '2025-12-22 09:20:00', '2025-12-22 09:10:00', '2025-12-23 12:00:00', 'AW'),
                                                                                                                                                                            (37, 'C01', NULL, 1, 1, '보안 소프트웨어 구매', '{"품목": "백신 프로그램", "수량": 50, "예상금액": 2500000}', '', 'E000002', '백신 250만원', FALSE, '2025-12-21 09:20:00', '2025-12-21 09:10:00', '2025-12-23 18:00:00', 'AW'),
                                                                                                                                                                            (38, 'C01', NULL, 1, 1, '보안 교육 자료', '{"품목": "교육 책자", "수량": 100, "예상금액": 200000}', '', 'E000002', '책자 20만원', FALSE, '2025-12-24 13:00:00', '2025-12-24 12:50:00', '2025-12-24 13:00:00', 'AW');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
-- 문서 34: 1번째 차례
('C01', 34, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE),
('C01', 34, 'E000005', 2, 'W', NULL, FALSE, NULL, FALSE),
-- 문서 35: 2번째(마지막) 차례
('C01', 35, 'E000001', 1, 'A', '2025-12-23 16:00:00', TRUE, NULL, FALSE),
('C01', 35, 'E000005', 2, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 36: 2번째(마지막) 차례
('C01', 36, 'E000001', 1, 'A', '2025-12-23 12:00:00', TRUE, NULL, FALSE),
('C01', 36, 'E000005', 2, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 37: 2번째(마지막) 차례
('C01', 37, 'E000001', 1, 'A', '2025-12-23 18:00:00', TRUE, NULL, FALSE),
('C01', 37, 'E000005', 2, 'I', NULL, FALSE, NULL, FALSE),
-- 문서 38: 1명만
('C01', 38, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE);

-- 최종승인 (FI) 4개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (39, 'C01', 'C01D0320250001', 6, 2, '연차 휴가 (11/20-11/21)', '{"기간": "2025-11-20 ~ 2025-11-21", "일수": 2}', '', 'E000002', '연차 2일', FALSE, '2025-11-15 09:00:00', '2025-11-15 08:50:00', '2025-11-18 16:00:00', 'FI'),
                                                                                                                                                                            (40, 'C01', 'C01D0320250002', 8, 3, '수원 출장 완료', '{"장소": "수원", "목적": "협력사 미팅"}', '', 'E000002', '수원 출장', FALSE, '2025-11-10 09:00:00', '2025-11-10 08:50:00', '2025-11-12 14:00:00', 'FI'),
                                                                                                                                                                            (41, 'C01', 'C01D0320250003', 6, 2, '오전 반차 (11/05)', '{"기간": "2025-11-05 오전"}', '', 'E000002', '오전 반차', FALSE, '2025-11-01 09:00:00', '2025-11-01 08:50:00', '2025-11-03 10:00:00', 'FI'),
                                                                                                                                                                            (42, 'C01', 'C01D0320250004', 1, 1, '보안 장비 구매', '{"품목": "보안 카메라", "수량": 5, "예상금액": 1000000}', '', 'E000002', '카메라 100만원', FALSE, '2025-10-25 09:00:00', '2025-10-25 08:50:00', '2025-10-28 16:00:00', 'FI');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 39, 'E000001', 1, 'A', '2025-11-16 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 39, 'E000005', 2, 'A', '2025-11-18 16:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 40, 'E000001', 1, 'A', '2025-11-11 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 40, 'E000005', 2, 'A', '2025-11-12 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 41, 'E000001', 1, 'A', '2025-11-02 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 41, 'E000005', 2, 'A', '2025-11-03 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 42, 'E000001', 1, 'A', '2025-10-26 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 42, 'E000005', 2, 'A', '2025-10-28 16:00:00', TRUE, NULL, FALSE);

-- 반려 (RJ) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (43, 'C01', NULL, 6, 2, '장기 휴가 (반려)', '{"기간": "2026-04-01 ~ 2026-04-14", "일수": 14}', '', 'E000002', '장기휴가 14일', FALSE, '2025-12-01 09:00:00', '2025-12-01 08:50:00', '2025-12-01 14:00:00', 'RJ'),
                                                                                                                                                                            (44, 'C01', NULL, 8, 3, '미국 출장 (반려)', '{"장소": "미국 뉴욕", "목적": "컨퍼런스"}', '', 'E000002', '미국 출장', FALSE, '2025-11-25 09:00:00', '2025-11-25 08:50:00', '2025-11-27 10:00:00', 'RJ'),
                                                                                                                                                                            (45, 'C01', NULL, 1, 1, '서버 구매 (반려)', '{"품목": "서버 장비", "수량": 1, "예상금액": 50000000}', '', 'E000002', '서버 5000만원', FALSE, '2025-11-20 09:00:00', '2025-11-20 08:50:00', '2025-11-22 16:00:00', 'RJ');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 43, 'E000001', 1, 'R', '2025-12-01 14:00:00', TRUE, '14일 휴가는 승인 불가합니다.', FALSE),
                                                                                                                          ('C01', 43, 'E000005', 2, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 44, 'E000001', 1, 'A', '2025-11-26 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 44, 'E000005', 2, 'R', '2025-11-27 10:00:00', TRUE, '해외 출장 예산이 없습니다.', FALSE),
                                                                                                                          ('C01', 45, 'E000001', 1, 'A', '2025-11-21 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 45, 'E000005', 2, 'R', '2025-11-22 16:00:00', TRUE, '고가 장비는 내년 예산으로 검토바랍니다.', FALSE);


-- ========================================================
-- E000001 홍관리 (차장) - 15개 문서
-- 결재라인: 최부장(부장)
-- ========================================================

-- 임시저장 (US) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (46, 'C01', NULL, 6, 2, '(임시) 하계 휴가', '{"기간": "2026-07-20 ~ 2026-07-24"}', '', 'E000001', NULL, TRUE, NULL, '2025-12-24 13:00:00', '2025-12-24 13:00:00', 'US'),
                                                                                                                                                                            (47, 'C01', NULL, 8, 3, '(임시) 울산 출장', '{"장소": "울산"}', '', 'E000001', NULL, TRUE, NULL, '2025-12-24 13:10:00', '2025-12-24 13:10:00', 'US'),
                                                                                                                                                                            (48, 'C01', NULL, 1, 1, '(임시) 회의실 장비', '{"품목": "프로젝터"}', '', 'E000001', NULL, TRUE, NULL, '2025-12-24 13:20:00', '2025-12-24 13:20:00', 'US');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 46, 'E000005', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 47, 'E000005', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 48, 'E000005', 1, 'W', NULL, FALSE, NULL, FALSE);

-- 결재중 (AW) 5개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (49, 'C01', NULL, 6, 2, '연차 휴가 (4/1-4/3)', '{"기간": "2026-04-01 ~ 2026-04-03", "일수": 3}', '', 'E000001', '연차 3일', FALSE, '2025-12-24 09:30:00', '2025-12-24 09:20:00', '2025-12-24 09:30:00', 'AW'),
                                                                                                                                                                            (50, 'C01', NULL, 8, 3, '창원 출장 신청', '{"장소": "창원", "목적": "공장 점검"}', '', 'E000001', '창원 출장', FALSE, '2025-12-23 09:30:00', '2025-12-23 09:20:00', '2025-12-23 09:30:00', 'AW'),
                                                                                                                                                                            (51, 'C01', NULL, 6, 2, '오전 반차 (4/10)', '{"기간": "2026-04-10 오전"}', '', 'E000001', '오전 반차', FALSE, '2025-12-22 09:30:00', '2025-12-22 09:20:00', '2025-12-22 09:30:00', 'AW'),
                                                                                                                                                                            (52, 'C01', NULL, 1, 1, '회의 테이블 구매', '{"품목": "대형 테이블", "수량": 1, "예상금액": 3000000}', '', 'E000001', '테이블 300만원', FALSE, '2025-12-21 09:30:00', '2025-12-21 09:20:00', '2025-12-21 09:30:00', 'AW'),
                                                                                                                                                                            (53, 'C01', NULL, 1, 1, '의자 구매', '{"품목": "회의용 의자", "수량": 10, "예상금액": 1000000}', '', 'E000001', '의자 100만원', FALSE, '2025-12-24 14:00:00', '2025-12-24 13:50:00', '2025-12-24 14:00:00', 'AW');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 49, 'E000005', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 50, 'E000005', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 51, 'E000005', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 52, 'E000005', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 53, 'E000005', 1, 'I', NULL, FALSE, NULL, FALSE);

-- 최종승인 (FI) 4개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (54, 'C01', 'C01D0420250001', 6, 2, '연차 휴가 (10/15-10/17)', '{"기간": "2025-10-15 ~ 2025-10-17", "일수": 3}', '', 'E000001', '연차 3일', FALSE, '2025-10-10 09:00:00', '2025-10-10 08:50:00', '2025-10-12 16:00:00', 'FI'),
                                                                                                                                                                            (55, 'C01', 'C01D0420250002', 8, 3, '대구 출장 완료', '{"장소": "대구", "목적": "지사 방문"}', '', 'E000001', '대구 출장', FALSE, '2025-10-05 09:00:00', '2025-10-05 08:50:00', '2025-10-07 14:00:00', 'FI'),
                                                                                                                                                                            (56, 'C01', 'C01D0420250003', 6, 2, '오후 반차 (10/01)', '{"기간": "2025-10-01 오후"}', '', 'E000001', '오후 반차', FALSE, '2025-09-28 09:00:00', '2025-09-28 08:50:00', '2025-09-30 10:00:00', 'FI'),
                                                                                                                                                                            (57, 'C01', 'C01D0420250004', 1, 1, '사무실 리모델링', '{"품목": "인테리어 공사", "예상금액": 10000000}', '', 'E000001', '리모델링 1000만원', FALSE, '2025-09-20 09:00:00', '2025-09-20 08:50:00', '2025-09-25 16:00:00', 'FI');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 54, 'E000005', 1, 'A', '2025-10-12 16:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 55, 'E000005', 1, 'A', '2025-10-07 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 56, 'E000005', 1, 'A', '2025-09-30 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 57, 'E000005', 1, 'A', '2025-09-25 16:00:00', TRUE, NULL, FALSE);

-- 반려 (RJ) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (58, 'C01', NULL, 6, 2, '장기 휴가 (반려)', '{"기간": "2026-05-01 ~ 2026-05-20", "일수": 20}', '', 'E000001', '장기휴가 20일', FALSE, '2025-11-01 09:00:00', '2025-11-01 08:50:00', '2025-11-02 14:00:00', 'RJ'),
                                                                                                                                                                            (59, 'C01', NULL, 8, 3, '해외 연수 (반려)', '{"장소": "독일", "목적": "기술 연수", "기간": "2026-06-01~14"}', '', 'E000001', '독일 연수 2주', FALSE, '2025-10-20 09:00:00', '2025-10-20 08:50:00', '2025-10-22 10:00:00', 'RJ'),
                                                                                                                                                                            (60, 'C01', NULL, 1, 1, '차량 구매 (반려)', '{"품목": "법인 차량", "수량": 1, "예상금액": 80000000}', '', 'E000001', '법인차 8000만원', FALSE, '2025-10-15 09:00:00', '2025-10-15 08:50:00', '2025-10-17 16:00:00', 'RJ');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 58, 'E000005', 1, 'R', '2025-11-02 14:00:00', TRUE, '20일 휴가는 승인 불가합니다.', FALSE),
                                                                                                                          ('C01', 59, 'E000005', 1, 'R', '2025-10-22 10:00:00', TRUE, '해외 연수 예산이 없습니다.', FALSE),
                                                                                                                          ('C01', 60, 'E000005', 1, 'R', '2025-10-17 16:00:00', TRUE, '차량 구매는 내년도 예산으로 검토바랍니다.', FALSE);


-- ========================================================
-- E000005 최부장 (부장) - 15개 문서
-- 결재라인: 없음 (CEO가 없어서 본인이 최종 결재권자이므로 결재라인 1명도 가능)
-- 여기서는 홍관리(차장)를 형식적 결재자로 설정
-- ========================================================

-- 임시저장 (US) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (61, 'C01', NULL, 6, 2, '(임시) 연말 휴가', '{"기간": "2025-12-30 ~ 2025-12-31"}', '', 'E000005', NULL, TRUE, NULL, '2025-12-24 14:00:00', '2025-12-24 14:00:00', 'US'),
                                                                                                                                                                            (62, 'C01', NULL, 8, 3, '(임시) 본사 출장', '{"장소": "본사"}', '', 'E000005', NULL, TRUE, NULL, '2025-12-24 14:10:00', '2025-12-24 14:10:00', 'US'),
                                                                                                                                                                            (63, 'C01', NULL, 1, 1, '(임시) 경영진 회의 준비', '{"품목": "회의 물품"}', '', 'E000005', NULL, TRUE, NULL, '2025-12-24 14:20:00', '2025-12-24 14:20:00', 'US');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 61, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 62, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 63, 'E000001', 1, 'W', NULL, FALSE, NULL, FALSE);

-- 결재중 (AW) 5개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (64, 'C01', NULL, 6, 2, '연차 휴가 (5/1-5/3)', '{"기간": "2026-05-01 ~ 2026-05-03", "일수": 3}', '', 'E000005', '연차 3일', FALSE, '2025-12-24 09:40:00', '2025-12-24 09:30:00', '2025-12-24 09:40:00', 'AW'),
                                                                                                                                                                            (65, 'C01', NULL, 8, 3, '제주 워크샵', '{"장소": "제주도", "목적": "경영진 워크샵"}', '', 'E000005', '제주 워크샵', FALSE, '2025-12-23 09:40:00', '2025-12-23 09:30:00', '2025-12-23 09:40:00', 'AW'),
                                                                                                                                                                            (66, 'C01', NULL, 6, 2, '오전 반차 (5/10)', '{"기간": "2026-05-10 오전"}', '', 'E000005', '오전 반차', FALSE, '2025-12-22 09:40:00', '2025-12-22 09:30:00', '2025-12-22 09:40:00', 'AW'),
                                                                                                                                                                            (67, 'C01', NULL, 1, 1, '임원 회의 비용', '{"품목": "회의 케이터링", "예상금액": 500000}', '', 'E000005', '케이터링 50만원', FALSE, '2025-12-21 09:40:00', '2025-12-21 09:30:00', '2025-12-21 09:40:00', 'AW'),
                                                                                                                                                                            (68, 'C01', NULL, 1, 1, '경영 컨설팅 비용', '{"품목": "컨설팅", "예상금액": 20000000}', '', 'E000005', '컨설팅 2000만원', FALSE, '2025-12-24 15:00:00', '2025-12-24 14:50:00', '2025-12-24 15:00:00', 'AW');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 64, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 65, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 66, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 67, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE),
                                                                                                                          ('C01', 68, 'E000001', 1, 'I', NULL, FALSE, NULL, FALSE);

-- 최종승인 (FI) 4개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (69, 'C01', 'C01D0120250010', 6, 2, '연차 휴가 (9/1-9/3)', '{"기간": "2025-09-01 ~ 2025-09-03", "일수": 3}', '', 'E000005', '연차 3일', FALSE, '2025-08-25 09:00:00', '2025-08-25 08:50:00', '2025-08-27 16:00:00', 'FI'),
                                                                                                                                                                            (70, 'C01', 'C01D0120250011', 8, 3, '싱가포르 출장', '{"장소": "싱가포르", "목적": "해외 파트너 미팅"}', '', 'E000005', '싱가포르 출장', FALSE, '2025-08-20 09:00:00', '2025-08-20 08:50:00', '2025-08-22 14:00:00', 'FI'),
                                                                                                                                                                            (71, 'C01', 'C01D0120250012', 6, 2, '오후 반차 (8/15)', '{"기간": "2025-08-15 오후"}', '', 'E000005', '오후 반차', FALSE, '2025-08-10 09:00:00', '2025-08-10 08:50:00', '2025-08-12 10:00:00', 'FI'),
                                                                                                                                                                            (72, 'C01', 'C01D0120250013', 1, 1, '전사 워크샵 비용', '{"품목": "워크샵 비용", "예상금액": 30000000}', '', 'E000005', '워크샵 3000만원', FALSE, '2025-08-01 09:00:00', '2025-08-01 08:50:00', '2025-08-05 16:00:00', 'FI');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 69, 'E000001', 1, 'A', '2025-08-27 16:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 70, 'E000001', 1, 'A', '2025-08-22 14:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 71, 'E000001', 1, 'A', '2025-08-12 10:00:00', TRUE, NULL, FALSE),
                                                                                                                          ('C01', 72, 'E000001', 1, 'A', '2025-08-05 16:00:00', TRUE, NULL, FALSE);

-- 반려 (RJ) 3개
INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, submitted_at, created_at, updated_at, doc_stat) VALUES
                                                                                                                                                                            (73, 'C01', NULL, 6, 2, '장기 해외 휴가 (반려)', '{"기간": "2026-06-01 ~ 2026-06-30", "일수": 30}', '', 'E000005', '장기휴가 30일', FALSE, '2025-09-01 09:00:00', '2025-09-01 08:50:00', '2025-09-02 14:00:00', 'RJ'),
                                                                                                                                                                            (74, 'C01', NULL, 8, 3, '해외 지사 설립 출장 (반려)', '{"장소": "베트남", "목적": "지사 설립", "기간": "2026-07-01~30"}', '', 'E000005', '베트남 1개월', FALSE, '2025-08-15 09:00:00', '2025-08-15 08:50:00', '2025-08-17 10:00:00', 'RJ'),
                                                                                                                                                                            (75, 'C01', NULL, 1, 1, '전용기 구매 (반려)', '{"품목": "전용기", "수량": 1, "예상금액": 5000000000}', '', 'E000005', '전용기 50억', FALSE, '2025-08-10 09:00:00', '2025-08-10 08:50:00', '2025-08-12 16:00:00', 'RJ');

INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at, is_actual_appr, rej_reason, is_delegate) VALUES
                                                                                                                          ('C01', 73, 'E000001', 1, 'R', '2025-09-02 14:00:00', TRUE, '30일 휴가는 승인 불가합니다.', FALSE),
                                                                                                                          ('C01', 74, 'E000001', 1, 'R', '2025-08-17 10:00:00', TRUE, '지사 설립은 이사회 결의가 필요합니다.', FALSE),
                                                                                                                          ('C01', 75, 'E000001', 1, 'R', '2025-08-12 16:00:00', TRUE, '예산 범위를 초과합니다.', FALSE);



SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================
-- 데이터 확인 쿼리
-- ========================================================
SELECT '=== 사원별 문서 현황 ===' AS info;
SELECT e.emp_name, e.emp_id,
       SUM(CASE WHEN d.doc_stat = 'US' THEN 1 ELSE 0 END) as 임시저장,
       SUM(CASE WHEN d.doc_stat = 'AW' THEN 1 ELSE 0 END) as 결재중,
       SUM(CASE WHEN d.doc_stat = 'FI' THEN 1 ELSE 0 END) as 최종승인,
       SUM(CASE WHEN d.doc_stat = 'RJ' THEN 1 ELSE 0 END) as 반려,
       COUNT(*) as 총합
FROM employee e
         LEFT JOIN document d ON e.emp_id = d.emp_id
WHERE e.emp_id IN ('E000001', 'E000002', 'E000003', 'E000004', 'E000005')
GROUP BY e.emp_id, e.emp_name
ORDER BY e.emp_id;

SELECT '=== 결재라인 상태별 현황 ===' AS info;
SELECT appr_stat, COUNT(*) as cnt FROM approval_line GROUP BY appr_stat;

SELECT '=== 사원별 결재 대기 문서 (I 상태) ===' AS info;
SELECT e.emp_name, e.emp_id, COUNT(al.apprl_no) as 결재대기
FROM employee e
         JOIN approval_line al ON e.emp_id = al.emp_id
WHERE al.appr_stat = 'I'
GROUP BY e.emp_id, e.emp_name
ORDER BY e.emp_id;

SELECT '=== 근태 데이터 현황 ===' AS info;
SELECT e.emp_name, a.doc_id, a.type, a.day, a.created_at, a.ended_at
FROM attendance a
         JOIN employee e ON a.emp_id = e.emp_id
ORDER BY a.emp_id, a.created_at;
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================
-- 데이터 확인 쿼리
-- ========================================================
SELECT '=== 문서 현황 ===' AS info;
SELECT doc_stat, COUNT(*) as cnt FROM document GROUP BY doc_stat;

SELECT '=== 결재라인 현황 ===' AS info;
SELECT appr_stat, COUNT(*) as cnt FROM approval_line GROUP BY appr_stat;

SELECT '=== 사원별 작성 문서 ===' AS info;
SELECT e.emp_name, e.emp_id, COUNT(d.doc_no) as doc_cnt
FROM employee e
         LEFT JOIN document d ON e.emp_id = d.emp_id
GROUP BY e.emp_id, e.emp_name
ORDER BY doc_cnt DESC;

-- prov_document (5)
INSERT INTO prov_document
(prov_no, com_id, doc_title, description, is_public, file_name, file_url, file_size, chunk_cnt, proc_stat, error_msg)
VALUES
    (1,'C01','취업규칙','요약본', TRUE, 'rule.pdf','/s3/prov/rule.pdf', 500000, 20, 'DONE', NULL),
    (2,'C01','보안규정','사내 보안', TRUE, 'sec.pdf','/s3/prov/sec.pdf', 300000, 15, 'DONE', NULL),
    (3,'C01','개인정보처리','PIPA', TRUE, 'pipa.pdf','/s3/prov/pipa.pdf', 250000, 12, 'DONE', NULL),
    (4,'C01','비상대응','DR', TRUE, 'dr.pdf','/s3/prov/dr.pdf', 200000, 10, 'DONE', NULL),
    (5,'C01','복무규정','근태', TRUE, 'hr.pdf','/s3/prov/hr.pdf', 180000,  9, 'DONE', NULL);

-- ========================================================
-- meeting / meeting_emp / chat / system_logs (각 5)
-- ========================================================
INSERT INTO meeting
(meet_no, com_id, dep_no, emp_id, title, stt_text, ai_text, started_at, is_deleted)
VALUES
    (1,'C01',1,'E000001','개발 주간회의','회의 텍스트1','요약1','2025-12-17 10:00:00',FALSE),
    (2,'C01',2,'E000002','보안 점검회의','회의 텍스트2','요약2','2025-12-17 11:00:00',FALSE),
    (3,'C01',3,'E000003','채용 회의','회의 텍스트3','요약3','2025-12-17 13:00:00',FALSE),
    (4,'C01',4,'E000004','총무 정산회의','회의 텍스트4','요약4','2025-12-17 14:00:00',FALSE),
    (5,'C01',5,'E000001','영업 전략회의','회의 텍스트5','요약5','2025-12-17 15:00:00',FALSE);

INSERT INTO meeting_emp (meemp_no, com_id, emp_id, meet_no) VALUES
                                                                (1,'C01','E000002',1),
                                                                (2,'C01','E000003',1),
                                                                (3,'C01','E000004',2),
                                                                (4,'C01','E000001',3),
                                                                (5,'C01','E000002',5);

INSERT INTO chat_rooms (room_no, room_name, room_type) VALUES
                                                           (1,'전체 공지','GROUP'),
                                                           (2,'개발팀','GROUP'),
                                                           (3,'보안팀','GROUP'),
                                                           (4,'1:1-홍관리-이사원','ONE'),
                                                           (5,'임원방','GROUP');

INSERT INTO chat_room_members (romem_no, room_no, emp_id, joined_at, last_read_msg_id) VALUES
                                                                                           (1,1,'E000001', NOW(), NULL),
                                                                                           (2,2,'E000004', NOW(), 'mongo_msg_101'),
                                                                                           (3,3,'E000002', NOW(), 'mongo_msg_202'),
                                                                                           (4,4,'E000001', NOW(), 'mongo_msg_303'),
                                                                                           (5,4,'E000004', NOW(), 'mongo_msg_303');

INSERT INTO system_logs (log_no, com_id, level, message) VALUES
                                                             (1,'C01','INFO','RAG 요청 성공'),
                                                             (2,'C01','WARN','RAG 응답 지연'),
                                                             (3,'C01','ERROR','RAG 파싱 실패'),
                                                             (4,'SYS','INFO','시스템 점검 완료'),
                                                             (5,'C01','INFO','문서 요약 생성');

-- ========================================================
-- reservation (각 5)
-- ========================================================
INSERT INTO meeting_room (room_no, com_id, room_name, cap, loc, img_url, equip_list, note) VALUES
                                                                                               (1,'C01','A회의실',8,'본사 3층',NULL,'TV,화이트보드',''),
                                                                                               (2,'C01','B회의실',4,'본사 3층',NULL,'모니터',''),
                                                                                               (3,'C01','C회의실',12,'본사 4층',NULL,'프로젝터',''),
                                                                                               (4,'C01','D회의실',6,'본사 4층',NULL,'TV',''),
                                                                                               (5,'C01','E회의실',10,'본사 5층',NULL,'프로젝터,화이트보드','');

INSERT INTO corporate_car (car_no, com_id, car_name, car_type, plate_no, cap, fuel, img_url) VALUES
                                                                                                 (1,'C01','쏘나타','SEDAN','12가3456',4,'GAS',NULL),
                                                                                                 (2,'C01','카니발','VAN','34나5678',7,'GAS',NULL),
                                                                                                 (3,'C01','아반떼','SEDAN','56다7890',4,'GAS',NULL),
                                                                                                 (4,'C01','스타리아','VAN','78라1234',9,'DIESEL',NULL),
                                                                                                 (5,'C01','아이오닉','EV','90마5678',4,'EV',NULL);

INSERT INTO shared_equipment (eq_no, com_id, eq_name, eq_id, model_name, img_url, loc) VALUES
                                                                                           (1,'C01','노트북','EQ-NB-01','LG Gram',NULL,'자산창고'),
                                                                                           (2,'C01','빔프로젝터','EQ-PJ-01','Epson',NULL,'총무실'),
                                                                                           (3,'C01','카메라','EQ-CM-01','Sony',NULL,'자산창고'),
                                                                                           (4,'C01','마이크','EQ-MC-01','Shure',NULL,'회의실'),
                                                                                           (5,'C01','태블릿','EQ-TB-01','iPad',NULL,'자산창고');

INSERT INTO meeting_room_reservation
(meeting_resv_no, com_id, room_no, started_at, ended_at, resv_emp, purp, is_deleted)
VALUES
    (1,'C01',1,'2025-12-18 10:00:00','2025-12-18 11:00:00','E000001','주간회의',FALSE),
    (2,'C01',2,'2025-12-18 11:10:00','2025-12-18 12:00:00','E000002','보안점검',FALSE),
    (3,'C01',3,'2025-12-18 13:00:00','2025-12-18 14:00:00','E000003','채용면접',FALSE),
    (4,'C01',4,'2025-12-18 14:10:00','2025-12-18 15:00:00','E000004','회의',FALSE),
    (5,'C01',5,'2025-12-18 15:10:00','2025-12-18 16:00:00','E000001','고객미팅',FALSE);

INSERT INTO meeting_room_attendee (atte_no, com_id, meeting_resv_no, emp_id) VALUES
                                                                                 (1,'C01',1,'E000002'),
                                                                                 (2,'C01',2,'E000001'),
                                                                                 (3,'C01',3,'E000004'),
                                                                                 (4,'C01',4,'E000001'),
                                                                                 (5,'C01',5,'E000003');

INSERT INTO corporate_car_reservation
(car_resv_no, com_id, car_no, started_at, ended_at, resv_emp, purp, is_deleted)
VALUES
    (1,'C01',1,'2025-12-19 09:00:00','2025-12-19 12:00:00','E000004','외근',FALSE),
    (2,'C01',2,'2025-12-19 13:00:00','2025-12-19 15:00:00','E000001','출장',FALSE),
    (3,'C01',3,'2025-12-20 10:00:00','2025-12-20 12:00:00','E000002','점검',FALSE),
    (4,'C01',4,'2025-12-20 13:00:00','2025-12-20 16:00:00','E000003','면접',FALSE),
    (5,'C01',5,'2025-12-21 09:00:00','2025-12-21 11:00:00','E000001','회의',FALSE);

INSERT INTO shared_equipment_reservation
(eq_resv_no, com_id, eq_no, started_at, ended_at, resv_emp, purp, is_deleted)
VALUES
    (1,'C01',1,'2025-12-19 10:00:00','2025-12-19 18:00:00','E000004','개발',FALSE),
    (2,'C01',2,'2025-12-20 09:00:00','2025-12-20 11:00:00','E000001','발표',FALSE),
    (3,'C01',3,'2025-12-20 13:00:00','2025-12-20 15:00:00','E000002','촬영',FALSE),
    (4,'C01',4,'2025-12-21 09:00:00','2025-12-21 10:00:00','E000003','회의',FALSE),
    (5,'C01',5,'2025-12-21 14:00:00','2025-12-21 17:00:00','E000001','점검',FALSE);

-- ========================================================
-- drive (folder/file/folder_permission) (각 5)
-- ========================================================
INSERT INTO folder
(folder_no, com_id, dep_no, parent_id, folder_name, owner_id, scope, path)
VALUES
    (1,'C01',NULL,NULL,'ROOT','E000001','all','/C01'),
    (2,'C01',1,1,'개발팀','E000001','dept','/C01/dev'),
    (3,'C01',2,1,'보안팀','E000002','dept','/C01/sec'),
    (4,'C01',NULL,1,'개인폴더','E000004','prvt','/C01/users/E000004'),
    (5,'C01',5,1,'영업팀','E000003','dept','/C01/sales');

INSERT INTO `file` (file_no, com_id, folder_no, file_name, size, emp_id, path) VALUES
                                                                                   (1,'C01',2,'api_spec.md', 4096,'E000004','/C01/dev/api_spec.md'),
                                                                                   (2,'C01',3,'security_checklist.xlsx', 20480,'E000002','/C01/sec/checklist.xlsx'),
                                                                                   (3,'C01',4,'memo.txt', 512,'E000004','/C01/users/E000004/memo.txt'),
                                                                                   (4,'C01',5,'proposal.pptx', 102400,'E000003','/C01/sales/proposal.pptx'),
                                                                                   (5,'C01',1,'readme.md', 2048,'E000001','/C01/readme.md');

INSERT INTO folder_permission (perm_no, com_id, folder_no, emp_id, perm_type) VALUES
                                                                                  (1,'C01',2,'E000001','W'),
                                                                                  (2,'C01',2,'E000004','W'),
                                                                                  (3,'C01',3,'E000002','W'),
                                                                                  (4,'C01',5,'E000003','W'),
                                                                                  (5,'C01',1,'E000003','R');

-- ========================================================
-- board (각 5)
-- ========================================================
INSERT INTO board_cat (board_cat_no, cat_code, cat_descript) VALUES
                                                                 (1,'N','공지'),
                                                                 (2,'F','자유'),
                                                                 (3,'Q','질문'),
                                                                 (4,'P','프로젝트'),
                                                                 (5,'E','이벤트');

INSERT INTO notice
(notice_no, com_id, is_deleted, title, contents, is_popup, started_at, ended_at, emp_id, rating)
VALUES
    (1,'C01',FALSE,'시스템 점검 안내', JSON_OBJECT('text','금요일 22시 점검'), TRUE, '2025-12-20 22:00:00','2025-12-21 01:00:00','E000001',10),
    (2,'C01',FALSE,'연말 일정',       JSON_OBJECT('text','연말 휴무 확인'), FALSE,NULL,NULL,'E000003',3),
    (3,'C01',FALSE,'보안 교육',       JSON_OBJECT('text','필수 교육 수강'), FALSE,NULL,NULL,'E000002',5),
    (4,'C01',FALSE,'회의실 이용',     JSON_OBJECT('text','예약 규정 준수'), FALSE,NULL,NULL,'E000004',1),
    (5,'C01',FALSE,'영업 행사',       JSON_OBJECT('text','전시회 참가'), FALSE,NULL,NULL,'E000001',2);

INSERT INTO notice_attach (notice_attach_no, com_id, notice_no, org_name, folder_path) VALUES
                                                                                           (1,'C01',1,'maintenance.pdf','/drive/C01/notice/maintenance.pdf'),
                                                                                           (2,'C01',2,'calendar.png','/drive/C01/notice/calendar.png'),
                                                                                           (3,'C01',3,'security_training.pdf','/drive/C01/notice/security_training.pdf'),
                                                                                           (4,'C01',4,'room_policy.pdf','/drive/C01/notice/room_policy.pdf'),
                                                                                           (5,'C01',5,'event_info.pdf','/drive/C01/notice/event_info.pdf');

INSERT INTO board
(board_no, com_id, is_deleted, title, contents, cat_code, emp_id, rating)
VALUES
    (1,'C01',FALSE,'자유글1', JSON_OBJECT('text','안녕하세요'), 'F','E000004',0),
    (2,'C01',FALSE,'질문1',   JSON_OBJECT('text','FK 에러 왜 나요?'), 'Q','E000003',2),
    (3,'C01',FALSE,'프로젝트 공유', JSON_OBJECT('text','ERD 공유합니다'), 'P','E000001',5),
    (4,'C01',FALSE,'이벤트 공지', JSON_OBJECT('text','회식합니다'), 'E','E000003',1),
    (5,'C01',FALSE,'자유글2', JSON_OBJECT('text','오늘 점심 뭐먹지'), 'F','E000002',0);

INSERT INTO comment (comment_no, com_id, board_no, contents, emp_id) VALUES
                                                                         (1,'C01',2,'company에 com_id 먼저 넣어야 해요','E000001'),
                                                                         (2,'C01',2,'공백/길이 확인해보세요','E000002'),
                                                                         (3,'C01',3,'자료 감사합니다','E000003'),
                                                                         (4,'C01',1,'환영합니다~','E000004'),
                                                                         (5,'C01',4,'참석합니다','E000001');

INSERT INTO board_attach (board_attach_no, com_id, board_no, org_name, folder_path) VALUES
                                                                                        (1,'C01',1,'hello.png','/drive/C01/board/hello.png'),
                                                                                        (2,'C01',2,'fk_tip.pdf','/drive/C01/board/fk_tip.pdf'),
                                                                                        (3,'C01',3,'erd.png','/drive/C01/board/erd.png'),
                                                                                        (4,'C01',4,'party.jpg','/drive/C01/board/party.jpg'),
                                                                                        (5,'C01',5,'lunch.txt','/drive/C01/board/lunch.txt');

-- ========================================================
-- payment / attendance (각 5)
-- ========================================================
INSERT INTO payment_method
(paym_no, com_id, paym_type, card_type, billing_key, mask, active)
VALUES
    (1,'C01','C','VISA','billkey_c01_visa_001','****-****-****-1111',TRUE),
    (2,'C01','C','MASTER','billkey_c01_mc_002','****-****-****-2222',TRUE),
    (3,'C01','A',NULL,'billkey_c01_acc_003',NULL,TRUE),
    (4,'SYS','A',NULL,'billkey_sys_acc_004',NULL,TRUE),
    (5,'C01','C','AMEX','billkey_c01_amex_005','****-****-****-5555',FALSE);

INSERT INTO payment_history
(payh_no, com_id, amount, pay_result, paym_no, created_at)
VALUES
    (1,'C01',  9900, TRUE,  1, '2025-12-01 10:00:00'),
    (2,'C01', 19900, TRUE,  2, '2025-12-02 10:00:00'),
    (3,'C01',  9900, FALSE, 3, '2025-12-03 10:00:00'),
    (4,'SYS',     0, TRUE,  4, '2025-12-04 10:00:00'),
    (5,'C01',  9900, TRUE,  1, '2025-12-05 10:00:00');

-- ========================================================
-- attendance (근태) 데이터 - 최종승인된 휴가/출장 문서 기반
-- ========================================================
INSERT INTO attendance (com_id, emp_id, doc_id, type, day, delegate, created_at, ended_at) VALUES
-- E000004 이사원
('C01', 'E000004', 'C01D0120250001', 'V', 3, NULL, '2025-12-20 09:00:00', '2025-12-22 18:00:00'),
('C01', 'E000004', 'C01D0120250002', 'B', 2, NULL, '2025-12-15 09:00:00', '2025-12-16 18:00:00'),
('C01', 'E000004', 'C01D0120250003', 'V', 1, NULL, '2025-12-05 09:00:00', '2025-12-05 13:00:00'),
-- E000003 박삼차
('C01', 'E000003', 'C01D0220250001', 'V', 2, NULL, '2025-12-10 09:00:00', '2025-12-11 18:00:00'),
('C01', 'E000003', 'C01D0220250002', 'B', 1, NULL, '2025-12-01 09:00:00', '2025-12-01 18:00:00'),
('C01', 'E000003', 'C01D0220250003', 'V', 1, NULL, '2025-11-25 09:00:00', '2025-11-25 18:00:00'),
-- E000002 김보안
('C01', 'E000002', 'C01D0320250001', 'V', 2, NULL, '2025-11-20 09:00:00', '2025-11-21 18:00:00'),
('C01', 'E000002', 'C01D0320250002', 'B', 1, NULL, '2025-11-10 09:00:00', '2025-11-10 18:00:00'),
('C01', 'E000002', 'C01D0320250003', 'V', 1, NULL, '2025-11-05 09:00:00', '2025-11-05 13:00:00'),
-- E000001 홍관리
('C01', 'E000001', 'C01D0420250001', 'V', 3, NULL, '2025-10-15 09:00:00', '2025-10-17 18:00:00'),
('C01', 'E000001', 'C01D0420250002', 'B', 1, NULL, '2025-10-05 09:00:00', '2025-10-05 18:00:00'),
('C01', 'E000001', 'C01D0420250003', 'V', 1, NULL, '2025-10-01 13:00:00', '2025-10-01 18:00:00'),
-- E000005 최부장
('C01', 'E000005', 'C01D0120250010', 'V', 3, NULL, '2025-09-01 09:00:00', '2025-09-03 18:00:00'),
('C01', 'E000005', 'C01D0120250011', 'B', 3, NULL, '2025-08-20 09:00:00', '2025-08-22 18:00:00'),
('C01', 'E000005', 'C01D0120250012', 'V', 1, NULL, '2025-08-15 13:00:00', '2025-08-15 18:00:00');

-- ========================================================
-- mail (각 5)
-- ========================================================
INSERT INTO mail (mail_no, mail_id, sender_id, title, cntt) VALUES
                                                                (1,'MAIL_E000001_251217_001','E000001','테스트 메일 1', JSON_OBJECT('text','안녕하세요 1')),
                                                                (2,'MAIL_E000002_251217_002','E000002','테스트 메일 2', JSON_OBJECT('text','안녕하세요 2')),
                                                                (3,'MAIL_E000003_251217_003','E000003','테스트 메일 3', JSON_OBJECT('text','안녕하세요 3')),
                                                                (4,'MAIL_E000004_251217_004','E000004','테스트 메일 4', JSON_OBJECT('text','안녕하세요 4')),
                                                                (5,'MAIL_E000001_251217_005','E000001','테스트 메일 5', JSON_OBJECT('text','안녕하세요 5'));

INSERT INTO mail_user_state
(recipient_no, mail_id, user_id, role, is_read, is_prior, deleted_at, purged_at)
VALUES
    (1,'MAIL_E000001_251217_001','E000002','RECIPIENT',FALSE,TRUE, NULL, NULL),
    (2,'MAIL_E000002_251217_002','E000003','RECIPIENT',TRUE, FALSE, NULL, NULL),
    (3,'MAIL_E000003_251217_003','E000004','RECIPIENT',FALSE,FALSE, NULL, NULL),
    (4,'MAIL_E000004_251217_004','E000001','RECIPIENT',TRUE, TRUE,  NULL, NULL),
    (5,'MAIL_E000001_251217_005','E000003','RECIPIENT',FALSE,FALSE, NULL, NULL);

INSERT INTO mail_attach (mail_attach_no, mail_id, path, size) VALUES
                                                                  (1,'MAIL_E000001_251217_001','/mail/attach/a1.png', 12345),
                                                                  (2,'MAIL_E000002_251217_002','/mail/attach/a2.pdf', 23456),
                                                                  (3,'MAIL_E000003_251217_003','/mail/attach/a3.xlsx',34567),
                                                                  (4,'MAIL_E000004_251217_004','/mail/attach/a4.txt',   456),
                                                                  (5,'MAIL_E000001_251217_005','/mail/attach/a5.jpg', 98765);

SET FOREIGN_KEY_CHECKS = 1;


USE bizportal;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE attendance;
TRUNCATE TABLE approval_line;
TRUNCATE TABLE document;
SET FOREIGN_KEY_CHECKS = 1;

DELIMITER $$

DROP PROCEDURE IF EXISTS PopulateDocumentTestData;
CREATE PROCEDURE PopulateDocumentTestData()
BEGIN
    DECLARE v_writer_id VARCHAR(7);
    DECLARE v_doc_no BIGINT DEFAULT 1;
    DECLARE v_writer_idx INT DEFAULT 1;
    DECLARE v_doc_idx INT DEFAULT 1;
    DECLARE v_stat VARCHAR(2);
    DECLARE v_temp BOOLEAN;
    DECLARE v_form_no INT;
    DECLARE v_cat_no INT;
    DECLARE v_appr_count INT;
    DECLARE v_appr_step INT;
    DECLARE v_appr_stat CHAR(1);
    DECLARE v_doc_id_val VARCHAR(14);
    DECLARE v_appr_emp_id VARCHAR(7);
    DECLARE v_submit_time DATETIME;
    DECLARE v_reject_step INT; -- ✅ 추가: 몇 번째 결재자가 반려했는지

    -- 작성자 루프: 4(이사원), 8(윤사원), 9(장사원)
    WHILE v_writer_idx <= 3 DO
            SET v_writer_id = CASE v_writer_idx WHEN 1 THEN 'E000004' WHEN 2 THEN 'E000008' ELSE 'E000009' END;
            SET v_doc_idx = 1;

            WHILE v_doc_idx <= 50 DO
                    -- 1. 문서 상태 배분
                    IF v_doc_idx <= 10 THEN
                        SET v_stat = 'US', v_temp = TRUE;
                    ELSEIF v_doc_idx <= 25 THEN
                        SET v_stat = 'AW', v_temp = FALSE;
                    ELSEIF v_doc_idx <= 40 THEN
                        SET v_stat = 'FI', v_temp = FALSE;
                    ELSE
                        SET v_stat = 'RJ', v_temp = FALSE;
                    END IF;

                    SET v_form_no = (v_doc_idx % 4) + 1;
                    SET v_cat_no = CASE v_form_no WHEN 1 THEN 1 WHEN 2 THEN 6 WHEN 3 THEN 8 ELSE 10 END;
                    SET v_submit_time = DATE_SUB(NOW(), INTERVAL (200 - v_doc_no) HOUR);

                    IF v_stat = 'FI' THEN
                        SET v_doc_id_val = CONCAT('DOC', DATE_FORMAT(v_submit_time, '%y%m%d'), LPAD(v_doc_no, 5, '0'));
                    ELSE
                        SET v_doc_id_val = NULL;
                    END IF;

                    -- ✅ 반려 문서인 경우, 몇 번째 결재자가 반려할지 미리 결정
                    IF v_stat = 'RJ' THEN
                        SET v_reject_step = (v_doc_idx % 3) + 1; -- 1, 2, 3번째 결재자 중 하나가 반려
                    ELSE
                        SET v_reject_step = 0;
                    END IF;

                    -- 문서 생성
                    INSERT INTO document (doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, temp, submitted_at, created_at, doc_stat)
                    VALUES (v_doc_no, 'C01', v_doc_id_val, v_cat_no, v_form_no,
                            CONCAT(v_writer_id, '의 문서 ', v_doc_idx, ' (', v_stat, ')'),
                            '{"data": "Test Content"}', '<p>내용 샘플</p>', v_writer_id, v_temp,
                            IF(v_temp, NULL, v_submit_time), DATE_SUB(v_submit_time, INTERVAL 10 MINUTE), v_stat);

                    SET v_appr_count = 3;
                    SET v_appr_step = 1;

                    WHILE v_appr_step <= v_appr_count DO
                            -- 결재자 배정
                            SET v_appr_emp_id = CASE v_writer_id
                                                    WHEN 'E000004' THEN
                                                        CASE v_appr_step WHEN 1 THEN 'E000008' WHEN 2 THEN 'E000009' ELSE 'E000001' END
                                                    WHEN 'E000008' THEN
                                                        CASE
                                                            WHEN v_doc_idx BETWEEN 16 AND 20 THEN
                                                                CASE v_appr_step WHEN 1 THEN 'E000009' WHEN 2 THEN 'E000004' ELSE 'E000001' END
                                                            ELSE
                                                                CASE v_appr_step WHEN 1 THEN 'E000004' WHEN 2 THEN 'E000009' ELSE 'E000001' END
                                                            END
                                                    WHEN 'E000009' THEN
                                                        CASE
                                                            WHEN v_doc_idx BETWEEN 16 AND 20 THEN
                                                                CASE v_appr_step WHEN 1 THEN 'E000008' WHEN 2 THEN 'E000004' ELSE 'E000001' END
                                                            ELSE
                                                                CASE v_appr_step WHEN 1 THEN 'E000004' WHEN 2 THEN 'E000008' ELSE 'E000001' END
                                                            END
                                END;

                            -- ✅ 2. 결재 상태 결정 (수정됨)
                            IF v_stat = 'US' THEN
                                SET v_appr_stat = 'W';

                            ELSEIF v_stat = 'FI' THEN
                                SET v_appr_stat = 'A';

                            ELSEIF v_stat = 'RJ' THEN
                                -- ✅ 반려 로직 수정: 지정된 결재자만 반려, 그 이전은 승인, 이후는 대기
                                IF v_appr_step < v_reject_step THEN
                                    SET v_appr_stat = 'A';  -- 반려 전까지는 승인
                                ELSEIF v_appr_step = v_reject_step THEN
                                    SET v_appr_stat = 'R';  -- 해당 순서에서 반려
                                ELSE
                                    SET v_appr_stat = 'W';  -- 반려 이후는 대기 (결재 못함)
                                END IF;

                            ELSEIF v_stat = 'AW' THEN
                                IF v_appr_emp_id = 'E000004' AND v_doc_idx BETWEEN 16 AND 20 THEN
                                    SET v_appr_stat = 'W'; -- 대기중
                                ELSEIF v_appr_emp_id = 'E000004' AND v_doc_idx BETWEEN 11 AND 15 THEN
                                    SET v_appr_stat = 'I'; -- 내순서
                                ELSEIF v_appr_emp_id = 'E000004' AND v_doc_idx BETWEEN 21 AND 25 THEN
                                    SET v_appr_stat = 'A'; -- 승인완료
                                ELSE
                                    IF v_appr_step = 1 THEN
                                        SET v_appr_stat = 'I';
                                    ELSE
                                        SET v_appr_stat = 'W';
                                    END IF;
                                END IF;
                            END IF;

                            INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, is_actual_appr, is_delegate, rej_reason, ended_at)
                            VALUES ('C01', v_doc_no, v_appr_emp_id, v_appr_step, v_appr_stat,
                                    IF(v_appr_stat IN ('A', 'R'), 1, 0),
                                    FALSE,
                                    IF(v_appr_stat = 'R', '데이터 불충분 반려', NULL),
                                    IF(v_appr_stat IN ('A', 'R'), DATE_ADD(v_submit_time, INTERVAL (v_appr_step * 30) MINUTE), NULL));

                            SET v_appr_step = v_appr_step + 1;
                        END WHILE;

                    SET v_doc_no = v_doc_no + 1;
                    SET v_doc_idx = v_doc_idx + 1;
                END WHILE;
            SET v_writer_idx = v_writer_idx + 1;
        END WHILE;
END $$
DELIMITER ;


CALL PopulateDocumentTestData();
-- 사원 컬럼 추가 후 데이터 넣기
-- 사원 COM_ADMIN(empId=E000001): ComAdmin!123
INSERT INTO employee
(com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
 hire_date, ret_date, addr, role, is_deleted, atte, msg_stat, delegate,
 birth, created_at, updated_at, object_key)
VALUES
    ('C01', 1, 1, 'E000001',
     '$2b$10$ATeuTqhILEKaCdmEIZfdtu9XdRy2M8taQO7x/euNVp5mZXESP/j0i',
     '관리자', 'e000001@c01.com', '010-0000-0001', '1001', 'M',
     NOW(), NULL, '서울특별시 강남구', 'COM_ADMIN', FALSE, 'c', 'c', NULL,
     '1990-01-01', NOW(6), NOW(6), NULL);

-- 사원 SEC_ADMIN(empId=E000002): SecAdmin!123
INSERT INTO employee
(com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
 hire_date, ret_date, addr, role, is_deleted, atte, msg_stat, delegate,
 birth, created_at, updated_at, object_key)
VALUES
    ('C01', 1, 1, 'E000002',
     '$2b$10$2IrR4YB0.bq07kclwgXEcOjuH0ed6/Ztq8uwFaGp53t5.ubvvJOCK',
     '보안관리자', 'e000002@c01.com', '010-0000-0002', '1002', 'M',
     NOW(), NULL, '서울특별시 강남구', 'SEC_ADMIN', FALSE, 'c', 'c', NULL,
     '1991-02-02', NOW(6), NOW(6), NULL);

-- 사원 THR_ADMIN(empId=E000003): ThrAdmin!123
INSERT INTO employee
(com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
 hire_date, ret_date, addr, role, is_deleted, atte, msg_stat, delegate,
 birth, created_at, updated_at, object_key)
VALUES
    ('C01', 1, 1, 'E000003',
     '$2b$10$tHXsfKtqkeKwMpL0lMD7q.KoGGGbAEHAaXpm.oLHcEl/blKCjD5qC',
     '인사관리자', 'e000003@c01.com', '010-0000-0003', '1003', 'F',
     NOW(), NULL, '서울특별시 강남구', 'THR_ADMIN', FALSE, 'c', 'c', NULL,
     '1992-03-03', NOW(6), NOW(6), NULL);

-- 사원 EMPLOYEE(empId=E000004): Emp!12345
INSERT INTO employee
(com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
 hire_date, ret_date, addr, role, is_deleted, atte, msg_stat, delegate,
 birth, created_at, updated_at, object_key)
VALUES
    ('C01', 1, 1, 'E000004',
     '$2b$10$cE6Ul1Pt9RWVcUYqyX0i..cfPssd/xfcGyM.v/77h.Jsgi4QRNzYe',
     '일반사원', 'e000004@c01.com', '010-0000-0004', '1004', 'F',
     NOW(), NULL, '서울특별시 강남구', 'EMPLOYEE', FALSE, 'c', 'c', NULL,
     '1993-04-04', NOW(6), NOW(6), NULL);