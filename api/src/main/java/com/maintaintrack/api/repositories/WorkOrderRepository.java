package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(String status);
    List<WorkOrder> findByEquipmentId(Long equipmentId);
    long countByStatus(String status);
    Optional<WorkOrder> findBySyncId(String syncId);
}