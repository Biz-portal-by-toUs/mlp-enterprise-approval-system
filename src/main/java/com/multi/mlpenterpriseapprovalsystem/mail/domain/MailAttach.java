package com.multi.mlpenterpriseapprovalsystem.mail.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메일 첨부파일 엔티티
 *
 * @author : 김승기
 * @filename : MailAttach
 * @since : 2025. 12. 16. 화요일
 */

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mail_attach")
public class MailAttach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mail_attach_no")
    private Long mailAttachNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_id", referencedColumnName = "mail_id", nullable = false)
    private Mail mail;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Long size;
}
