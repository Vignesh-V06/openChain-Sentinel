package openchain_sentinel_backend.controller;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;
import openchain_sentinel_backend.service.ScanOrchestrationService;
import openchain_sentinel_backend.service.ScanService;

@RestController
@RequestMapping("/api/projects/{projectId}/scans")
public class ScanController {

    private final ScanService scanService;

    private final ScanOrchestrationService
            scanOrchestrationService;

    private final VulnerabilityResultRepository
            vulnerabilityResultRepository;

    private final MongoTemplate mongoTemplate;

    public ScanController(
            ScanService scanService,
            ScanOrchestrationService scanOrchestrationService,
            VulnerabilityResultRepository vulnerabilityResultRepository,
            MongoTemplate mongoTemplate) {

        this.scanService = scanService;

        this.scanOrchestrationService =
                scanOrchestrationService;

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.mongoTemplate = mongoTemplate;
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
     * Get vulnerabilities for a scan
     * --------------------------------------------------
     */

    @GetMapping("/{scanId}/vulnerabilities")
    public List<VulnerabilityResult> getScanVulnerabilities(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        return vulnerabilityResultRepository
                .findByScanId(scanId);
    }

    /*
     * --------------------------------------------------
     * Get risk assessments for a scan
     * --------------------------------------------------
     */

    @GetMapping("/{scanId}/risk")
    public List<Document> getScanRiskAssessments(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        Query query =
                Query.query(
                        Criteria.where("scanId")
                                .is(scanId)
                );

        return mongoTemplate.find(
                query,
                Document.class,
                "risk_assessments"
        );
    }

    /*
     * --------------------------------------------------
     * Get scan-level risk summary
     * --------------------------------------------------
     */

    @GetMapping("/{scanId}/summary")
    public Document getScanRiskSummary(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        Query query =
                Query.query(
                        Criteria.where("scanId")
                                .is(scanId)
                );

        return mongoTemplate.findOne(
                query,
                Document.class,
                "scan_risk_summaries"
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