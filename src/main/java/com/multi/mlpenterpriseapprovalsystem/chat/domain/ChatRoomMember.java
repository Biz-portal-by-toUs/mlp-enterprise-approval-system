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
    @Column(name = "romem_no") // 기존 컬럼명 유지하려면(선택)
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

    @Column(name = "unread_count", columnDefinition = "bigint default 0")
    private Long unreadCount = 0L; // 자바 객체 생성 시에도 0으로 초기화

    @Column(name="is_active", columnDefinition = "boolean default false",nullable=false)
    private boolean isActive;

    // 메시지 옆 '1' 같은 거 할 때 쓰는 필드인데, 지금은 안 써도 됨

    private String lastReadMsgId;

    public static ChatRoomMember create(ChatRoom chatRoom, Employee employee) {
        ChatRoomMember member = new ChatRoomMember();
        member.chatRoom = chatRoom;
        member.employee = employee;
        member.joinedAt = LocalDateTime.now();
        member.lastReadAt = null;
        member.unreadCount = 0L;
        member.isActive = true;

        return member;
    }

    public void markReadNow() {
        this.lastReadAt = LocalDateTime.now();
        this.unreadCount = 0L;
    }

    public void deactivateNow() {
        this.isActive = false;
        this.unreadCount = 0L;
        this.lastReadAt = LocalDateTime.now();
    }

    public void reactivateNow(LocalDateTime joinedAt) {
        this.isActive = true;
        this.joinedAt = joinedAt;     // ✅ 재입장 시점 이후 메시지만 보이게
        this.unreadCount = 0L;
        this.lastReadAt = null;
    }


}