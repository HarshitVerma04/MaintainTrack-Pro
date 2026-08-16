package com.maintaintrack.api.repositories;

import com.maintaintrack.api.models.IssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {
    List<IssueRecord> findByPartId(Long partId);
    List<IssueRecord> findByEquipmentId(Long equipmentId);
}