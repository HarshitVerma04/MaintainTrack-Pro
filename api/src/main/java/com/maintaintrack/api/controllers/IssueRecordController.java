package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.IssueRecord;
import com.maintaintrack.api.services.IssueRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parts")
@CrossOrigin(origins = "*")
public class IssueRecordController {

    private final IssueRecordService service;

    public IssueRecordController(IssueRecordService service) {
        this.service = service;
    }

    @GetMapping("/issues")
    public List<IssueRecord> getAll() {
        return service.getAll();
    }

    @GetMapping("/issues/by-part/{partId}")
    public List<IssueRecord> getByPart(@PathVariable Long partId) {
        return service.getByPart(partId);
    }

    @GetMapping("/issues/by-equipment/{equipmentId}")
    public List<IssueRecord> getByEquipment(@PathVariable Long equipmentId) {
        return service.getByEquipment(equipmentId);
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issue(@RequestBody Map<String, Object> body) {
        try {
            Long partId       = Long.valueOf(body.get("partId").toString());
            Long equipmentId  = Long.valueOf(body.get("equipmentId").toString());
            int  qty          = Integer.parseInt(body.get("qty").toString());
            String issuedBy   = (String) body.getOrDefault("issuedBy", "system");
            Long breakdownId  = body.containsKey("breakdownId")
                    ? Long.valueOf(body.get("breakdownId").toString()) : null;
            Long maintenanceId = body.containsKey("maintenanceId")
                    ? Long.valueOf(body.get("maintenanceId").toString()) : null;

            IssueRecord record = service.issue(
                    partId, equipmentId, qty, issuedBy, breakdownId, maintenanceId);
            return ResponseEntity.ok(record);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnPart(@RequestBody Map<String, Object> body) {
        try {
            Long partId      = Long.valueOf(body.get("partId").toString());
            Long equipmentId = Long.valueOf(body.get("equipmentId").toString());
            int  qty         = Integer.parseInt(body.get("qty").toString());
            String issuedBy  = (String) body.getOrDefault("issuedBy", "system");

            IssueRecord record = service.returnPart(partId, equipmentId, qty, issuedBy);
            return ResponseEntity.ok(record);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}