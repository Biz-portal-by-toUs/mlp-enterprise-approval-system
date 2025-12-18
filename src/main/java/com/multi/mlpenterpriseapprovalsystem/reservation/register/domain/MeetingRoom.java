package com.multi.mlpenterpriseapprovalsystem.reservation.register.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ReqMeetingRoomDto;
import jakarta.persistence.*;
import lombok.*;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : MeetingRoom
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "meeting_room")
@Builder
public class MeetingRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long roomNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    private String roomName;
    private Integer cap;
    private String loc;
    private String imgUrl;
    private String equipList;
    private String note;

    public void updateInfo(ReqMeetingRoomDto dto) {
        this.roomName = dto.getRoomName();
        this.cap = dto.getCapacity();
        this.loc = dto.getLocation();
        this.equipList = dto.getEquipList();
        this.note = dto.getNote();
    }

    public void changeImageUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}