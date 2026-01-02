package com.multi.mlpenterpriseapprovalsystem.cloud.domain;

import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScopeConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클라우드(공유함 / 개인함)에서 사용하는 폴더 엔티티
 *
 * @author : 김승기
 * @filename : Folder
 * @since : 2025. 12. 16. 화요일
 */

@Entity
@Table(name = "folder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_no")
    private Long folderNo;

    // FK: company.com_id
    @Column(name = "com_id", nullable = false, length = 3)
    private String comId;

    // FK: department.dep_no (dept 폴더일 때만 사용 가능 -> null 허용)
    @Column(name = "dep_no")
    private Long depNo;

    // FK: folder.folder_no (root면 null)
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "folder_name", nullable = false, length = 255)
    private String folderName;

    // FK: employee.emp_id
    @Column(name = "owner_id", nullable = false, length = 7)
    private String ownerId;

    // CK: dept / prvt
    @Column(name = "scope", nullable = false, length = 10)
    @Convert(converter = FolderScopeConverter.class)
    private FolderScope scope;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "path", nullable = false, length = 255)
    private String path;

    // ---------- lifecycle ----------
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.path == null) this.path = ""; // NOT NULL 방지 (저장 후 updatePath로 세팅)
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- factory ----------
    public static Folder create(String comId,
                                Long depNo,
                                Long parentId,
                                String folderName,
                                String ownerId,
                                FolderScope scope) {
        Folder f = new Folder();
        f.comId = comId;
        f.depNo = depNo;         // dept면 depNo, prvt면 null로 넣는 건 service에서 결정
        f.parentId = parentId;
        f.folderName = folderName;
        f.ownerId = ownerId;
        f.scope = scope;
        f.path = "";
        return f;
    }

    // ---------- behavior ----------
    public void rename(String newName) {
        this.folderName = newName;
    }

    public void updatePath(String path) {
        this.path = path;
    }
}