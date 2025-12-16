-- ========================================================
-- 더미 데이터 (FK 순서 고려)
-- ========================================================

-- 1) subscription (최대 3개만 가능)
INSERT INTO subscription (sub_name, sub_desc, sub_price) VALUES
                                                             ('basic',    'Basic plan',    10000),
                                                             ('pro',      'Pro plan',      30000),
                                                             ('ultimate', 'Ultimate plan', 70000);

-- 2) company (sub_no: 1~3 참조)
INSERT INTO company (com_id, com_name, email, pwd, brn, emp_cnt, addr, sub_no, img_url, path) VALUES
                                                                                                  ('A01','Alpha Co','admin@a01.com','pw','1234567890', 50,'Seoul',     1, NULL, NULL),
                                                                                                  ('A02','Beta Co', 'admin@a02.com','pw','2345678901',120,'Busan',     2, NULL, NULL),
                                                                                                  ('A03','Gamma Co','admin@a03.com','pw','3456789012', 80,'Incheon',   3, NULL, NULL),
                                                                                                  ('A04','Delta Co','admin@a04.com','pw','4567890123', 35,'Daegu',     1, NULL, NULL),
                                                                                                  ('A05','Epsilon','admin@a05.com','pw','5678901234',200,'Daejeon',    2, NULL, NULL);

-- 3) department (dep_no는 자동 1~5 생성될 것)
INSERT INTO department (dep_id, dep_name, com_id) VALUES
                                                      ('001','HR',    'A01'),
                                                      ('001','DEV',   'A02'),
                                                      ('001','FIN',   'A03'),
                                                      ('001','SALES', 'A04'),
                                                      ('001','OPS',   'A05');

-- 4) positions (pos_no 자동 1~5)
INSERT INTO positions (pos_name, com_id) VALUES
                                             ('사원', 'A01'),
                                             ('대리', 'A02'),
                                             ('과장', 'A03'),
                                             ('차장', 'A04'),
                                             ('부장', 'A05');

-- 5) employee (dep_no:1~5, pos_no:1~5)
INSERT INTO employee (
    com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen,
    hire_date, ret_date, addr, role_no, is_deleted, atte, msg_stat, delegate
) VALUES
      ('A01', 1, 1, 'EMP0001','pw','김하나','emp1@a01.com','01011112222','1001','F','2024-01-10 09:00:00', NULL,'Seoul',   1, FALSE,'0','0', NULL),
      ('A02', 2, 2, 'EMP0002','pw','이둘','emp2@a02.com','01022223333','1002','M','2023-03-05 09:00:00', NULL,'Busan',   1, FALSE,'0','1', 'EMP0001'),
      ('A03', 3, 3, 'EMP0003','pw','박셋','emp3@a03.com','01033334444','1003','F','2022-07-01 09:00:00', NULL,'Incheon', 1, FALSE,'1','0', NULL),
      ('A04', 4, 4, 'EMP0004','pw','최넷','emp4@a04.com','01044445555','1004','M','2021-11-20 09:00:00', NULL,'Daegu',   2, FALSE,'0','2', 'EMP0002'),
      ('A05', 5, 5, 'EMP0005','pw','정다섯','emp5@a05.com','01055556666','1005','F','2020-05-15 09:00:00', NULL,'Daejeon', 2, FALSE,'2','0', NULL);

-- 6) schedule
INSERT INTO schedule (com_id, dep_no, title, content, start_at, ended_at, color, reg_emp) VALUES
                                                                                              ('A01',1,'주간회의','주간업무 공유','2025-12-16 10:00:00','2025-12-16 11:00:00','#FFAA00','EMP0001'),
                                                                                              ('A02',2,'스프린트 계획','백로그 점검','2025-12-17 14:00:00','2025-12-17 15:00:00','#00AAFF','EMP0002'),
                                                                                              ('A03',3,'결산 준비','재무 자료 정리','2025-12-18 09:30:00','2025-12-18 10:30:00','#00CC66','EMP0003'),
                                                                                              ('A04',4,'영업 미팅','고객사 미팅','2025-12-19 16:00:00','2025-12-19 17:00:00','#AA00FF','EMP0004'),
                                                                                              ('A05',5,'운영 점검','장비 점검','2025-12-20 13:00:00','2025-12-20 14:00:00','#666666','EMP0005');

