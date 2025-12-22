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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "romem_no")
    private Long romemNo;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_no")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;

    @Column(name="joined_at")
    private LocalDateTime joinedAt;

    @Column(name="last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "unread_count", columnDefinition = "int default 0 ")
    private int unreadCount = 0; // 자바 객체 생성 시에도 0으로 초기화

    @Column(name="is_active", columnDefinition = "boolean default false",nullable=false)
    private boolean isActive;


    public static ChatRoomMember create(ChatRoom chatRoom, Employee employee) {
        ChatRoomMember member = new ChatRoomMember();
        member.chatRoom = chatRoom;
        member.employee = employee;
        member.joinedAt = LocalDateTime.now();
        member.lastReadAt = null;
        member.unreadCount = 0;
        member.isActive = true;

        return member;
    }

    public void markReadNow() {
        this.lastReadAt = LocalDateTime.now();
        this.unreadCount = 0;
    }

    public void deactivateNow() {
        this.isActive = false;
        this.unreadCount = 0;
        this.lastReadAt = LocalDateTime.now();
    }

    public void reactivateNow(LocalDateTime joinedAt) {
        this.isActive = true;
        this.joinedAt = joinedAt;
        this.unreadCount = 0;
        this.lastReadAt = null;
    }


}