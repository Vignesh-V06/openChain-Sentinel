package openchain_sentinel_backend.controller;

import java.util.List;

import org.bson.Document;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import openchain_sentinel_backend.model.Dependency;
import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.model.User;
import openchain_sentinel_backend.model.VulnerabilityResult;

import openchain_sentinel_backend.repository.DependencyRepository;
import openchain_sentinel_backend.repository.UserRepository;
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

    private final UserRepository userRepository;


    public ScanController(
            DependencyRepository dependencyRepository,
            ScanService scanService,
            AsyncScanExecutionService asyncScanExecutionService,
            VulnerabilityResultRepository vulnerabilityResultRepository,
            MongoTemplate mongoTemplate,
            UserRepository userRepository) {

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

        this.userRepository =
                userRepository;
    }


    /*
     * =====================================================
     * CREATE SCAN
     * =====================================================
     */

    @PostMapping
    public Scan createScan(
            @PathVariable String projectId,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        /*
         * The service verifies that the project belongs
         * to the authenticated user.
         */
        Scan scan =
                scanService.createScan(
                        projectId,
                        userId
                );

        if (scan == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            );
        }


        /*
         * Existing async pipeline remains unchanged.
         */

        asyncScanExecutionService.executeScan(
                projectId,
                scan.getId()
        );


        /*
         * Return immediately with PENDING.
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
            @PathVariable String projectId,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        List<Scan> scans =
                scanService.getScansByProjectId(
                        projectId,
                        userId
                );

        /*
         * Empty can mean either:
         *
         * - project has no scans
         * - project doesn't belong to user
         *
         * To avoid exposing ownership information,
         * verify the project separately.
         */

        if (!scanService.projectExistsForUser(
                projectId,
                userId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            );
        }

        return scans;
    }


    /*
     * =====================================================
     * GET SINGLE SCAN
     * =====================================================
     */

    @GetMapping("/{scanId}")
    public Scan getScan(
            @PathVariable String projectId,
            @PathVariable String scanId,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        Scan scan =
                scanService.getScanById(
                        projectId,
                        scanId,
                        userId
                );

        if (scan == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scan not found"
            );
        }

        return scan;
    }


    /*
     * =====================================================
     * GET DEPENDENCIES
     * =====================================================
     */

    @GetMapping("/{scanId}/dependencies")
    public List<Dependency> getScanDependencies(
            @PathVariable String projectId,
            @PathVariable String scanId,
            Authentication authentication) {

        verifyScanOwnership(
                projectId,
                scanId,
                authentication
        );

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
            @PathVariable String scanId,
            Authentication authentication) {

        verifyScanOwnership(
                projectId,
                scanId,
                authentication
        );

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
            @PathVariable String scanId,
            Authentication authentication) {

        verifyScanOwnership(
                projectId,
                scanId,
                authentication
        );

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
            @PathVariable String scanId,
            Authentication authentication) {

        verifyScanOwnership(
                projectId,
                scanId,
                authentication
        );

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
     *
     * The ownership check is kept here because this endpoint
     * is exposed over HTTP.
     *
     * AsyncScanExecutionService still calls the service
     * directly and does not need authentication.
     */

    @PutMapping("/{scanId}/status")
    public Scan updateScanStatus(
            @PathVariable String projectId,
            @PathVariable String scanId,
            @RequestParam String status,
            Authentication authentication) {

        verifyScanOwnership(
                projectId,
                scanId,
                authentication
        );

        Scan scan =
                scanService.updateScanStatus(
                        scanId,
                        status
                );

        if (scan == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scan not found"
            );
        }

        return scan;
    }


    /*
     * =====================================================
     * VERIFY SCAN OWNERSHIP
     * =====================================================
     */

    private void verifyScanOwnership(
            String projectId,
            String scanId,
            Authentication authentication) {

        String userId =
                getCurrentUserId(authentication);

        boolean ownsScan =
                scanService.userOwnsScan(
                        projectId,
                        scanId,
                        userId
                );

        if (!ownsScan) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scan not found"
            );
        }
    }


    /*
     * =====================================================
     * GET CURRENT USER ID
     * =====================================================
     */

    private String getCurrentUserId(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        String username =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(username)
                        .orElse(null);

        if (user == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user not found"
            );
        }

        return user.getId();
    }
}