-- 7) emp_schedule
INSERT INTO emp_schedule (emp_id, title, content, start_at, ended_at, color) VALUES
                                                                                 ('EMP0001','개인운동','헬스','2025-12-16 19:00:00','2025-12-16 20:00:00','#111111'),
                                                                                 ('EMP0002','병원','정기검진','2025-12-17 09:00:00','2025-12-17 10:00:00','#222222'),
                                                                                 ('EMP0003','스터디','CS스터디','2025-12-18 20:00:00','2025-12-18 22:00:00','#333333'),
                                                                                 ('EMP0004','은행','업무','2025-12-19 11:00:00','2025-12-19 11:30:00','#444444'),
                                                                                 ('EMP0005','외근','출장','2025-12-20 08:00:00','2025-12-20 12:00:00','#555555');

-- 8) todo_list
INSERT INTO todo_list (emp_id, title, is_done) VALUES
                                                   ('EMP0001','API 문서 정리', FALSE),
                                                   ('EMP0002','ERD 수정',      TRUE),
                                                   ('EMP0003','결재 테스트',   FALSE),
                                                   ('EMP0004','회의록 업로드', TRUE),
                                                   ('EMP0005','서버 로그 점검',FALSE);

-- 9) document_form (writer_id는 FK 없지만 emp_id 값 넣음)
INSERT INTO document_form (com_id, writer_id, docfo_name, cntt_json, cntt_html, docfo_stat, reject_reason) VALUES
                                                                                                               ('A01','EMP0001','휴가신청서', JSON_OBJECT('type','leave','fields',JSON_ARRAY('from','to','reason')), '<p>휴가신청서</p>', 'T', NULL),
                                                                                                               ('A02','EMP0002','출장신청서', JSON_OBJECT('type','trip','fields',JSON_ARRAY('place','date','purpose')), '<p>출장신청서</p>', 'P', NULL),
                                                                                                               ('A03','EMP0003','지출결의서', JSON_OBJECT('type','expense','fields',JSON_ARRAY('amount','items')), '<p>지출결의서</p>', 'A', NULL),
                                                                                                               ('A04','EMP0004','구매요청서', JSON_OBJECT('type','purchase','fields',JSON_ARRAY('item','qty')), '<p>구매요청서</p>', 'R', '예산 부족'),
                                                                                                               ('A05','EMP0005','회의록양식', JSON_OBJECT('type','meeting','fields',JSON_ARRAY('agenda','decisions')), '<p>회의록</p>', 'P', NULL);

-- 10) document_form_category (docfo_no: 1~5 가정)
INSERT INTO document_form_category (com_id, name, docfo_no) VALUES
                                                                ('A01','인사', 1),
                                                                ('A02','업무', 2),
                                                                ('A03','재무', 3),
                                                                ('A04','구매', 4),
                                                                ('A05','회의', 5);

-- 11) attach_box (FK는 company만)
INSERT INTO attach_box (com_id, uploader, title, dscp, path, size) VALUES
                                                                       ('A01','EMP0001','프로필사진','이미지','/s3/a01/profile1.png', 120345),
                                                                       ('A02','EMP0002','기획서','PDF','/s3/a02/plan.pdf',          804512),
                                                                       ('A03','EMP0003','정산자료','엑셀','/s3/a03/settle.xlsx',     50211),
                                                                       ('A04','EMP0004','영업자료','PPT','/s3/a04/sales.pptx',       912345),
                                                                       ('A05','EMP0005','운영매뉴얼','문서','/s3/a05/ops.docx',      223344);

