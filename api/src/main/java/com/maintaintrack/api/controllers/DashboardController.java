package com.maintaintrack.api.controllers;

import com.maintaintrack.api.dto.DashboardKpiDto;
import com.maintaintrack.api.services.DashboardService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "Aggregated KPIs. Response is cached for 60 seconds.")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Get dashboard KPIs",
            description = "Returns total equipment, overdue count, low stock count, " +
                    "open work orders, and a recent activity feed. Cached for 60s.")
    @GetMapping("/kpis")
    public DashboardKpiDto getKpis() {
        return service.getKpis();
    }
}