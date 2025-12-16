package com.multi.mlpenterpriseapprovalsystem.document_form.form.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : DocumentForm
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 자식 테이블이 (com_id, docfo_id)로 FK를 걸기 때문에 인덱스가 필요할 수 있음
// docfo_id 자체가 유니크하므로 uniqueConstraints는 docfo_id에만 걸림
@Table(name = "document_form")
public class DocumentForm extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docfoNo;

    // [수정] 전역 유니크 설정 (unique = true)
    // [중요] name = "docfo_id"를 명시해야 다른 엔티티에서 referencedColumnName="docfo_id"로 찾을 수 있습니다.
    @Column(name = "docfo_id", nullable = false, length = 6, unique = true)
    private String docfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    private String writerId; // 작성자 사원번호 (단순 매핑)

    private String docfoName;

    @Column(columnDefinition = "json")
    private String cnttJson;

    @Lob
    private String cnttHtml;

    @Column(length = 1)
    private String docfoStat;

    private String rejectReason;
}