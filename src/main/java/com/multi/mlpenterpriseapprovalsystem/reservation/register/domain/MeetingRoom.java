package com.multi.mlpenterpriseapprovalsystem.reservation.register.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Table(name = "meeting_room")
public class MeetingRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long roomNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    private String roomName;
    private Integer cap;
    private String loc;
    private String imgUrl;
    private String equipList;
    private String note;
}