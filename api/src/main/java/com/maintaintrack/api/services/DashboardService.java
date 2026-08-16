package com.maintaintrack.api.services;

import com.maintaintrack.api.dto.DashboardKpiDto;
import com.maintaintrack.api.models.BreakdownLog;
import com.maintaintrack.api.models.MaintenanceLog;
import com.maintaintrack.api.repositories.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final EquipmentRepository      equipRepo;
    private final PartRepository           partRepo;
    private final WorkOrderRepository      workOrderRepo;
    private final MaintenanceLogRepository maintenanceRepo;
    private final BreakdownLogRepository   breakdownRepo;

    public DashboardService(EquipmentRepository equipRepo,
                            PartRepository partRepo,
                            WorkOrderRepository workOrderRepo,
                            MaintenanceLogRepository maintenanceRepo,
                            BreakdownLogRepository breakdownRepo) {
        this.equipRepo       = equipRepo;
        this.partRepo        = partRepo;
        this.workOrderRepo   = workOrderRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.breakdownRepo   = breakdownRepo;
    }

    @Cacheable("dashboard")
    public DashboardKpiDto getKpis() {
        DashboardKpiDto dto = new DashboardKpiDto();
        String today = LocalDate.now().toString();

        dto.setTotalEquipment(equipRepo.count());

        long overdue = equipRepo.findAll().stream()
                .filter(e -> e.getNextMaintenanceDate() != null
                        && !e.getNextMaintenanceDate().isEmpty()
                        && e.getNextMaintenanceDate().compareTo(today) < 0
                        && "Operational".equals(e.getStatus()))
                .count();
        dto.setOverdueCount(overdue);

        long underMaintenance = equipRepo.findByStatus("Under Maintenance").size();
        dto.setUnderMaintenanceCount(underMaintenance);

        dto.setLowStockCount(partRepo.findLowStock().size());

        dto.setOpenWorkOrders(workOrderRepo.countByStatus("Open"));
        dto.setInProgressWorkOrders(workOrderRepo.countByStatus("In Progress"));

        List<Map<String, Object>> activity = new ArrayList<>();

        maintenanceRepo.findAll().stream()
                .sorted(Comparator.comparing(MaintenanceLog::getDoneOn).reversed())
                .limit(10)
                .forEach(m -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("type", "maintenance");
                    entry.put("date", m.getDoneOn());
                    entry.put("description", "Maintenance on " +
                            m.getEquipment().getName() +
                            (m.getNotes() != null ? ": " + m.getNotes() : ""));
                    entry.put("by", m.getDoneBy());
                    activity.add(entry);
                });

        breakdownRepo.findAll().stream()
                .sorted(Comparator.comparing(BreakdownLog::getOccurredOn).reversed())
                .limit(10)
                .forEach(b -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("type", "breakdown");
                    entry.put("date", b.getOccurredOn());
                    entry.put("description", "Breakdown on " +
                            b.getEquipment().getName() +
                            (b.getDescription() != null ? ": " + b.getDescription() : ""));
                    entry.put("by", b.getResolvedBy());
                    activity.add(entry);
                });

        activity.sort((a, b) -> b.get("date").toString()
                .compareTo(a.get("date").toString()));

        dto.setRecentActivity(activity.stream().limit(20)
                .collect(Collectors.toList()));

        return dto;
    }
}