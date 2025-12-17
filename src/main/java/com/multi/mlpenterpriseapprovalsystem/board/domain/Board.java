package com.multi.mlpenterpriseapprovalsystem.board.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Table(name = "board")
public class Board extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long boardNo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    private Boolean isDeleted;
    private String title;
    @Column(columnDefinition = "json") private String contents;
    private String catCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", referencedColumnName = "emp_id") private Employee writer;
    private Integer rating;
}