package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.BreakdownLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BreakdownLogRepository extends JpaRepository<BreakdownLog, Long> {
    List<BreakdownLog> findByEquipmentIdOrderByOccurredOnDesc(Long equipmentId);
}