package com.multi.mlpenterpriseapprovalsystem.cloud.repository;

import com.multi.mlpenterpriseapprovalsystem.cloud.domain.Folder;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 폴더 엔티티(Folder)에 대한 JPA Repository
 *
 * @author : 송현님
 * @filename : FolderRepository
 * @since : 2025-12-29 오전 10:24 월요일
 */

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {


    Optional<Folder> findByFolderNoAndComId(Long folderNo, String comId);

    @Query("""
        select f from Folder f
        where f.comId = :comId
          and ( (:parentId is null and f.parentId is null) or (f.parentId = :parentId) )
          and f.scope = :scope
          and f.depNo = :depNo
        order by f.folderName asc
    """)
    List<Folder> findDeptChildren(@Param("comId") String comId,
                                  @Param("parentId") Long parentId,
                                  @Param("depNo") Long depNo,
                                  @Param("scope") FolderScope scope);

    @Query("""
        select f from Folder f
        where f.comId = :comId
          and ( (:parentId is null and f.parentId is null) or (f.parentId = :parentId) )
          and f.scope = :scope
          and f.ownerId = :ownerId
        order by f.folderName asc
    """)
    List<Folder> findPrvtChildren(@Param("comId") String comId,
                                  @Param("parentId") Long parentId,
                                  @Param("ownerId") String ownerId,
                                  @Param("scope") FolderScope scope);
}