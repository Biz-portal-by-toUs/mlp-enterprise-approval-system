package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ReqSharedEquipmentDto;
import jakarta.persistence.*;
import lombok.*;

/**
 * 공유 설비 엔티티
 *
 * @author : 고송현
 * @filename : SharedEquipment
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "shared_equipment")
@Builder
public class SharedEquipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long eqNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @Column(length = 20, nullable = false)
    private String eqName;

    @Column(length = 20)
    private String eqId;

    @Column(length = 20)
    private String modelName;

    @Column(length = 255)
    private String imgUrl;

    @Column(length = 20)
    private String loc;

    /**
     * 공유 설비 텍스트 정보(이미지 제외) 업데이트
     * - setter 대신 엔티티 메서드로 변경 로직을 한 곳에 모아 관리
     * - 이미지 URL은 별도 메서드(changeImageUrl)에서 갱신
     */
    public void updateInfo(ReqSharedEquipmentDto dto) {
        this.eqName = dto.getEqName();
        this.eqId = dto.getEqId();
        this.modelName = dto.getModelName();
        this.loc = dto.getLocation();
    }

    /**
     * 공유 설비 이미지 URL 갱신
     * - 파일 업로드 성공 후 생성된 접근 URL을 저장
     * - imageFile이 없으면 기존 imgUrl 유지(서비스에서 호출 여부로 제어)
     */
    public void changeImageUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}