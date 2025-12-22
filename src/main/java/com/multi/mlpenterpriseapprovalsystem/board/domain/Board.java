package com.multi.mlpenterpriseapprovalsystem.board.domain;

import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardReqDto;
import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Board
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 빌더 사용하려면 전체 생성자 필요
@Builder
@Table(name = "board")
public class Board extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardNo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;
    private Boolean isDeleted;
    private String title;
    @Column(columnDefinition = "json")
    private String contents;
    private Character catCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;
    private Integer rating;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void update(@Valid BoardReqDto dto) {

        this.isDeleted = dto.getIsDeleted();
        this.title = dto.getTitle();
        this.contents = dto.getContents();
        this.catCode = dto.getCatCode();

    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setBoard(this);
    }

    public void removeComment(Comment comment) {

        // (삭제 트리거) 부모의 컬렉션에서 제거 -> orphanRemoval=true인 경우 flush 때 DB에서 삭제
        comments.remove(comment);

        // (양쪽 동기화) 연관관계의 주인(자식) 쪽 참조도 끊어 일관성 유지
        comment.setBoard(null);
    }


    public void assignCompany(Company company) {
        this.company = company;
    }

    public void assignEmployee(Employee employee) {
        this.employee = employee;
    }

}