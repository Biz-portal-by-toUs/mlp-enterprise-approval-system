-- ========================================================
use bizportal
-- [설정] FK 체크 해제 (편의상)
-- ========================================================
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1. 공통/조직 (Organization) - 기초 데이터
-- =========================

-- 1) 요금제
INSERT INTO subscription (sub_name, sub_desc, sub_price) VALUES
                                                             ('basic', '기본 요금제', 0),
                                                             ('pro', '전문가용 요금제', 50000),
                                                             ('ultimate', '기업용 요금제', 100000);

-- 2) 회사 (C01: 테헤란 솔루션)
INSERT INTO company (com_id, com_name, email, pwd, brn, emp_cnt, addr, sub_no, img_url, path) VALUES
    ('C01', '테헤란 솔루션', 'admin@teheran.com', '$2a$10$Ew...', '1234567890', 50, '서울 강남구 테헤란로 123', 3, 'http://img.url/logo.png', '/company/c01');

-- 3) 부서 (D01: 경영지원, D02: 개발팀)
-- AUTO_INCREMENT 가정: D01 -> dep_no=1, D02 -> dep_no=2
INSERT INTO department (dep_id, dep_name, com_id) VALUES
                                                      ('D01', '경영지원', 'C01'),
                                                      ('D02', '개발팀', 'C01'),
                                                      ('D03', '영업팀', 'C01');

-- 4) 직급 (P01: 사원, P02: 대리, P03: 부장)
-- AUTO_INCREMENT 가정: 사원=1, 대리=2, 부장=3
INSERT INTO positions (pos_name, com_id) VALUES
                                             ('사원', 'C01'),
                                             ('대리', 'C01'),
                                             ('부장', 'C01');

-- 5) 사원 (3명 생성)
-- 2024001: 김대표 (개발팀 부장, 관리자)
-- 2024002: 이사원 (개발팀 사원)
-- 2024003: 박대리 (경영지원 대리)
INSERT INTO employee (com_id, dep_no, pos_no, emp_id, pwd, emp_name, email, phone, work_phone, gen, hire_date, addr, role_no, is_deleted, atte, msg_stat) VALUES
                                                                                                                                                              ('C01', 2, 3, '2024001', '$2a$10$dummyHash...', '김대표', 'ceo@teheran.com', '010-1111-1111', '02-111', 'M', '2020-01-01', '서울 서초구', 1, 0, 'Y', 'Y'),
                                                                                                                                                              ('C01', 2, 1, '2024002', '$2a$10$dummyHash...', '이사원', 'lee@teheran.com', '010-2222-2222', '02-222', 'M', '2024-01-01', '경기 성남시', 2, 0, 'Y', 'Y'),
                                                                                                                                                              ('C01', 1, 2, '2024003', '$2a$10$dummyHash...', '박대리', 'park@teheran.com', '010-3333-3333', '02-333', 'F', '2022-05-05', '서울 강동구', 2, 0, 'N', 'Y');

-- =========================
-- 2. 인증 (Refresh Token) - New!
-- =========================
INSERT INTO refresh_token (email, emp_id, token, expired_at) VALUES
    ('ceo@teheran.com', '2024001', 'ref_token_sample_string_12345', DATE_ADD(NOW(), INTERVAL 7 DAY));

-- =========================
-- 3. 일정 및 할일
-- =========================

-- 전사 일정
INSERT INTO schedule (com_id, dep_no, title, content, start_at, ended_at, color, reg_emp) VALUES
    ('C01', 2, '개발팀 워크샵', '강원도 양양 서핑', '2025-06-20 09:00:00', '2025-06-21 18:00:00', '#FF0000', '2024001');

-- 개인 일정
INSERT INTO emp_schedule (emp_id, title, content, start_at, ended_at, color) VALUES
    ('2024002', '치과 예약', '사랑니 발치', '2025-01-10 14:00:00', '2025-01-10 16:00:00', '#00FF00');

-- 할일
INSERT INTO todo_list (emp_id, title, is_done) VALUES
                                                   ('2024002', 'API 명세서 작성', 0),
                                                   ('2024002', '더미 데이터 넣기', 1);

-- =========================
-- 4. 전자결재 (E-Approval)
-- =========================

-- 문서 양식
INSERT INTO document_form (com_id, writer_id, docfo_name, docfo_id, cntt_json, cntt_html, docfo_stat) VALUES
                                                                                                          ('C01', '2024003', '휴가신청서', 'FORM01', '{"fields": ["date", "reason"]}', '<h1>휴가신청서</h1>', 'A'),
                                                                                                          ('C01', '2024003', '지출결의서', 'FORM02', '{"fields": ["amount", "usage"]}', '<h1>지출결의서</h1>', 'A');

-- 양식 카테고리
INSERT INTO document_form_category (com_id, docfo_cat_id, name, docfo_id) VALUES
    ('C01', 'CAT001', '인사 관련', 'FORM01');

