package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByStatus(String status);
    Optional<Equipment> findBySyncId(String syncId);   
}