package com.multi.mlpenterpriseapprovalsystem.cloud.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : DriveFile
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file") // DB Table Name
public class DriveFile { // Java Class Name
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_no")
    private Folder folder;

    private String fileName;
    private Long size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "empId")
    private Employee uploader;

    private String path;

    @CreatedDate
    private LocalDateTime createdAt;
}