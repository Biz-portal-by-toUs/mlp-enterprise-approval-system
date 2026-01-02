-- ========================================================
-- [설정] 외래키 검사 일시 해제
-- ========================================================
SET FOREIGN_KEY_CHECKS = 0;

-- DB 생성
CREATE DATABASE IF NOT EXISTS bizportal
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE bizportal;

-- ========================================================
-- 1. 공통/조직 (Organization)
-- ========================================================

-- 1) 요금제
CREATE TABLE IF NOT EXISTS subscription (
                                            sub_no      INT           NOT NULL AUTO_INCREMENT,
                                            sub_name    VARCHAR(20)   NOT NULL,
                                            sub_desc    TEXT          NULL,
                                            sub_price   DECIMAL(8,0)  NOT NULL,
                                            CONSTRAINT pk_subscription PRIMARY KEY (sub_no),
                                            CONSTRAINT uk_subscription_name UNIQUE (sub_name),
                                            CONSTRAINT ck_subscription_name CHECK (sub_name IN ('basic','pro','ultimate'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) 회사 (✅ role 컬럼 반영)
CREATE TABLE IF NOT EXISTS company (
                                       com_no     BIGINT       NOT NULL AUTO_INCREMENT,
                                       com_id     VARCHAR(3)   NOT NULL,
                                       com_name   VARCHAR(50)  NOT NULL,
                                       email      VARCHAR(50)  NOT NULL,
                                       pwd        VARCHAR(255) NOT NULL,
                                       brn        VARCHAR(10)  NOT NULL,
                                       emp_cnt    INT          NOT NULL,
                                       addr       VARCHAR(100) NOT NULL,

                                       role       VARCHAR(20)  NOT NULL DEFAULT 'COM_ADMIN',  -- ✅ 추가 (SYS_ADMIN/COM_ADMIN 등)

                                       sub_no     INT          NOT NULL,
                                       created_at TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       img_url    VARCHAR(255) NULL,
                                       path       VARCHAR(255) NULL,

                                       CONSTRAINT pk_company PRIMARY KEY (com_no),
                                       CONSTRAINT uk_company_com_id UNIQUE (com_id),
                                       CONSTRAINT uk_company_email UNIQUE (email),
                                       CONSTRAINT fk_company_subscription
                                           FOREIGN KEY (sub_no) REFERENCES subscription(sub_no)
                                               ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) 부서
CREATE TABLE IF NOT EXISTS department (
                                          dep_no    BIGINT      NOT NULL AUTO_INCREMENT,
                                          dep_id    VARCHAR(3)  NOT NULL,
                                          dep_name  VARCHAR(10) NOT NULL,
                                          com_id    VARCHAR(3)  NOT NULL,

                                          CONSTRAINT pk_department PRIMARY KEY (dep_no),
                                          CONSTRAINT uk_department_composite UNIQUE (com_id, dep_id),
                                          CONSTRAINT fk_department_company
                                              FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                  ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) 직급
CREATE TABLE IF NOT EXISTS positions (
                                         pos_no    BIGINT      NOT NULL AUTO_INCREMENT,
                                         pos_name  VARCHAR(10) NOT NULL,
                                         com_id    VARCHAR(3)  NOT NULL,
                                         pos_order int         not null,

                                         CONSTRAINT pk_positions PRIMARY KEY (pos_no),
                                         CONSTRAINT fk_positions_company
                                             FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                 ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- 5) 사원 (✅ role_no 제거, ✅ role 추가, ✅ atte/msg_stat default 'c')
CREATE TABLE IF NOT EXISTS employee (
                                        emp_no      BIGINT       NOT NULL AUTO_INCREMENT,
                                        com_id      VARCHAR(3)   NOT NULL,
                                        dep_no      BIGINT       NOT NULL,
                                        pos_no      BIGINT       NOT NULL,

                                        emp_id      VARCHAR(7)   NOT NULL,
                                        pwd         VARCHAR(255) NOT NULL,
                                        emp_name    VARCHAR(20)  NOT NULL,
                                        email       VARCHAR(50)  NOT NULL,
                                        phone       VARCHAR(15)  NOT NULL,
                                        work_phone  VARCHAR(10)  NOT NULL,
                                        gen         VARCHAR(1)   NOT NULL,
                                        hire_date   DATETIME     NOT NULL,
                                        ret_date    DATETIME     NULL,
                                        addr        VARCHAR(100) NOT NULL,

                                        role        VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE', -- ✅ 추가 (COM_ADMIN/SEC_ADMIN/THR_ADMIN/EMPLOYEE)

                                        is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,

                                        atte        CHAR(1)      NOT NULL DEFAULT 'c', -- ✅ default
                                        msg_stat    CHAR(1)      NOT NULL DEFAULT 'c', -- ✅ default

                                        delegate    VARCHAR(7)   NULL,

                                        CONSTRAINT pk_employee PRIMARY KEY (emp_no),
                                        CONSTRAINT uk_employee_emp_id UNIQUE (emp_id),

                                        CONSTRAINT fk_employee_company
                                            FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                ON UPDATE CASCADE ON DELETE CASCADE,

                                        CONSTRAINT fk_employee_department
                                            FOREIGN KEY (dep_no) REFERENCES department(dep_no)
                                                ON UPDATE CASCADE ON DELETE CASCADE,

                                        CONSTRAINT fk_employee_positions
                                            FOREIGN KEY (pos_no) REFERENCES positions(pos_no)
                                                ON UPDATE CASCADE ON DELETE CASCADE,

                                        CONSTRAINT fk_employee_delegate
                                            FOREIGN KEY (delegate) REFERENCES employee(emp_id)
                                                ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 2. 일정 및 할일
-- ========================================================

CREATE TABLE IF NOT EXISTS schedule (
                                        sch_no    BIGINT       NOT NULL AUTO_INCREMENT,
                                        com_id    VARCHAR(3)   NOT NULL,
                                        dep_no    BIGINT       NOT NULL,
                                        title     VARCHAR(100) NOT NULL,
                                        content   VARCHAR(200) NOT NULL,
                                        start_at  DATETIME     NOT NULL,
                                        ended_at  DATETIME     NOT NULL,
                                        color     VARCHAR(10)  NOT NULL,
                                        reg_emp   VARCHAR(7)   NOT NULL,

                                        CONSTRAINT pk_schedule PRIMARY KEY (sch_no),
                                        CONSTRAINT fk_schedule_company
                                            FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                ON UPDATE CASCADE ON DELETE CASCADE,
                                        CONSTRAINT fk_schedule_department
                                            FOREIGN KEY (dep_no) REFERENCES department(dep_no)
                                                ON UPDATE CASCADE ON DELETE CASCADE,
                                        CONSTRAINT fk_schedule_reg_emp
                                            FOREIGN KEY (reg_emp) REFERENCES employee(emp_id)
                                                ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emp_schedule (
                                            sch_no    BIGINT       NOT NULL AUTO_INCREMENT,
                                            emp_id    VARCHAR(7)   NOT NULL,
                                            title     VARCHAR(100) NOT NULL,
                                            content   VARCHAR(200) NOT NULL,
                                            start_at  DATETIME     NOT NULL,
                                            ended_at  DATETIME     NOT NULL,
                                            color     VARCHAR(10)  NOT NULL,

                                            CONSTRAINT pk_emp_schedule PRIMARY KEY (sch_no),
                                            CONSTRAINT fk_emp_schedule_employee
                                                FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS todo_list (
                                         todo_no  BIGINT       NOT NULL AUTO_INCREMENT,
                                         emp_id   VARCHAR(7)   NOT NULL,
                                         title    VARCHAR(100) NOT NULL,
                                         is_done  BOOLEAN      NOT NULL,

                                         CONSTRAINT pk_todo_list PRIMARY KEY (todo_no),
                                         CONSTRAINT fk_todo_employee
                                             FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                 ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 3. 전자결재 및 문서
-- ========================================================

CREATE TABLE IF NOT EXISTS document_form (
                                             docfo_no      BIGINT NOT NULL AUTO_INCREMENT COMMENT '문서양식 식별자',
                                             com_id        VARCHAR(3) NOT NULL COMMENT '회사 코드',
                                             writer_id     VARCHAR(7) NOT NULL COMMENT '작성자 사원번호',
                                             docfo_name    VARCHAR(100) NOT NULL COMMENT '문서 양식 이름',
                                             cntt_json     JSON NOT NULL COMMENT '내용(JSON)',
                                             cntt_html     mediumtext NULL COMMENT '내용(HTML)',
                                             created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             docfo_stat    CHAR(1) NOT NULL COMMENT 'T:임시저장, P:대기, R:반려, A:승인, D:삭제',
                                             reject_reason VARCHAR(255) NULL COMMENT '반려 이유',

                                             PRIMARY KEY (docfo_no),
                                             CONSTRAINT ck_docform_stat CHECK (docfo_stat IN ('T','P','R','A','D')),
                                             CONSTRAINT fk_docform_company
                                                 FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_form_category (
                                                      docfo_cat_no BIGINT NOT NULL AUTO_INCREMENT,
                                                      com_id       VARCHAR(3) NOT NULL,
                                                      name         VARCHAR(100) NOT NULL,
                                                      docfo_no     BIGINT NOT NULL,

                                                      PRIMARY KEY (docfo_cat_no),
                                                      CONSTRAINT fk_docfocat_company
                                                          FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                              ON UPDATE CASCADE ON DELETE CASCADE,
                                                      CONSTRAINT fk_docfocat_docform
                                                          FOREIGN KEY (docfo_no) REFERENCES document_form(docfo_no)
                                                              ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attach_box (
                                          attach_no BIGINT NOT NULL AUTO_INCREMENT,
                                          com_id    VARCHAR(3) NOT NULL,
                                          uploader  VARCHAR(7) NOT NULL,
                                          title     VARCHAR(100) NOT NULL,
                                          dscp      TEXT NULL,
                                          path      VARCHAR(255) NOT NULL,
                                          size      BIGINT NOT NULL,

                                          PRIMARY KEY (attach_no),
                                          CONSTRAINT fk_attach_box_company
                                              FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document (
                                        doc_no        BIGINT        NOT NULL AUTO_INCREMENT,
                                        com_id        VARCHAR(3)    NOT NULL,
                                        doc_id        VARCHAR(14)   NULL unique,
                                        docfo_cat_no  BIGINT        NOT NULL,
                                        docfo_no      BIGINT        NOT NULL,
                                        title         VARCHAR(100)  NOT NULL,
                                        content       mediumTEXT    not NULL,
                                        cntt_html     mediumtext    NULL,
                                        emp_id        VARCHAR(7)    NOT NULL,
                                        ai_summ       TEXT          NULL,
                                        temp          BOOLEAN       NOT NULL DEFAULT FALSE,
                                        submitted_at  TIMESTAMP      NULL,
                                        doc_stat      varchar(2)    NOT NULL DEFAULT 'AW',

                                        created_at    TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at    TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                        is_resubmitted boolean      null default false,
                                        resubmitted_by_doc_no  bigint null,
                                        resubmitted_for_doc_no bigint null,
                                        atte_no        bigint        null,

                                        CONSTRAINT pk_document PRIMARY KEY (doc_no),
                                        CONSTRAINT uk_document_doc_id UNIQUE (doc_id),
                                        CONSTRAINT ck_document_stat CHECK (doc_stat IN ('US', 'AW', 'RJ', 'FI')),

                                        CONSTRAINT fk_document_company
                                            FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                ON UPDATE CASCADE ON DELETE CASCADE,

                                        CONSTRAINT fk_document_employee
                                            FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                ON UPDATE CASCADE,

                                        CONSTRAINT fk_document_form
                                            FOREIGN KEY (docfo_no) REFERENCES document_form(docfo_no)
                                                ON UPDATE CASCADE,

                                        CONSTRAINT fk_document_form_cat
                                            FOREIGN KEY (docfo_cat_no) REFERENCES document_form_category(docfo_cat_no)
                                                ON UPDATE CASCADE,

                                        CONSTRAINT fk_document_resubmitted_by
                                            FOREIGN KEY (resubmitted_by_doc_no) REFERENCES document(doc_no)
                                                ON UPDATE CASCADE ON DELETE SET NULL,

                                        CONSTRAINT fk_document_resubmitted_for
                                            FOREIGN KEY (resubmitted_for_doc_no) REFERENCES document(doc_no)
                                                ON UPDATE CASCADE ON DELETE SET NULL,

                                        CONSTRAINT fk_document_attendance
                                            FOREIGN KEY (atte_no) REFERENCES attendance(atte_no)
                                                ON UPDATE CASCADE ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS approval_line (
                                             apprl_no    BIGINT        NOT NULL AUTO_INCREMENT,
                                             com_id      VARCHAR(3)    NOT NULL,
                                             doc_no      BIGINT        NOT NULL,
                                             emp_id      VARCHAR(7)    NOT NULL,
                                             seq         INT           NOT NULL,
                                             appr_stat   CHAR(1)       NOT NULL DEFAULT 'W',
                                             ended_at    TIMESTAMP     NULL,
                                             is_actual_appr boolean    not null,
                                             rej_reason  text          null,
                                             is_delegate boolean       not null default false,
                                             target_emp_id  VARCHAR(7) NULL COMMENT '권한 위임자 (누구를 대신하는가)',

                                             created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at     TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                             CONSTRAINT pk_approval_line PRIMARY KEY (apprl_no),
                                             CONSTRAINT ck_approval_line_stat CHECK (appr_stat IN ('I', 'W', 'A', 'R')),

                                             CONSTRAINT fk_approval_line_company
                                                 FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,

                                             CONSTRAINT fk_approval_line_document
                                                 FOREIGN KEY (doc_no) REFERENCES document(doc_no)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,

                                             CONSTRAINT fk_approval_line_employee
                                                 FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                     ON UPDATE CASCADE,

                                             CONSTRAINT fk_approval_line_target
                                                 FOREIGN KEY (target_emp_id) REFERENCES employee(emp_id)
                                                     ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# CREATE TABLE IF NOT EXISTS document_file (
#                                              docfi_no    BIGINT        NOT NULL AUTO_INCREMENT,
#                                              com_id      VARCHAR(3)    NOT NULL,
#                                              doc_no      BIGINT        NOT NULL,
#                                              docfi_name  VARCHAR(255)  NOT NULL,
#                                              folder_path VARCHAR(255)  NULL,
#
#                                              CONSTRAINT pk_document_file PRIMARY KEY (docfi_no),
#                                              CONSTRAINT fk_document_file_company
#                                                  FOREIGN KEY (com_id) REFERENCES company(com_id)
#                                                      ON UPDATE CASCADE ON DELETE CASCADE,
#                                              CONSTRAINT fk_document_file_document
#                                                  FOREIGN KEY (doc_no) REFERENCES document(doc_no)
#                                                      ON UPDATE CASCADE ON DELETE CASCADE
# ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prov_document (
                                             prov_no     BIGINT NOT NULL AUTO_INCREMENT COMMENT '파일 식별자',
                                             com_id      VARCHAR(3) NOT NULL COMMENT '회사 코드',
                                             doc_title   VARCHAR(50) COMMENT '제목',
                                             description TEXT COMMENT '규정 요약 내용',
                                             is_public   BOOLEAN DEFAULT TRUE COMMENT '공개 여부',
                                             file_name   VARCHAR(255) NOT NULL COMMENT '파일 이름',
                                             file_url    VARCHAR(500) NOT NULL COMMENT '파일 경로',
                                             file_size   BIGINT COMMENT '파일 크기',
                                             chunk_cnt   INT COMMENT '청크 수',
                                             proc_stat   VARCHAR(20) NOT NULL COMMENT '처리 상태',
                                             error_msg   TEXT COMMENT '에러 메세지',
                                             created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                             PRIMARY KEY (prov_no),
                                             CONSTRAINT fk_prov_doc_company
                                                 FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                     ON DELETE CASCADE
) COMMENT='규정 파일 메타데이터' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 4. 커뮤니케이션 (Meeting & Chat)
-- ========================================================

CREATE TABLE IF NOT EXISTS meeting (
                                       meet_no     BIGINT NOT NULL AUTO_INCREMENT COMMENT '회의록 식별자',
                                       com_id      VARCHAR(3) NOT NULL COMMENT '회사 코드',
                                       dep_no      BIGINT NOT NULL COMMENT '부서 식별자',
                                       emp_id      VARCHAR(7) NOT NULL COMMENT '사원 번호 (작성자)',
                                       title       VARCHAR(100) NOT NULL COMMENT '제목',
                                       stt_text    MEDIUMTEXT COMMENT 'stt 변환 텍스트',
                                       ai_text     MEDIUMTEXT COMMENT 'ai 요약',
                                       started_at  DATETIME NOT NULL COMMENT '회의 시작 시간',
                                       is_deleted  BOOLEAN DEFAULT FALSE COMMENT '삭제 여부',
                                       created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                       PRIMARY KEY (meet_no),
                                       CONSTRAINT fk_meeting_company
                                           FOREIGN KEY (com_id) REFERENCES company(com_id)
                                               ON DELETE CASCADE,
                                       CONSTRAINT fk_meeting_department
                                           FOREIGN KEY (dep_no) REFERENCES department(dep_no)
                                               ON DELETE CASCADE,
                                       CONSTRAINT fk_meeting_employee
                                           FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                               ON DELETE CASCADE
) COMMENT='회의' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS meeting_emp (
                                           meemp_no BIGINT NOT NULL AUTO_INCREMENT COMMENT '회의 참석자 식별자',
                                           com_id   VARCHAR(3) NOT NULL COMMENT '회사 코드',
                                           emp_id   VARCHAR(7) NOT NULL COMMENT '사원 번호',
                                           meet_no  BIGINT NOT NULL COMMENT '회의록 식별자',

                                           PRIMARY KEY (meemp_no),
                                           CONSTRAINT fk_meeting_emp_company
                                               FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                   ON DELETE CASCADE,
                                           CONSTRAINT fk_meeting_emp_employee
                                               FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                   ON DELETE CASCADE,
                                           CONSTRAINT fk_meeting_emp_meeting
                                               FOREIGN KEY (meet_no) REFERENCES meeting(meet_no)
                                                   ON DELETE CASCADE
) COMMENT='회의 참석자' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_rooms (
                                          room_no    BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅방 식별자',
                                          room_name  VARCHAR(30) COMMENT '채팅방 이름',
                                          room_type VARCHAR(10) NOT NULL COMMENT '채팅방 타입 (ONE, GROUP)',
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                          PRIMARY KEY (room_no)
) COMMENT='채팅방' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_room_members (
                                                 romem_no         BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅방 멤버 식별자',
                                                 room_no          BIGINT NOT NULL COMMENT '채팅방 식별자',
                                                 emp_id           VARCHAR(7) NOT NULL COMMENT '사원 번호',
                                                 joined_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입장 일시',
                                                 last_read_msg_id VARCHAR(50) COMMENT '마지막으로 읽은 MongoDB 메세지 ID',

                                                 PRIMARY KEY (romem_no),
                                                 CONSTRAINT fk_chat_mem_room
                                                     FOREIGN KEY (room_no) REFERENCES chat_rooms(room_no)
                                                         ON DELETE CASCADE,
                                                 CONSTRAINT fk_chat_mem_employee
                                                     FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                         ON DELETE CASCADE
) COMMENT='채팅방 멤버' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_logs (
                                           log_no    BIGINT NOT NULL AUTO_INCREMENT COMMENT '로그 식별자',
                                           com_id    VARCHAR(3) NOT NULL COMMENT '회사 코드',
                                           level     VARCHAR(10) NOT NULL COMMENT '로그 레벨',
                                           message   TEXT NOT NULL COMMENT '로그 메세지',
                                           timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           PRIMARY KEY (log_no),
                                           CONSTRAINT fk_sys_log_company
                                               FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                   ON DELETE CASCADE
) COMMENT='rag 사용 로그' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 5. 자원 예약 (Reservation)
-- ========================================================

CREATE TABLE IF NOT EXISTS meeting_room (
                                            room_no    BIGINT NOT NULL AUTO_INCREMENT,
                                            com_id     VARCHAR(3) NOT NULL,
                                            room_name  VARCHAR(20) NOT NULL,
                                            cap        INT NOT NULL,
                                            loc        VARCHAR(20) NOT NULL,
                                            img_url    VARCHAR(255) NULL,
                                            equip_list VARCHAR(255) NULL,
                                            note       VARCHAR(255) NULL,

                                            PRIMARY KEY (room_no),
                                            CONSTRAINT fk_meeting_room_company
                                                FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS corporate_car (
                                             car_no   BIGINT NOT NULL AUTO_INCREMENT,
                                             com_id   VARCHAR(3) NOT NULL,
                                             car_name VARCHAR(20) NOT NULL,
                                             car_type VARCHAR(20) NULL,
                                             plate_no VARCHAR(20) NOT NULL,
                                             cap      INT NOT NULL,
                                             fuel     VARCHAR(10) NULL,
                                             img_url  VARCHAR(255) NULL,

                                             PRIMARY KEY (car_no),
                                             CONSTRAINT fk_corporate_car_company
                                                 FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shared_equipment (
                                                eq_no      BIGINT NOT NULL AUTO_INCREMENT,
                                                com_id     VARCHAR(3) NOT NULL,
                                                eq_name    VARCHAR(20) NOT NULL,
                                                eq_id      VARCHAR(20) NOT NULL,
                                                model_name VARCHAR(20) NULL,
                                                img_url    VARCHAR(255) NULL,
                                                loc        VARCHAR(20) NULL,

                                                PRIMARY KEY (eq_no),
                                                CONSTRAINT fk_shared_equipment_company
                                                    FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS meeting_room_reservation (
                                                        meeting_resv_no BIGINT NOT NULL AUTO_INCREMENT,
                                                        com_id          VARCHAR(3) NOT NULL,
                                                        room_no         BIGINT NOT NULL,
                                                        started_at      DATETIME NOT NULL,
                                                        ended_at        DATETIME NOT NULL,
                                                        resv_emp        VARCHAR(7) NOT NULL,
                                                        purp            VARCHAR(100) NULL,
                                                        is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,

                                                        PRIMARY KEY (meeting_resv_no),
                                                        CONSTRAINT fk_mrr_company
                                                            FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                                ON DELETE CASCADE,
                                                        CONSTRAINT fk_mrr_room
                                                            FOREIGN KEY (room_no) REFERENCES meeting_room(room_no)
                                                                ON DELETE CASCADE,
                                                        CONSTRAINT fk_mrr_employee
                                                            FOREIGN KEY (resv_emp) REFERENCES employee(emp_id)
                                                                ON DELETE CASCADE,
                                                        CONSTRAINT ck_mrr_time CHECK (ended_at > started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS meeting_room_attendee (
                                                     atte_no         BIGINT NOT NULL AUTO_INCREMENT,
                                                     com_id          VARCHAR(3) NOT NULL,
                                                     meeting_resv_no BIGINT NOT NULL,
                                                     emp_id          VARCHAR(7) NOT NULL,

                                                     PRIMARY KEY (atte_no),
                                                     UNIQUE KEY uq_mra_unique (meeting_resv_no, emp_id),

                                                     CONSTRAINT fk_mra_company
                                                         FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                             ON DELETE CASCADE,
                                                     CONSTRAINT fk_mra_resv
                                                         FOREIGN KEY (meeting_resv_no) REFERENCES meeting_room_reservation(meeting_resv_no)
                                                             ON DELETE CASCADE,
                                                     CONSTRAINT fk_mra_emp
                                                         FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                             ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS corporate_car_reservation (
                                                         car_resv_no BIGINT NOT NULL AUTO_INCREMENT,
                                                         com_id      VARCHAR(3) NOT NULL,
                                                         car_no      BIGINT NOT NULL,
                                                         started_at  DATETIME NOT NULL,
                                                         ended_at    DATETIME NOT NULL,
                                                         resv_emp    VARCHAR(7) NOT NULL,
                                                         purp        VARCHAR(100) NULL,
                                                         is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,

                                                         PRIMARY KEY (car_resv_no),
                                                         CONSTRAINT fk_ccr_company
                                                             FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                                 ON DELETE CASCADE,
                                                         CONSTRAINT fk_ccr_car
                                                             FOREIGN KEY (car_no) REFERENCES corporate_car(car_no)
                                                                 ON DELETE CASCADE,
                                                         CONSTRAINT fk_ccr_employee
                                                             FOREIGN KEY (resv_emp) REFERENCES employee(emp_id)
                                                                 ON DELETE CASCADE,
                                                         CONSTRAINT ck_ccr_time CHECK (ended_at > started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shared_equipment_reservation (
                                                            eq_resv_no BIGINT NOT NULL AUTO_INCREMENT,
                                                            com_id     VARCHAR(3) NOT NULL,
                                                            eq_no      BIGINT NOT NULL,
                                                            started_at DATETIME NOT NULL,
                                                            ended_at   DATETIME NOT NULL,
                                                            resv_emp   VARCHAR(7) NOT NULL,
                                                            purp       VARCHAR(100) NULL,
                                                            is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

                                                            PRIMARY KEY (eq_resv_no),
                                                            CONSTRAINT fk_ser_company
                                                                FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                                    ON DELETE CASCADE,
                                                            CONSTRAINT fk_ser_equipment
                                                                FOREIGN KEY (eq_no) REFERENCES shared_equipment(eq_no)
                                                                    ON DELETE CASCADE,
                                                            CONSTRAINT fk_ser_employee
                                                                FOREIGN KEY (resv_emp) REFERENCES employee(emp_id)
                                                                    ON DELETE CASCADE,
                                                            CONSTRAINT ck_ser_time CHECK (ended_at > started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 6. 파일 드라이브 (File Drive)
-- ========================================================

# CREATE TABLE IF NOT EXISTS folder (
#                                       folder_no   BIGINT NOT NULL AUTO_INCREMENT,
#                                       com_id      VARCHAR(3) NOT NULL,
#                                       dep_no      BIGINT NULL,
#                                       parent_id   BIGINT NULL,
#                                       folder_name VARCHAR(255) NOT NULL,
#                                       owner_id    VARCHAR(7) NOT NULL,
#                                       scope       VARCHAR(10) NOT NULL,
#                                       created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
#                                       updated_at  TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
#                                       path        VARCHAR(255) NOT NULL,
#
#                                       PRIMARY KEY (folder_no),
#                                       CONSTRAINT fk_folder_company
#                                           FOREIGN KEY (com_id) REFERENCES company(com_id)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT fk_folder_department
#                                           FOREIGN KEY (dep_no) REFERENCES department(dep_no)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT fk_folder_owner
#                                           FOREIGN KEY (owner_id) REFERENCES employee(emp_id)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT fk_folder_parent
#                                           FOREIGN KEY (parent_id) REFERENCES folder(folder_no)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT ck_folder_scope CHECK (scope IN ('all','dept','prvt'))
# ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# CREATE TABLE IF NOT EXISTS `file` (
#                                       file_no    BIGINT NOT NULL AUTO_INCREMENT,
#                                       com_id     VARCHAR(3) NOT NULL,
#                                       folder_no  BIGINT NOT NULL,
#                                       file_name  VARCHAR(255) NOT NULL,
#                                       size       BIGINT NOT NULL,
#                                       emp_id     VARCHAR(7) NOT NULL,
#                                       path       VARCHAR(255) NOT NULL,
#                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
#
#                                       PRIMARY KEY (file_no),
#                                       CONSTRAINT fk_file_company
#                                           FOREIGN KEY (com_id) REFERENCES company(com_id)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT fk_file_folder
#                                           FOREIGN KEY (folder_no) REFERENCES folder(folder_no)
#                                               ON DELETE CASCADE,
#                                       CONSTRAINT fk_file_emp
#                                           FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
#                                               ON DELETE CASCADE
# ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# CREATE TABLE IF NOT EXISTS folder_permission (
#                                                  perm_no   BIGINT NOT NULL AUTO_INCREMENT,
#                                                  com_id    VARCHAR(3) NOT NULL,
#                                                  folder_no BIGINT NOT NULL,
#                                                  emp_id    VARCHAR(7) NOT NULL,
#                                                  perm_type CHAR(1) NOT NULL,
#
#                                                  PRIMARY KEY (perm_no),
#                                                  UNIQUE KEY uq_fp_unique (folder_no, emp_id),
#
#                                                  CONSTRAINT fk_fp_company
#                                                      FOREIGN KEY (com_id) REFERENCES company(com_id)
#                                                          ON DELETE CASCADE,
#                                                  CONSTRAINT fk_fp_folder
#                                                      FOREIGN KEY (folder_no) REFERENCES folder(folder_no)
#                                                          ON DELETE CASCADE,
#                                                  CONSTRAINT fk_fp_emp
#                                                      FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
#                                                          ON DELETE CASCADE,
#                                                  CONSTRAINT ck_fp_perm CHECK (perm_type IN ('R','W'))
# ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 7. 게시판 (Board & Notice)
-- ========================================================

CREATE TABLE IF NOT EXISTS board_cat (
                                         board_cat_no BIGINT NOT NULL AUTO_INCREMENT,
                                         cat_code     CHAR(1) NOT NULL,
                                         cat_descript VARCHAR(100) NOT NULL,

                                         PRIMARY KEY (board_cat_no),
                                         UNIQUE KEY uk_board_cat_code (cat_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
                                      notice_no   BIGINT NOT NULL AUTO_INCREMENT,
                                      com_id      VARCHAR(3) NOT NULL,
                                      is_deleted  BOOLEAN DEFAULT FALSE,
                                      title       VARCHAR(100) NOT NULL,
                                      contents    JSON NOT NULL,
                                      is_popup    BOOLEAN DEFAULT FALSE,
                                      started_at  DATETIME,
                                      ended_at    DATETIME,
                                      emp_id      VARCHAR(7) NOT NULL,
                                      rating      INT NOT NULL DEFAULT 0,
                                      created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                      PRIMARY KEY (notice_no),
                                      CONSTRAINT fk_notice_company
                                          FOREIGN KEY (com_id) REFERENCES company(com_id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_notice_employee
                                          FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                              ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice_attach (
                                             notice_attach_no BIGINT NOT NULL AUTO_INCREMENT,
                                             com_id           VARCHAR(3) NOT NULL,
                                             notice_no        BIGINT NOT NULL,
                                             org_name         VARCHAR(255) NOT NULL,
                                             folder_path      VARCHAR(255) NOT NULL,

                                             PRIMARY KEY (notice_attach_no),
                                             CONSTRAINT fk_notice_attach_company
                                                 FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,
                                             CONSTRAINT fk_notice_attach_notice
                                                 FOREIGN KEY (notice_no) REFERENCES notice(notice_no)
                                                     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS board (
                                     board_no    BIGINT NOT NULL AUTO_INCREMENT,
                                     com_id      VARCHAR(3) NOT NULL,
                                     is_deleted  BOOLEAN DEFAULT FALSE,
                                     title       VARCHAR(100) NOT NULL,
                                     contents    JSON NOT NULL,
                                     cat_code    CHAR(1) NOT NULL,
                                     emp_id      VARCHAR(7) NOT NULL,
                                     rating      INT NOT NULL DEFAULT 0,
                                     created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                     PRIMARY KEY (board_no),
                                     CONSTRAINT fk_board_company
                                         FOREIGN KEY (com_id) REFERENCES company(com_id)
                                             ON UPDATE CASCADE ON DELETE CASCADE,
                                     CONSTRAINT fk_board_employee
                                         FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                             ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comment (
                                       comment_no  BIGINT NOT NULL AUTO_INCREMENT,
                                       com_id      VARCHAR(3) NOT NULL,
                                       board_no    BIGINT NOT NULL,
                                       contents    TEXT NOT NULL,
                                       emp_id      VARCHAR(7) NOT NULL,
                                       created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                       PRIMARY KEY (comment_no),
                                       CONSTRAINT fk_comment_company
                                           FOREIGN KEY (com_id) REFERENCES company(com_id)
                                               ON UPDATE CASCADE ON DELETE CASCADE,
                                       CONSTRAINT fk_comment_board
                                           FOREIGN KEY (board_no) REFERENCES board(board_no)
                                               ON UPDATE CASCADE ON DELETE CASCADE,
                                       CONSTRAINT fk_comment_employee
                                           FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                               ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS board_attach (
                                            board_attach_no BIGINT NOT NULL AUTO_INCREMENT,
                                            com_id          VARCHAR(3) NOT NULL,
                                            board_no        BIGINT NOT NULL,
                                            org_name        VARCHAR(255) NOT NULL,
                                            folder_path     VARCHAR(255) NOT NULL,

                                            PRIMARY KEY (board_attach_no),
                                            CONSTRAINT fk_board_attach_company
                                                FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                    ON UPDATE CASCADE ON DELETE CASCADE,
                                            CONSTRAINT fk_board_attach_board
                                                FOREIGN KEY (board_no) REFERENCES board(board_no)
                                                    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 8. 결제 및 근태 (Billing & HR)
-- ========================================================

CREATE TABLE IF NOT EXISTS payment_method (
                                              paym_no     BIGINT        NOT NULL AUTO_INCREMENT,
                                              com_id      VARCHAR(3)    NOT NULL,
                                              paym_type   CHAR(1)       NOT NULL,
                                              card_type   VARCHAR(30)   NULL,
                                              billing_key VARCHAR(255)  NOT NULL,
                                              mask        VARCHAR(20)   NULL,
                                              active      BOOLEAN       NOT NULL DEFAULT TRUE,
                                              created_at  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                              CONSTRAINT pk_payment_method PRIMARY KEY (paym_no),
                                              CONSTRAINT uk_payment_method_bill UNIQUE (billing_key),
                                              CONSTRAINT uk_payment_method_card UNIQUE (com_id, mask),
                                              CONSTRAINT ck_payment_method_type CHECK (paym_type IN ('C','A')),
                                              CONSTRAINT fk_payment_method_company
                                                  FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                      ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_history (
                                               payh_no     BIGINT        NOT NULL AUTO_INCREMENT,
                                               com_id      VARCHAR(3)    NOT NULL,
                                               amount      DECIMAL(8,0)  NOT NULL,
                                               pay_result  BOOLEAN       NOT NULL DEFAULT FALSE,
                                               paym_no     BIGINT        NOT NULL,
                                               created_at  DATETIME      NULL DEFAULT CURRENT_TIMESTAMP,

                                               CONSTRAINT pk_payment_history PRIMARY KEY (payh_no),
                                               CONSTRAINT fk_payment_history_company
                                                   FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE,
                                               CONSTRAINT fk_payment_history_method
                                                   FOREIGN KEY (paym_no) REFERENCES payment_method(paym_no)
                                                       ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attendance (
                                          atte_no     BIGINT        NOT NULL AUTO_INCREMENT,
                                          com_id      VARCHAR(3)    NOT NULL,
                                          emp_id      VARCHAR(7)    NOT NULL,
                                          doc_no      bigint        NOT NULL,
                                          type        CHAR(1)       NOT NULL,
                                          day         INT           NOT NULL DEFAULT 1,
                                          delegate    VARCHAR(7)    NULL,
                                          start_at    datetime      not null,
                                          end_at      DATETIME      NOT NULL,

                                          created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at  TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                          CONSTRAINT pk_attendance PRIMARY KEY (atte_no),
                                          CONSTRAINT fk_attendance_company
                                              FOREIGN KEY (com_id) REFERENCES company(com_id)
                                                  ON UPDATE CASCADE ON DELETE CASCADE,
                                          CONSTRAINT fk_attendance_employee
                                              FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
                                                  ON DELETE CASCADE,
                                          CONSTRAINT fk_attendance_document
                                              FOREIGN KEY (doc_no) REFERENCES document(doc_no),
                                          CONSTRAINT fk_attendance_delegate
                                              FOREIGN KEY (delegate) REFERENCES employee(emp_id)
                                                  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 9. 메일 (Mail)
-- ========================================================

CREATE TABLE IF NOT EXISTS mail (
                                    mail_no    BIGINT NOT NULL AUTO_INCREMENT COMMENT '메일 식별자',
                                    mail_id    VARCHAR(100) NOT NULL COMMENT '메일 코드(사번+작성날짜ms)',
                                    sender_id  VARCHAR(7) NOT NULL COMMENT '발신자 사원번호',
                                    title      VARCHAR(100) NOT NULL COMMENT '메일 제목',
                                    cntt       JSON NOT NULL COMMENT '메일 내용(JSON)',
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '작성 시각',

                                    PRIMARY KEY (mail_no),
                                    UNIQUE KEY uk_mail_mail_id (mail_id),
                                    KEY idx_mail_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_user_state (
                                               recipient_no BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 상태 식별자',
                                               mail_id      VARCHAR(100) NOT NULL COMMENT '메일 번호',
                                               user_id      VARCHAR(7) NOT NULL COMMENT '사용자 사원번호',
                                               role         ENUM('SENDER','RECIPIENT') NOT NULL COMMENT '발신자/수신자 구분',
                                               is_read      BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
                                               is_prior     BOOLEAN NOT NULL DEFAULT FALSE COMMENT '중요메일 표시 여부',
                                               deleted_at   TIMESTAMP NULL DEFAULT NULL COMMENT '휴지통 이동 시각',
                                               purged_at    TIMESTAMP NULL DEFAULT NULL COMMENT '완전 삭제 시각',
                                               created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상태 생성 시각',
                                               updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '상태 수정 시각',

                                               PRIMARY KEY (recipient_no),
                                               UNIQUE KEY uk_mus_mail_user (mail_id, user_id),
                                               KEY idx_mus_mail_id (mail_id),
                                               KEY idx_mus_user_id (user_id),

                                               CONSTRAINT fk_mus_mail
                                                   FOREIGN KEY (mail_id) REFERENCES mail(mail_id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_attach (
                                           mail_attach_no BIGINT NOT NULL AUTO_INCREMENT COMMENT '메일 첨부파일 코드',
                                           mail_id        VARCHAR(100) NOT NULL COMMENT '메일 번호',
                                           path           VARCHAR(255) NOT NULL COMMENT '원본 저장 경로',
                                           size           BIGINT NOT NULL COMMENT '파일 크기',

                                           PRIMARY KEY (mail_attach_no),
                                           KEY idx_ma_mail_id (mail_id),

                                           CONSTRAINT fk_ma_mail
                                               FOREIGN KEY (mail_id) REFERENCES mail(mail_id)
                                                   ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================================
-- 10. 리프레쉬 토큰 (✅ revoked + index 반영)
-- ========================================================

CREATE TABLE IF NOT EXISTS refresh_token (
                                             ref_no        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '리프레시 토큰 PK',
                                             subject_type  VARCHAR(10)  NOT NULL COMMENT '로그인 주체 타입 (EMPLOYEE / COMPANY)',
                                             subject_id    BIGINT       NOT NULL COMMENT '로그인 주체 PK (EMPLOYEE.emp_no / COMPANY.com_no)',
                                             token         VARCHAR(500) NOT NULL UNIQUE COMMENT '리프레시 토큰 값',
                                             expired_at    DATETIME     NOT NULL COMMENT '만료 일시',
                                             revoked       BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '폐기 여부',
                                             created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시'
) COMMENT='JWT Refresh Token 테이블' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_subject
    ON refresh_token(subject_type, subject_id);

-- ========================================================
-- (선택) 인덱스
-- ========================================================
CREATE INDEX idx_department_com      ON department(com_id);
CREATE INDEX idx_positions_com       ON positions(com_id);
CREATE INDEX idx_employee_com        ON employee(com_id);
CREATE INDEX idx_employee_dep        ON employee(dep_no);
CREATE INDEX idx_employee_pos        ON employee(pos_no);
CREATE INDEX idx_schedule_com        ON schedule(com_id);
CREATE INDEX idx_schedule_dep        ON schedule(dep_no);
CREATE INDEX idx_schedule_reg        ON schedule(reg_emp);
CREATE INDEX idx_emp_schedule_emp    ON emp_schedule(emp_id);
CREATE INDEX idx_todo_emp            ON todo_list(emp_id);
CREATE INDEX idx_folder_com_id       ON folder(com_id);
CREATE INDEX idx_file_folder_no      ON `file`(folder_no);

-- ========================================================
-- [설정] 외래키 검사 재활성화
-- ========================================================
SET FOREIGN_KEY_CHECKS = 1;


-- =========================================================
use bizportal;

CREATE TABLE attachment (
                            attachment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 소속/참조(논리적 참조: FK는 못 걺)
                            com_id          VARCHAR(3)   NOT NULL,
                            domain          VARCHAR(20)  NOT NULL,   -- 예: MAIL, NOTICE, BOARD, APPROVAL, ...
                            entity_id       BIGINT       NOT NULL,   -- 예: mail_id / notice_id / post_id / appr_doc_id

    -- 화면 표시 순서 (1~5)
                            display_order   TINYINT UNSIGNED NOT NULL,

    -- 파일 메타
                            file_type       VARCHAR(10)  NOT NULL,   -- DOC | IMAGE | AUDIO
                            original_name   VARCHAR(255) NOT NULL,
                            content_type    VARCHAR(100) NOT NULL,
                            size            BIGINT       NOT NULL,
                            ext             VARCHAR(20)  NULL,

    -- S3 위치
                            object_key      VARCHAR(255) NOT NULL,
                            etag            VARCHAR(128)  NULL,
                            checksum_sha256 CHAR(64)      NULL,

    -- 운영/감사
                            status          VARCHAR(10) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | DELETED
                            created_by      BIGINT NULL,
                            created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at      TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    -- 제약/인덱스
                            CONSTRAINT ck_display_order_1_5 CHECK (display_order BETWEEN 1 AND 5),
                            CONSTRAINT ck_status CHECK (status IN ('ACTIVE','DELETED')),

    -- 같은 글(메일/공지/게시글/결재문서 등)에서 같은 순서는 1개만
                            UNIQUE KEY uk_attach_order (com_id, domain, entity_id, display_order),

    -- S3 key는 중복되면 안 됨(버킷 1개 기준)
                            UNIQUE KEY uk_object_key (object_key),

    -- 조회 최적화
                            INDEX idx_attach_ref (com_id, domain, entity_id),
                            INDEX idx_attach_ref_status (com_id, domain, entity_id, status)
);

use bizportal;
show databases;
DESC employee;

-- 1) 먼저 NULL 허용으로 추가
ALTER TABLE employee ADD COLUMN birth DATE NULL;

-- 2) 기존 데이터 채우기 (정책에 맞게)
UPDATE employee SET birth = '2000-01-01' WHERE birth IS NULL;

-- 3) 마지막에 NOT NULL로 변경
ALTER TABLE employee MODIFY birth DATE NOT NULL;

ALTER TABLE employee MODIFY COLUMN object_key VARCHAR(255) NULL;



-- 일정 관련 변경 --
use bizportal;
-- 1) 부서+회사 스케줄 테이블에서 dep_no nullable로 변경
ALTER TABLE schedule
    MODIFY dep_no BIGINT NULL;
-- 2) emp_schedule, schedule all_day 컬럼 추가
ALTER TABLE emp_schedule
    ADD COLUMN all_day TINYINT(1) NOT NULL DEFAULT 0 AFTER ended_at;

ALTER TABLE schedule
    ADD COLUMN all_day TINYINT(1) NOT NULL DEFAULT 0 AFTER ended_at;
drop table folder;
-- 공유함 Folder 테이블 변경(드롭하고 실행해주세요)
CREATE TABLE folder (
                        folder_no   BIGINT NOT NULL AUTO_INCREMENT,
                        com_id      VARCHAR(3)  NOT NULL,
                        dep_no      BIGINT      NULL,
                        parent_id   BIGINT      NULL,
                        folder_name VARCHAR(255) NOT NULL,
                        owner_id    VARCHAR(7)  NOT NULL,
                        scope       VARCHAR(10) NOT NULL,
                        created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at  TIMESTAMP   NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                        path        VARCHAR(255) NOT NULL DEFAULT '',

                        PRIMARY KEY (folder_no),

                        CONSTRAINT fk_folder_company
                            FOREIGN KEY (com_id) REFERENCES company(com_id),

                        CONSTRAINT fk_folder_department
                            FOREIGN KEY (dep_no) REFERENCES department(dep_no),

                        CONSTRAINT fk_folder_parent
                            FOREIGN KEY (parent_id) REFERENCES folder(folder_no),

                        CONSTRAINT fk_folder_owner
                            FOREIGN KEY (owner_id) REFERENCES employee(emp_id),

                        CONSTRAINT ck_folder_scope
                            CHECK (scope IN ('dept','prvt'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE document_form
    DROP CHECK ck_docform_stat;

ALTER TABLE document_form
    ADD CONSTRAINT ck_docform_stat
        CHECK (docfo_stat IN ('T','P','R','A','D','W','X'));

ALTER TABLE mail_user_state
    DROP COLUMN purged_at;
