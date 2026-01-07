package com.multi.mlpenterpriseapprovalsystem.mail.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Maiil
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mail")
public class Mail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mail_no")
    private Long mailNo;

    @Column(name = "mail_id", nullable = false, unique = true, length = 100)
    private String mailId; // UK

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "json", nullable = false)
    private String cntt;

    @Column(nullable = true)
    private LocalDateTime savedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "emp_id", nullable = false)
    private Employee sender;

    // 1:N 관계 매핑
    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL)
    private List<MailUserState> userStates = new ArrayList<>();

    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL)
    private List<MailAttach> attachments = new ArrayList<>();

    public static Mail create(String mailId, String title, String cntt, Employee sender) {
        Mail m = new Mail();
        m.mailId = mailId;
        m.title = title;
        m.cntt = cntt;
        m.sender = sender;
        return m;
    }

    public boolean isDraft() {
        return this.savedAt != null;
    }

    // 임시저장 생성
    public static Mail createDraft(String mailId, String title, String cntt, Employee sender) {
        Mail m = new Mail();
        m.mailId = mailId;
        m.title = title;
        m.cntt = cntt;
        m.sender = sender;
        m.savedAt = LocalDateTime.now();
        return m;
    }

    // 임시저장 업데이트(자동저장/수동저장 공용)
    public void updateDraft(String title, String cntt) {
        this.title = title;
        this.cntt = cntt;
        this.savedAt = LocalDateTime.now();
    }

    // 발송 처리(초안 해제)
    public void clearDraft() {
        this.savedAt = null;
    }
}