package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ReqCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqMeetingRoomDto;
import jakarta.persistence.*;
import lombok.*;

/**
 * 법인 차량 엔티티
 *
 * @author : 고송현
 * @filename : CorporateCar
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "corporate_car")
@Builder
public class CorporateCar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long carNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @Column(length = 20, nullable = false)
    private String carName;

    @Column(length = 20)
    private String carType;

    @Column(length = 20, nullable = false)
    private String plateNo;

    @Column(nullable = false)
    private Integer cap;

    @Column(length = 10)
    private String fuel;

    @Column(length = 255)
    private String imgUrl;

    /**
     * 법인 차량 텍스트 정보(이미지 제외) 업데이트
     * - setter 대신 엔티티 메서드로 변경 로직을 한 곳에 모아 관리
     * - 이미지 URL은 별도 메서드(changeImageUrl)에서 갱신
     */
    public void updateInfo(ReqCorporateCarDto dto) {
        this.carName = dto.getCarName();
        this.plateNo = dto.getPlateNo();
        this.cap = dto.getCapacity();
        this.carType = dto.getCarType();
        this.fuel = dto.getFuel();
    }

    /**
     * 법인 차량 이미지 URL 갱신
     * - 파일 업로드 성공 후 생성된 접근 URL을 저장
     * - imageFile이 없으면 기존 imgUrl 유지(서비스에서 호출 여부로 제어)
     */
    public void changeImageUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}