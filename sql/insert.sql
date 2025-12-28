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
('C01', 1, 1, 'E000001',
 '$2b$10$Dua3gQf03uEl91WTUjabhuxdk0gl1lq2UcdoPJpINAyV2KovcFtB6',
 '홍관리', 'comadmin@c01.com', '010-1111-1111', '0212345678', 'M',
 '2025-01-02 09:00:00', NULL, '서울특별시 강남구', 'COM_ADMIN', 'N', 'N', NULL,false),

-- 2차 관리자
('C01', 1, 1, 'E000002',
 '$2b$10$IRGCWkvwSo1QejSQoHuH/.V0TRU69cFAteCJurcoIWODwWmg5zu6G',
 '김보안', 'secadmin@c01.com', '010-2222-2222', '0212345679', 'F',
 '2025-01-02 09:10:00', NULL, '서울특별시 강남구', 'SEC_ADMIN', 'N', 'N', 'E000001',false),

-- 3차 관리자
('C01', 1, 1, 'E000003',
 '$2b$10$D4ZZH6fC11EykcEdVhHUvulXICZ6BRtKd4GeJ9hSMjGiOE0e5PCfS',
 '박삼차', 'thradmin@c01.com', '010-3333-3333', '0212345680', 'M',
 '2025-01-02 09:20:00', NULL, '서울특별시 강남구', 'THR_ADMIN', 'N', 'N', 'E000001',false),

-- 일반 사원
('C01', 1, 1, 'E000004',
 '$2b$10$q/lg0kzYBgvalTaVFkToz.EBgcesFddLybxJ4jZc8UADuUTbMq4Iq',
 '이사원', 'employee@c01.com', '010-4444-4444', '0212345681', 'F',
 '2025-01-02 09:30:00', NULL, '서울특별시 강남구', 'EMPLOYEE', 'N', 'N', 'E000001',false);


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
INSERT INTO positions (pos_no, pos_name, com_id) VALUES
                                                     (1, '사원', 'C01'),
                                                     (2, '대리', 'C01'),
                                                     (3, '과장', 'C01'),
                                                     (4, '차장', 'C01'),
                                                     (5, '부장', 'C01');

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
(docfo_no, com_id, writer_id, docfo_name, cntt_json, cntt_html, docfo_stat, reject_reason)
VALUES
    (1,'C01','E000001','휴가 신청서', JSON_OBJECT('type','leave','fields',JSON_ARRAY('기간','사유')), '<h1>휴가 신청서</h1>', 'A', NULL),
    (2,'C01','E000002','출장 신청서', JSON_OBJECT('type','trip','fields',JSON_ARRAY('목적','장소')), '<h1>출장 신청서</h1>', 'P', NULL),
    (3,'C01','E000003','지출 결의서', JSON_OBJECT('type','expense','fields',JSON_ARRAY('금액','내역')), '<h1>지출 결의서</h1>', 'T', NULL),
    (4,'C01','E000004','사내 공지 양식', JSON_OBJECT('type','notice','fields',JSON_ARRAY('제목','내용')), '<h1>공지</h1>', 'A', NULL),
    (5,'C01','E000001','구매 요청서', JSON_OBJECT('type','purchase','fields',JSON_ARRAY('품목','수량')), '<h1>구매 요청서</h1>', 'R', '예산 부족');

-- document_form_category (5)
INSERT INTO document_form_category (docfo_cat_no, com_id, name, docfo_no) VALUES
                                                                              (1,'C01','인사',1),
                                                                              (2,'C01','총무',2),
                                                                              (3,'C01','재무',3),
                                                                              (4,'C01','공지',4),
                                                                              (5,'C01','구매',5);

-- attach_box (5)
INSERT INTO attach_box (attach_no, com_id, uploader, title, dscp, path, size) VALUES
                                                                                  (1,'C01','E000001','회사 소개서','PDF','/s3/attach/company_intro.pdf', 102400),
                                                                                  (2,'C01','E000002','보안 정책','DOCX','/s3/attach/security_policy.docx', 204800),
                                                                                  (3,'C01','E000003','채용 공고','PDF','/s3/attach/recruit.pdf', 51200),
                                                                                  (4,'C01','E000004','개발 가이드','MD','/s3/attach/dev_guide.md', 4096),
                                                                                  (5,'C01','E000001','견적서','XLSX','/s3/attach/quote.xlsx', 307200);