-- 12) document (docfo_cat_no:1~5, docfo_no:1~5)
INSERT INTO document (com_id, doc_id, docfo_cat_no, docfo_no, title, content, cntt_html, emp_id, ai_summ, temp) VALUES
                                                                                                                    ('A01','20251216000001',1,1,'휴가 신청(김하나)', JSON_OBJECT('from','2025-12-25','to','2025-12-26','reason','개인사유'), '<p>휴가</p>', 'EMP0001', NULL, FALSE),
                                                                                                                    ('A02','20251216000002',2,2,'출장 신청(이둘)',   JSON_OBJECT('place','Jeju','date','2025-12-28','purpose','고객미팅'), '<p>출장</p>', 'EMP0002', NULL, FALSE),
                                                                                                                    ('A03','20251216000003',3,3,'지출 결의(박셋)',   JSON_OBJECT('amount',120000,'items',JSON_ARRAY('택시','식대')), '<p>지출</p>', 'EMP0003', NULL, FALSE),
                                                                                                                    ('A04','20251216000004',4,4,'구매 요청(최넷)',   JSON_OBJECT('item','모니터','qty',2), '<p>구매</p>', 'EMP0004', NULL, TRUE),
                                                                                                                    ('A05','20251216000005',5,5,'회의록(정다섯)',    JSON_OBJECT('agenda','운영점검','decisions',JSON_ARRAY('백업강화')), '<p>회의록</p>', 'EMP0005', NULL, FALSE);

-- 13) approval_line (doc_no는 document의 PK 1~5 가정)
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat, ended_at) VALUES
                                                                                 ('A01',1,'EMP0002',1,'0',NULL),
                                                                                 ('A02',2,'EMP0003',1,'1','2025-12-17 15:10:00'),
                                                                                 ('A03',3,'EMP0004',1,'2','2025-12-18 10:40:00'),
                                                                                 ('A04',4,'EMP0005',1,'3','2025-12-19 17:05:00'),
                                                                                 ('A05',5,'EMP0001',1,'0',NULL);

-- 14) document_file
INSERT INTO document_file (com_id, doc_no, docfi_name, folder_path) VALUES
                                                                        ('A01',1,'leave.pdf','/drive/a01/docs'),
                                                                        ('A02',2,'trip.pdf','/drive/a02/docs'),
                                                                        ('A03',3,'expense.pdf','/drive/a03/docs'),
                                                                        ('A04',4,'purchase.pdf','/drive/a04/docs'),
                                                                        ('A05',5,'meeting.pdf','/drive/a05/docs');

-- 15) prov_document
INSERT INTO prov_document (com_id, doc_title, description, is_public, file_name, file_url, file_size, chunk_cnt, proc_stat, error_msg) VALUES
                                                                                                                                           ('A01','인사규정','인사 규정 요약', TRUE, 'hr.pdf','/s3/a01/prov/hr.pdf', 123000, 5, 'DONE', NULL),
                                                                                                                                           ('A02','개발규칙','코딩 컨벤션', TRUE, 'dev.pdf','/s3/a02/prov/dev.pdf', 98000, 3, 'DONE', NULL),
                                                                                                                                           ('A03','회계규정','회계 처리', TRUE, 'fin.pdf','/s3/a03/prov/fin.pdf', 110000, 4, 'DONE', NULL),
                                                                                                                                           ('A04','영업정책','영업 정책', TRUE, 'sales.pdf','/s3/a04/prov/sales.pdf', 99000, 3, 'DONE', NULL),
                                                                                                                                           ('A05','운영매뉴얼','운영 절차', TRUE, 'ops.pdf','/s3/a05/prov/ops.pdf', 150000, 6, 'DONE', NULL);

-- 16) meeting
INSERT INTO meeting (com_id, dep_no, emp_id, title, stt_text, ai_text, started_at, is_deleted) VALUES
                                                                                                   ('A01',1,'EMP0001','주간회의','text1','sum1','2025-12-16 10:00:00',FALSE),
                                                                                                   ('A02',2,'EMP0002','개발회의','text2','sum2','2025-12-17 14:00:00',FALSE),
                                                                                                   ('A03',3,'EMP0003','재무회의','text3','sum3','2025-12-18 09:30:00',FALSE),
                                                                                                   ('A04',4,'EMP0004','영업회의','text4','sum4','2025-12-19 16:00:00',FALSE),
                                                                                                   ('A05',5,'EMP0005','운영회의','text5','sum5','2025-12-20 13:00:00',FALSE);

-- 17) meeting_emp (meet_no 1~5 가정)
INSERT INTO meeting_emp (com_id, emp_id, meet_no) VALUES
                                                      ('A01','EMP0002',1),
                                                      ('A02','EMP0003',2),
                                                      ('A03','EMP0004',3),
                                                      ('A04','EMP0005',4),
                                                      ('A05','EMP0001',5);

