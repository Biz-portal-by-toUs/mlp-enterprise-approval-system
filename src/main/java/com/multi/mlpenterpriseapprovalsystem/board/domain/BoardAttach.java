package com.multi.mlpenterpriseapprovalsystem.board.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : BoardAttach
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_attach")
public class BoardAttach {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long boardAttachNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "board_no") private Board board;
    private String orgName;
    private String folderPath;
}