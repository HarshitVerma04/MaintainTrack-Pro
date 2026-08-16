package com.maintaintrack.api.services;

import com.maintaintrack.api.models.*;
import com.maintaintrack.api.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.maintaintrack.api.config.SecurityUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class IssueRecordService {

    private final IssueRecordRepository issueRepo;
    private final PartRepository        partRepo;
    private final EquipmentRepository   equipRepo;
    private final BreakdownLogRepository breakdownRepo;
    private final MaintenanceLogRepository maintenanceRepo;

    public IssueRecordService(IssueRecordRepository issueRepo,
                              PartRepository partRepo,
                              EquipmentRepository equipRepo,
                              BreakdownLogRepository breakdownRepo,
                              MaintenanceLogRepository maintenanceRepo) {
        this.issueRepo       = issueRepo;
        this.partRepo        = partRepo;
        this.equipRepo       = equipRepo;
        this.breakdownRepo   = breakdownRepo;
        this.maintenanceRepo = maintenanceRepo;
    }

    public List<IssueRecord> getAll() {
        return issueRepo.findAll();
    }

    public List<IssueRecord> getByPart(Long partId) {
        return issueRepo.findByPartId(partId);
    }

    public List<IssueRecord> getByEquipment(Long equipmentId) {
        return issueRepo.findByEquipmentId(equipmentId);
    }

    @Transactional
    public IssueRecord issue(Long partId, Long equipmentId,
                             int qty,
                             Long breakdownId, Long maintenanceId) {

        Part part = partRepo.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found: " + partId));

        if (part.getQtyOnHand() < qty) {
            throw new RuntimeException("Insufficient stock. Available: " + part.getQtyOnHand());
        }

        Equipment equipment = equipRepo.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        // Atomically deduct stock
        part.setQtyOnHand(part.getQtyOnHand() - qty);
        part.setSynced(true);
        partRepo.save(part);

        // Build the record
        IssueRecord record = new IssueRecord();
        record.setPart(part);
        record.setEquipment(equipment);
        record.setQty(qty);
        record.setIssuedBy(SecurityUtils.getCurrentUsername());
        record.setIssuedOn(LocalDate.now().toString());
        record.setType("issue");
        record.setSynced(true);

        if (breakdownId != null) {
            breakdownRepo.findById(breakdownId).ifPresent(record::setBreakdown);
        }
        if (maintenanceId != null) {
            maintenanceRepo.findById(maintenanceId).ifPresent(record::setMaintenance);
        }

        return issueRepo.save(record);
    }

    @Transactional
    public IssueRecord returnPart(Long partId, Long equipmentId,
                                  int qty) {

        Part part = partRepo.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found: " + partId));

        Equipment equipment = equipRepo.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        // Atomically add stock back
        part.setQtyOnHand(part.getQtyOnHand() + qty);
        part.setSynced(true);
        partRepo.save(part);

        IssueRecord record = new IssueRecord();
        record.setPart(part);
        record.setEquipment(equipment);
        record.setQty(qty);
        record.setIssuedBy(SecurityUtils.getCurrentUsername());
        record.setIssuedOn(LocalDate.now().toString());
        record.setType("return");
        record.setSynced(true);

        return issueRepo.save(record);
    }
}