-- 18) chat_rooms
INSERT INTO chat_rooms (room_name, room_type) VALUES
                                                  ('1:1-하나둘','O'),
                                                  ('1:1-둘셋','O'),
                                                  ('1:1-셋넷','O'),
                                                  ('단톡-프로젝트','M'),
                                                  ('단톡-공지','M');

-- 19) chat_room_members (room_no 1~5 가정)
INSERT INTO chat_room_members (room_no, emp_id, last_read_msg_id) VALUES
                                                                      (1,'EMP0001','6565a1'),
                                                                      (2,'EMP0002','6565a2'),
                                                                      (3,'EMP0003','6565a3'),
                                                                      (4,'EMP0004','6565a4'),
                                                                      (5,'EMP0005','6565a5');

-- 20) system_logs
INSERT INTO system_logs (com_id, level, message) VALUES
                                                     ('A01','INFO','RAG started'),
                                                     ('A02','WARN','Slow query detected'),
                                                     ('A03','INFO','Index created'),
                                                     ('A04','ERROR','S3 upload failed'),
                                                     ('A05','INFO','Batch cleanup done');

-- 21) meeting_room
INSERT INTO meeting_room (com_id, room_name, cap, loc, img_url, equip_list, note) VALUES
                                                                                      ('A01','A01-회의실1',8,'10F',NULL,'TV,Whiteboard','-'),
                                                                                      ('A02','A02-회의실1',6,'3F',NULL,'TV','-'),
                                                                                      ('A03','A03-회의실1',10,'2F',NULL,'Projector','-'),
                                                                                      ('A04','A04-회의실1',4,'1F',NULL,'Whiteboard','-'),
                                                                                      ('A05','A05-회의실1',12,'5F',NULL,'TV,Projector','-');

-- 22) corporate_car
INSERT INTO corporate_car (com_id, car_name, car_type, plate_no, cap, fuel, img_url) VALUES
                                                                                         ('A01','쏘나타','Sedan','11가1111',4,'Gas',NULL),
                                                                                         ('A02','카니발','Van',  '22나2222',7,'Gas',NULL),
                                                                                         ('A03','아반떼','Sedan','33다3333',4,'Gas',NULL),
                                                                                         ('A04','스타렉스','Van','44라4444',9,'Diesel',NULL),
                                                                                         ('A05','K5','Sedan','55마5555',4,'Gas',NULL);

-- 23) shared_equipment
INSERT INTO shared_equipment (com_id, eq_name, eq_id, model_name, img_url, loc) VALUES
                                                                                    ('A01','빔프로젝터','EQ001','EPSON-X1',NULL,'10F'),
                                                                                    ('A02','노트북','EQ002','LG-그램',NULL,'3F'),
                                                                                    ('A03','카메라','EQ003','SONY-A7',NULL,'2F'),
                                                                                    ('A04','마이크','EQ004','SHURE',NULL,'1F'),
                                                                                    ('A05','태블릿','EQ005','iPad',NULL,'5F');

-- 24) meeting_room_reservation (room_no 1~5 가정)
INSERT INTO meeting_room_reservation (com_id, room_no, started_at, ended_at, resv_emp, purp, is_deleted) VALUES
                                                                                                             ('A01',1,'2025-12-21 10:00:00','2025-12-21 11:00:00','EMP0001','회의',FALSE),
                                                                                                             ('A02',2,'2025-12-21 14:00:00','2025-12-21 15:00:00','EMP0002','면접',FALSE),
                                                                                                             ('A03',3,'2025-12-22 09:00:00','2025-12-22 10:30:00','EMP0003','결산',FALSE),
                                                                                                             ('A04',4,'2025-12-22 16:00:00','2025-12-22 17:00:00','EMP0004','고객미팅',FALSE),
                                                                                                             ('A05',5,'2025-12-23 13:00:00','2025-12-23 14:00:00','EMP0005','운영점검',FALSE);

-- 25) meeting_room_attendee (meeting_resv_no 1~5 가정)
INSERT INTO meeting_room_attendee (com_id, meeting_resv_no, emp_id) VALUES
                                                                        ('A01',1,'EMP0002'),
                                                                        ('A02',2,'EMP0003'),
                                                                        ('A03',3,'EMP0004'),
                                                                        ('A04',4,'EMP0005'),
                                                                        ('A05',5,'EMP0001');

