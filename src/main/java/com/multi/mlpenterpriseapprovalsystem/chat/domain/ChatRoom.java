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
    private static final int LAST_MESSAGE_MAX_LEN = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomNo;
    @Column(name = "room_name",columnDefinition = "varchar(50)")
    private String roomName;
    @Enumerated(EnumType.STRING)
    private RoomType roomType;


    @Column(name = "last_message", columnDefinition = "varchar(50)")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY)
    private List<ChatRoomMember> members = new ArrayList<>();


    public static ChatRoom create(String roomName, RoomType roomType) {
        ChatRoom room = new ChatRoom();
        room.roomName = roomName;
        room.roomType = roomType;
        return room;
    }

    public void addMember(ChatRoomMember member) {
        members.add(member);
        member.setChatRoom(this);
    }

    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = trimLastMessage(message);
        this.lastMessageAt = time;
    }

    private String trimLastMessage(String msg) {
        if (msg == null) return null;
        String s = msg.trim();
        if (s.length() <= LAST_MESSAGE_MAX_LEN) return s;
        return s.substring(0, LAST_MESSAGE_MAX_LEN) + "...";
    }
}