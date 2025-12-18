package com.multi.mlpenterpriseapprovalsystem.chat.domain;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 엔티티
 *
 * @author : 김승기
 * @filename : ChatRoomMember
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room_members")
public class ChatRoomMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long romemNo;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_no")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "empId")
    private Employee employee;

    private LocalDateTime joinedAt;
    private String lastReadMsgId;

    
    

    public static ChatRoomMember create(ChatRoom chatRoom, Employee employee) {
        ChatRoomMember member = new ChatRoomMember();
        member.chatRoom = chatRoom;
        member.employee = employee;
        member.joinedAt = LocalDateTime.now();
        return member;
    }


}