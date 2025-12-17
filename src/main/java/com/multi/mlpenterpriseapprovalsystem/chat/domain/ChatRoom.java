package com.multi.mlpenterpriseapprovalsystem.chat.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 *
 * @author : 김승기
 * @filename : ChatRoom
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomNo;
    private String roomName;
    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    // 🔥 추가
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY)
    private List<ChatRoomMember> members = new ArrayList<>();

    public void updateLastMessage(String message) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();
    }

    public static ChatRoom create(String roomName, RoomType roomType) {
        ChatRoom room = new ChatRoom();
        room.roomName = roomName;
        room.roomType = roomType;
        return room;
    }

    // 🔥 핵심
    public void addMember(ChatRoomMember member) {
        members.add(member);
        member.setChatRoom(this);
    }

    // 메시지 도착 시 호출
    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = message;
        this.lastMessageAt = time;
    }
}