-- 26) corporate_car_reservation (car_no 1~5 가정)
INSERT INTO corporate_car_reservation (com_id, car_no, started_at, ended_at, resv_emp, purp, is_deleted) VALUES
                                                                                                             ('A01',1,'2025-12-24 09:00:00','2025-12-24 12:00:00','EMP0001','외근',FALSE),
                                                                                                             ('A02',2,'2025-12-24 13:00:00','2025-12-24 16:00:00','EMP0002','출장',FALSE),
                                                                                                             ('A03',3,'2025-12-25 09:00:00','2025-12-25 10:30:00','EMP0003','은행',FALSE),
                                                                                                             ('A04',4,'2025-12-25 14:00:00','2025-12-25 18:00:00','EMP0004','고객사',FALSE),
                                                                                                             ('A05',5,'2025-12-26 08:00:00','2025-12-26 11:00:00','EMP0005','장비수령',FALSE);

-- 27) shared_equipment_reservation (eq_no 1~5 가정)
INSERT INTO shared_equipment_reservation (com_id, eq_no, started_at, ended_at, resv_emp, purp, is_deleted) VALUES
                                                                                                               ('A01',1,'2025-12-21 09:00:00','2025-12-21 12:00:00','EMP0001','발표',FALSE),
                                                                                                               ('A02',2,'2025-12-21 13:00:00','2025-12-21 18:00:00','EMP0002','개발',FALSE),
                                                                                                               ('A03',3,'2025-12-22 10:00:00','2025-12-22 12:00:00','EMP0003','촬영',FALSE),
                                                                                                               ('A04',4,'2025-12-22 15:00:00','2025-12-22 16:00:00','EMP0004','회의',FALSE),
                                                                                                               ('A05',5,'2025-12-23 09:00:00','2025-12-23 11:00:00','EMP0005','점검',FALSE);

-- 28) folder (parent_id 자기참조)
INSERT INTO folder (com_id, dep_no, parent_id, folder_name, owner_id, scope, path) VALUES
                                                                                       ('A01', NULL, NULL, 'root-a01', 'EMP0001', 'all',  '/root-a01'),
                                                                                       ('A02', NULL, NULL, 'root-a02', 'EMP0002', 'all',  '/root-a02'),
                                                                                       ('A03', NULL, NULL, 'root-a03', 'EMP0003', 'all',  '/root-a03'),
                                                                                       ('A01', 1,    1,    'dept-a01', 'EMP0001', 'dept', '/root-a01/dept-a01'),
                                                                                       ('A01', NULL, 1,    'private',  'EMP0001', 'prvt', '/root-a01/private');

-- 29) file (folder_no 1~5)
INSERT INTO `file` (com_id, folder_no, file_name, size, emp_id, path) VALUES
                                                                          ('A01',1,'readme.txt', 120,   'EMP0001','/root-a01/readme.txt'),
                                                                          ('A02',2,'spec.pdf',   80231, 'EMP0002','/root-a02/spec.pdf'),
                                                                          ('A03',3,'data.csv',   5011,  'EMP0003','/root-a03/data.csv'),
                                                                          ('A01',4,'dept.docx',  22011, 'EMP0001','/root-a01/dept-a01/dept.docx'),
                                                                          ('A01',5,'secret.png', 9912,  'EMP0001','/root-a01/private/secret.png');

-- 30) folder_permission
INSERT INTO folder_permission (com_id, folder_no, emp_id, perm_type) VALUES
                                                                         ('A01',1,'EMP0002','R'),
                                                                         ('A02',2,'EMP0003','W'),
                                                                         ('A03',3,'EMP0004','R'),
                                                                         ('A01',4,'EMP0005','R'),
                                                                         ('A01',5,'EMP0001','W');

-- 31) board_cat
INSERT INTO board_cat (cat_code, cat_descript) VALUES
                                                   ('F','자유게시판'),
                                                   ('N','공지'),
                                                   ('Q','질문'),
                                                   ('P','프로젝트'),
                                                   ('E','이벤트');

