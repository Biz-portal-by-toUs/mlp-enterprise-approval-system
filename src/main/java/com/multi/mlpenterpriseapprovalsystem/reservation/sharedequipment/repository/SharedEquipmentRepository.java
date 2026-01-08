package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.domain.SharedEquipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : SharedEquipmentRepository
 * @since : 2025-12-21 오후 11:33 일요일
 */
public interface SharedEquipmentRepository extends JpaRepository<SharedEquipment, Long> {

    Page<SharedEquipment> findByCompany_ComId(String comId, Pageable pageable);
}