-- document (5)
INSERT INTO document
(doc_no, com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp, doc_stat)
VALUES
    (1,'C01','DOC25121600001',1,1,'휴가 신청(홍관리)', JSON_OBJECT('기간','2025-12-20~2025-12-22','사유','개인사유'), '<p>휴가 신청</p>', 'E000001', '휴가 3일 신청', FALSE, 'AW'),
    (2,'C01','DOC25121700002',2,2,'출장 신청(김보안)', JSON_OBJECT('장소','부산','목적','점검'), '<p>출장 신청</p>', 'E000002', '부산 출장 점검', FALSE, 'AW'),
    (3,'C01','DOC25121800003',3,3,'지출 결의(박삼차)', JSON_OBJECT('금액',100000,'내역','회의비'), '<p>지출 결의</p>', 'E000003', '회의비 10만원', FALSE, 'AW'),
    (4,'C01','DOC25121900004',4,4,'공지(이사원)',     JSON_OBJECT('제목','점검 안내','내용','금요일 점검'), '<p>공지</p>', 'E000004', '점검 공지', FALSE, 'AW'),
    (5,'C01','DOC25122000005',5,5,'구매 요청(홍관리)',   JSON_OBJECT('품목','모니터','수량',2), '<p>구매 요청</p>', 'E000001', '모니터 2대 구매', TRUE, 'AW');

-- approval_line (5)
INSERT INTO approval_line (apprl_no, com_id, doc_no, emp_id, seq, appr_stat, ended_at) VALUES
                                                                                           (1,'C01',1,'E000001',1,'A', NOW()),
                                                                                           (2,'C01',1,'E000002',2,'A', NOW()),
                                                                                           (3,'C01',1,'E000003',3,'A', NOW()),
                                                                                           (4,'C01',1,'E000004',4,'A', NOW()),
                                                                                           (5,'C01',2,'E000001',1,'I', NOW()),
                                                                                           (6,'C01',2,'E000002',2,'W', NOW()),
                                                                                           (7,'C01',2,'E000003',3,'W', NOW()),
                                                                                           (8,'C01',2,'E000004',4,'W', NOW());

-- document_file (5)
INSERT INTO document_file (docfi_no, com_id, doc_no, docfi_name, folder_path) VALUES
                                                                                  (1,'C01',1,'leave_attach.pdf','/drive/C01/docs/leave'),
                                                                                  (2,'C01',2,'trip_plan.pdf','/drive/C01/docs/trip'),
                                                                                  (3,'C01',3,'expense_receipt.jpg','/drive/C01/docs/expense'),
                                                                                  (4,'C01',4,'notice_img.png','/drive/C01/docs/notice'),
                                                                                  (5,'C01',5,'purchase_quote.xlsx','/drive/C01/docs/purchase');

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

INSERT INTO attendance
(atte_no, com_id, emp_id, doc_id, type, day, delegate, created_at, ended_at)
VALUES
    (1,'C01','E000001','DOC25121600001','V',3,NULL,'2025-12-20 09:00:00','2025-12-22 18:00:00'),
    (2,'C01','E000002','DOC25121700002','B',1,'E000001','2025-12-17 09:00:00','2025-12-17 18:00:00'),
    (3,'C01','E000003','DOC25121800003','O',1,NULL,'2025-12-18 09:00:00','2025-12-18 18:00:00'),
    (4,'C01','E000004','DOC25121900004','V',1,NULL,'2025-12-19 09:00:00','2025-12-19 18:00:00'),
    (5,'C01','E000001','DOC25122000005','B',2,'E000002','2025-12-20 09:00:00','2025-12-21 18:00:00');

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

use bizportal;
select * from comment;