-- 32) notice
INSERT INTO notice (com_id, is_deleted, title, contents, is_popup, started_at, ended_at, emp_id, rating) VALUES
                                                                                                             ('A01',FALSE,'공지1', JSON_OBJECT('text','공지 내용1'), TRUE,  '2025-12-16 00:00:00','2025-12-20 23:59:59','EMP0001',0),
                                                                                                             ('A02',FALSE,'공지2', JSON_OBJECT('text','공지 내용2'), FALSE, NULL,NULL,'EMP0002',0),
                                                                                                             ('A03',FALSE,'공지3', JSON_OBJECT('text','공지 내용3'), FALSE, NULL,NULL,'EMP0003',0),
                                                                                                             ('A04',FALSE,'공지4', JSON_OBJECT('text','공지 내용4'), FALSE, NULL,NULL,'EMP0004',0),
                                                                                                             ('A05',FALSE,'공지5', JSON_OBJECT('text','공지 내용5'), FALSE, NULL,NULL,'EMP0005',0);

-- 33) notice_attach (notice_no 1~5)
INSERT INTO notice_attach (com_id, notice_no, org_name, folder_path) VALUES
                                                                         ('A01',1,'notice1.pdf','/drive/a01/notice'),
                                                                         ('A02',2,'notice2.pdf','/drive/a02/notice'),
                                                                         ('A03',3,'notice3.pdf','/drive/a03/notice'),
                                                                         ('A04',4,'notice4.pdf','/drive/a04/notice'),
                                                                         ('A05',5,'notice5.pdf','/drive/a05/notice');

-- 34) board (cat_code는 board_cat의 코드 사용)
INSERT INTO board (com_id, is_deleted, title, contents, cat_code, emp_id, rating) VALUES
                                                                                      ('A01',FALSE,'자유글1', JSON_OBJECT('text','내용1'),'F','EMP0001',0),
                                                                                      ('A02',FALSE,'질문글2', JSON_OBJECT('text','내용2'),'Q','EMP0002',1),
                                                                                      ('A03',FALSE,'프로젝트3', JSON_OBJECT('text','내용3'),'P','EMP0003',2),
                                                                                      ('A04',FALSE,'이벤트4', JSON_OBJECT('text','내용4'),'E','EMP0004',0),
                                                                                      ('A05',FALSE,'자유글5', JSON_OBJECT('text','내용5'),'F','EMP0005',0);

-- 35) comment (board_no 1~5)
INSERT INTO comment (com_id, board_no, contents, emp_id) VALUES
                                                             ('A01',1,'댓글1','EMP0002'),
                                                             ('A02',2,'댓글2','EMP0003'),
                                                             ('A03',3,'댓글3','EMP0004'),
                                                             ('A04',4,'댓글4','EMP0005'),
                                                             ('A05',5,'댓글5','EMP0001');

-- 36) board_attach (board_no 1~5)
INSERT INTO board_attach (com_id, board_no, org_name, folder_path) VALUES
                                                                       ('A01',1,'b1.png','/drive/a01/board'),
                                                                       ('A02',2,'b2.png','/drive/a02/board'),
                                                                       ('A03',3,'b3.png','/drive/a03/board'),
                                                                       ('A04',4,'b4.png','/drive/a04/board'),
                                                                       ('A05',5,'b5.png','/drive/a05/board');

-- 37) payment_method
INSERT INTO payment_method (com_id, paym_type, card_type, billing_key, mask, active) VALUES
                                                                                         ('A01','C','VISA','billkey-a01-1','1111-****-****-1111',TRUE),
                                                                                         ('A02','C','MASTER','billkey-a02-1','2222-****-****-2222',TRUE),
                                                                                         ('A03','A',NULL,'billkey-a03-1',NULL,TRUE),
                                                                                         ('A04','C','AMEX','billkey-a04-1','4444-****-****-4444',TRUE),
                                                                                         ('A05','A',NULL,'billkey-a05-1',NULL,TRUE);

-- 38) payment_history (paym_no 1~5)
INSERT INTO payment_history (com_id, amount, pay_result, paym_no, created_at) VALUES
                                                                                  ('A01',10000,TRUE, 1,'2025-12-16 12:00:00'),
                                                                                  ('A02',30000,TRUE, 2,'2025-12-16 12:05:00'),
                                                                                  ('A03',70000,FALSE,3,'2025-12-16 12:10:00'),
                                                                                  ('A04',10000,TRUE, 4,'2025-12-16 12:15:00'),
                                                                                  ('A05',30000,TRUE, 5,'2025-12-16 12:20:00');

