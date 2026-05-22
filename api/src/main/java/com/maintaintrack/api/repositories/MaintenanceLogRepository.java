package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {
    List<MaintenanceLog> findByEquipmentIdOrderByDoneOnDesc(Long equipmentId);
}