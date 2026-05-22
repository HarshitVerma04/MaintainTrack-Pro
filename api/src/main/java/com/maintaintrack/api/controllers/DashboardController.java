package com.maintaintrack.api.controllers;

import com.maintaintrack.api.dto.DashboardKpiDto;
import com.maintaintrack.api.services.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    public DashboardKpiDto getKpis() {
        return service.getKpis();
    }
}