-- 39) attendance (doc_id는 document.doc_id 참조)
INSERT INTO attendance (com_id, emp_id, doc_id, type, day, delegate, created_at, ended_at) VALUES
                                                                                               ('A01','EMP0001','20251216000001','V',1,NULL,'2025-12-16 09:00:00','2025-12-16 18:00:00'),
                                                                                               ('A02','EMP0002','20251216000002','B',2,NULL,'2025-12-17 09:00:00','2025-12-18 18:00:00'),
                                                                                               ('A03','EMP0003','20251216000003','V',1,NULL,'2025-12-18 09:00:00','2025-12-18 18:00:00'),
                                                                                               ('A04','EMP0004','20251216000004','B',1,'EMP0002','2025-12-19 09:00:00','2025-12-19 18:00:00'),
                                                                                               ('A05','EMP0005','20251216000005','V',3,NULL,'2025-12-20 09:00:00','2025-12-22 18:00:00');

-- 40) mail
INSERT INTO mail (mail_id, sender_id, title, cntt) VALUES
                                                       ('EMP0001_1700000000001','EMP0001','메일1', JSON_OBJECT('text','내용1')),
                                                       ('EMP0002_1700000000002','EMP0002','메일2', JSON_OBJECT('text','내용2')),
                                                       ('EMP0003_1700000000003','EMP0003','메일3', JSON_OBJECT('text','내용3')),
                                                       ('EMP0004_1700000000004','EMP0004','메일4', JSON_OBJECT('text','내용4')),
                                                       ('EMP0005_1700000000005','EMP0005','메일5', JSON_OBJECT('text','내용5'));

-- 41) mail_user_state (FK는 mail_id만)
INSERT INTO mail_user_state (mail_id, user_id, role, is_read, is_prior, deleted_at, purged_at) VALUES
                                                                                                   ('EMP0001_1700000000001','EMP0002','RECIPIENT',FALSE,FALSE,NULL,NULL),
                                                                                                   ('EMP0002_1700000000002','EMP0003','RECIPIENT',TRUE, FALSE,NULL,NULL),
                                                                                                   ('EMP0003_1700000000003','EMP0004','RECIPIENT',FALSE,TRUE, NULL,NULL),
                                                                                                   ('EMP0004_1700000000004','EMP0005','RECIPIENT',TRUE, TRUE, NULL,NULL),
                                                                                                   ('EMP0005_1700000000005','EMP0001','RECIPIENT',FALSE,FALSE,NULL,NULL);

-- 42) mail_attach
INSERT INTO mail_attach (mail_id, path, size) VALUES
                                                  ('EMP0001_1700000000001','/s3/mail/a1.png', 1234),
                                                  ('EMP0002_1700000000002','/s3/mail/a2.pdf', 2345),
                                                  ('EMP0003_1700000000003','/s3/mail/a3.docx',3456),
                                                  ('EMP0004_1700000000004','/s3/mail/a4.zip', 4567),
                                                  ('EMP0005_1700000000005','/s3/mail/a5.txt', 5678);

-- 43) refresh_token (현재 DDL은 email/emp_id 둘 다 NOT NULL)
INSERT INTO refresh_token (email, emp_id, token, created_at, expired_at) VALUES
                                                                             ('admin@a01.com','EMP0001','rtk_a01_emp1_001','2025-12-16 12:00:00','2026-01-15 12:00:00'),
                                                                             ('admin@a02.com','EMP0002','rtk_a02_emp2_001','2025-12-16 12:01:00','2026-01-15 12:01:00'),
                                                                             ('admin@a03.com','EMP0003','rtk_a03_emp3_001','2025-12-16 12:02:00','2026-01-15 12:02:00'),
                                                                             ('admin@a04.com','EMP0004','rtk_a04_emp4_001','2025-12-16 12:03:00','2026-01-15 12:03:00'),
                                                                             ('admin@a05.com','EMP0005','rtk_a05_emp5_001','2025-12-16 12:04:00','2026-01-15 12:04:00');