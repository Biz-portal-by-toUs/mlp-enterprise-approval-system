package com.multi.mlpenterpriseapprovalsystem.board.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Comment
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@Table(name = "comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 빌더 사용하려면 전체 생성자 필요
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentNo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_no")
    private Board board;
    @Lob
    private String contents;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;
    @CreatedDate
    private LocalDateTime createdAt;

    public void setBoard(Board board) {
        this.board = board;
        // 양방향 무한루프 방지 및 이미 리스트에 있으면 중복 추가 방지
        if (board != null && !board.getComments().contains(this)) {
            board.getComments().add(this);//그 상품(Product)의 리뷰 목록에도 현재 리뷰(this)를 추가
        }
    }

    public void update(String commentContent) {
        this.contents = commentContent;
    }
}