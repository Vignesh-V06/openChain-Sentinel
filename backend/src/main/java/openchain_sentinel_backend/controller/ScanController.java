package openchain_sentinel_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.service.ScanOrchestrationService;
import openchain_sentinel_backend.service.ScanService;

@RestController
@RequestMapping("/api/projects/{projectId}/scans")
public class ScanController {

    private final ScanService scanService;

    private final ScanOrchestrationService
            scanOrchestrationService;

    public ScanController(
            ScanService scanService,
            ScanOrchestrationService
                    scanOrchestrationService) {

        this.scanService = scanService;

        this.scanOrchestrationService =
                scanOrchestrationService;
    }

    /*
     * --------------------------------------------------
     * Create and execute a scan
     * --------------------------------------------------
     */

    @PostMapping
    public Scan createScan(
            @PathVariable String projectId) {

        Scan scan =
                scanService.createScan(
                        projectId
                );

        return scanOrchestrationService.executeScan(
                projectId,
                scan.getId()
        );
    }

    /*
     * --------------------------------------------------
     * Get all scans for a project
     * --------------------------------------------------
     */

    @GetMapping
    public List<Scan> getScans(
            @PathVariable String projectId) {

        return scanService.getScansByProjectId(
                projectId
        );
    }

    /*
     * --------------------------------------------------
     * Get one scan
     * --------------------------------------------------
     */

    @GetMapping("/{scanId}")
    public Scan getScan(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        return scanService.getScanById(
                scanId
        );
    }

    /*
     * --------------------------------------------------
     * Manually update status
     * --------------------------------------------------
     *
     * Kept because it is already part of our
     * development/testing API.
     */

    @PutMapping("/{scanId}/status")
    public Scan updateScanStatus(
            @PathVariable String projectId,
            @PathVariable String scanId,
            @RequestParam String status) {

        return scanService.updateScanStatus(
                scanId,
                status
        );
    }
}