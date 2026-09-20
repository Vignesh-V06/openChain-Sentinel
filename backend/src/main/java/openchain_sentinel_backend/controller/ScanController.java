package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.service.ScanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/scans")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    // CREATE SCAN
    @PostMapping
    public Scan createScan(@PathVariable String projectId) {
        return scanService.createScan(projectId);
    }

    // GET ALL SCANS FOR PROJECT
    @GetMapping
    public List<Scan> getScansByProjectId(@PathVariable String projectId) {
        return scanService.getScansByProjectId(projectId);
    }

    // GET ONE SCAN
    @GetMapping("/{scanId}")
    public Scan getScanById(@PathVariable String scanId) {
        return scanService.getScanById(scanId);
    }

    // UPDATE SCAN STATUS
    @PutMapping("/{scanId}/status")
    public Scan updateScanStatus(
            @PathVariable String scanId,
            @RequestParam String status) {

        return scanService.updateScanStatus(scanId, status);
    }
}