-- 기안 문서 (이사원이 휴가 신청)
INSERT INTO document (com_id, doc_id, docfo_cat_id, docfo_id, title, content, cntt_html, emp_id, temp) VALUES
    ('C01', 'DOC-2024-001', 'CAT001', 'FORM01', '7월 정기 휴가 신청합니다', '{"date": "2024-07-01", "reason": "힐링"}', '<p>휴가 갑니다</p>', '2024002', 0);

-- 결재 라인 (이사원 기안 -> 김대표 결재 대기)
INSERT INTO approval_line (com_id, doc_no, emp_id, seq, appr_stat) VALUES
                                                                       ('C01', 1, '2024002', 1, '2'), -- 본인 기안(승인 상태)
                                                                       ('C01', 1, '2024001', 2, '0'); -- 대표 결재(대기 상태)

-- =========================
-- 5. 커뮤니케이션 (Meeting & Chat)
-- =========================

-- 회의록
INSERT INTO meeting (com_id, dep_no, emp_id, title, stt_text, ai_text, started_at) VALUES
    ('C01', 2, '2024002', 'DB 설계 회의', '그럼 테이블은 이렇게 하시죠...', '요약: 테이블 설계 완료', NOW());

-- 회의 참석자
INSERT INTO meeting_emp (com_id, emp_id, meet_no) VALUES
                                                      ('C01', '2024001', 1),
                                                      ('C01', '2024002', 1);

-- 채팅방
INSERT INTO chat_rooms (room_name, room_type) VALUES
    ('개발팀 전체방', 'M');

-- 채팅방 멤버
INSERT INTO chat_room_members (room_no, emp_id) VALUES
                                                    (1, '2024001'),
                                                    (1, '2024002');

-- =========================
-- 6. 자원 예약
-- =========================

-- 자원 등록
INSERT INTO meeting_room (com_id, room_name, cap, loc) VALUES ('C01', '대회의실', 10, '10F');
INSERT INTO corporate_car (com_id, car_name, plate_no, cap) VALUES ('C01', '제네시스', '12가3456', 4);
INSERT INTO shared_equipment (com_id, eq_name, eq_id) VALUES ('C01', '맥북 프로', 'EQ-001');

-- 예약
INSERT INTO meeting_room_reservation (com_id, room_no, started_at, ended_at, resv_emp, purp) VALUES
    ('C01', 1, '2025-01-20 10:00:00', '2025-01-20 12:00:00', '2024002', '주간 회의');

-- =========================
-- 7. 파일 드라이브
-- =========================

-- 폴더 (루트)
INSERT INTO folder (com_id, dep_no, folder_name, owner_id, scope, path) VALUES
    ('C01', 2, '개발팀 공유폴더', '2024001', 'dept', '/dev_shared');

-- 파일
INSERT INTO `file` (com_id, folder_no, file_name, size, emp_id, path) VALUES
    ('C01', 1, 'API_Spec_v1.0.pdf', 102400, '2024002', '/dev_shared/API_Spec_v1.0.pdf');

-- =========================
-- 8. 게시판
-- =========================

-- 공지사항
INSERT INTO notice (com_id, title, contents, emp_id, is_popup) VALUES
    ('C01', '설 연휴 공지', '{"text": "설 연휴 잘 보내세요"}', '2024001', 1);

-- 게시판 카테고리
INSERT INTO board_cat (cat_code, cat_descript) VALUES ('F', '자유게시판');

-- 자유게시판 글
INSERT INTO board (com_id, title, contents, cat_code, emp_id) VALUES
    ('C01', '오늘 점심 뭐 먹지?', '{"text": "추천 받습니다"}', 'F', '2024002');

-- 댓글
INSERT INTO comment (com_id, board_no, contents, emp_id) VALUES
    ('C01', 1, '국밥 어떠신가요?', '2024003');

-- =========================
-- 9. 메일 (Mail)
-- =========================

-- 메일 원본 (김대표 -> 이사원)
INSERT INTO mail (mail_id, sender_id, title, cntt) VALUES
    ('MAIL-001', '2024001', '프로젝트 진행 상황 보고 바랍니다.', '{"body": "언제까지 될까요?"}');

-- 메일 사용자 상태
-- 1. 발신자(김대표) 상태
INSERT INTO mail_user_state (mail_id, user_id, role, is_read) VALUES
    ('MAIL-001', '2024001', 'SENDER', 1);

-- 2. 수신자(이사원) 상태
INSERT INTO mail_user_state (mail_id, user_id, role, is_read) VALUES
    ('MAIL-001', '2024002', 'RECIPIENT', 0);

-- =========================
-- 10. 결제 및 근태
-- =========================

INSERT INTO payment_method (com_id, paym_type, billing_key) VALUES
    ('C01', 'C', 'BILL_KEY_123456');

INSERT INTO attendance (com_id, emp_id, doc_id, type, day, created_at, ended_at) VALUES
    ('C01', '2024002', 'DOC-2024-001', 'V', 1, '2024-07-01 09:00:00', '2024-07-01 18:00:00');

-- ========================================================
-- [설정] FK 체크 재활성화
-- ========================================================
SET FOREIGN_KEY_CHECKS = 1;