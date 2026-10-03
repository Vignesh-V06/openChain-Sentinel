package openchain_sentinel_backend.controller;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import openchain_sentinel_backend.model.Dependency;
import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.model.VulnerabilityResult;

import openchain_sentinel_backend.repository.DependencyRepository;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;

import openchain_sentinel_backend.service.AsyncScanExecutionService;
import openchain_sentinel_backend.service.ScanService;

@RestController
@RequestMapping("/api/projects/{projectId}/scans")
public class ScanController {

    private final DependencyRepository dependencyRepository;

    private final ScanService scanService;

    private final AsyncScanExecutionService
            asyncScanExecutionService;

    private final VulnerabilityResultRepository
            vulnerabilityResultRepository;

    private final MongoTemplate mongoTemplate;


    public ScanController(
            DependencyRepository dependencyRepository,
            ScanService scanService,
            AsyncScanExecutionService asyncScanExecutionService,
            VulnerabilityResultRepository vulnerabilityResultRepository,
            MongoTemplate mongoTemplate) {

        this.dependencyRepository =
                dependencyRepository;

        this.scanService =
                scanService;

        this.asyncScanExecutionService =
                asyncScanExecutionService;

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.mongoTemplate =
                mongoTemplate;
    }


    /*
     * =====================================================
     * CREATE SCAN
     * =====================================================
     */

    @PostMapping
    public Scan createScan(
            @PathVariable String projectId) {

        /*
         * Create the scan immediately.
         *
         * Initial state:
         * PENDING
         */

        Scan scan =
                scanService.createScan(
                        projectId
                );


        /*
         * Start the actual scan in
         * the background.
         */

        asyncScanExecutionService.executeScan(
                projectId,
                scan.getId()
        );


        /*
         * Return immediately.
         *
         * Frontend can now poll the
         * scan status.
         */

        return scan;
    }


    /*
     * =====================================================
     * GET ALL SCANS FOR PROJECT
     * =====================================================
     */

    @GetMapping
    public List<Scan> getScans(
            @PathVariable String projectId) {

        return scanService.getScansByProjectId(
                projectId
        );
    }


    /*
     * =====================================================
     * GET SINGLE SCAN
     * =====================================================
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
     * =====================================================
     * GET DEPENDENCIES
     * =====================================================
     */

    @GetMapping("/{scanId}/dependencies")
    public List<Dependency> getScanDependencies(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        return dependencyRepository.findByScanId(
                scanId
        );
    }


    /*
     * =====================================================
     * GET VULNERABILITIES
     * =====================================================
     */

    @GetMapping("/{scanId}/vulnerabilities")
    public List<VulnerabilityResult>
    getScanVulnerabilities(
            @PathVariable String projectId,
            @PathVariable String scanId) {

        return vulnerabilityResultRepository
                .findByScanId(scanId);
    }


    /*
     * =====================================================
     * GET RISK ASSESSMENTS
     * =====================================================
     */

    @GetMapping("/{scanId}/risk")
    public List<Document>
    getScanRiskAssessments(
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
     * =====================================================
     * GET SCAN SUMMARY
     * =====================================================
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
     * =====================================================
     * UPDATE STATUS
     * =====================================================
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