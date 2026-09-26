package openchain_sentinel_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import openchain_sentinel_backend.model.MavenAnalysisResult;
import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.model.VulnerabilityResult;
import openchain_sentinel_backend.repository.ProjectRepository;
import openchain_sentinel_backend.repository.VulnerabilityResultRepository;

@Service
public class ScanOrchestrationService {

    private final ProjectRepository projectRepository;

    private final ScanService scanService;

    private final MavenAnalyzerService
            mavenAnalyzerService;

    private final DependencyStorageService
            dependencyStorageService;

    private final Neo4jGraphService
            neo4jGraphService;

    private final OsvVulnerabilityStorageService
            osvVulnerabilityStorageService;

    private final VulnerabilityResultRepository
            vulnerabilityResultRepository;

    private final Neo4jVulnerabilityService
            neo4jVulnerabilityService;

    private final RiskAssessmentService
            riskAssessmentService;

    private final ScanRiskSummaryService
            scanRiskSummaryService;

    public ScanOrchestrationService(
            ProjectRepository projectRepository,
            ScanService scanService,
            MavenAnalyzerService
                    mavenAnalyzerService,
            DependencyStorageService
                    dependencyStorageService,
            Neo4jGraphService neo4jGraphService,
            OsvVulnerabilityStorageService
                    osvVulnerabilityStorageService,
            VulnerabilityResultRepository
                    vulnerabilityResultRepository,
            Neo4jVulnerabilityService
                    neo4jVulnerabilityService,
            RiskAssessmentService
                    riskAssessmentService,
            ScanRiskSummaryService
                    scanRiskSummaryService) {

        this.projectRepository =
                projectRepository;

        this.scanService =
                scanService;

        this.mavenAnalyzerService =
                mavenAnalyzerService;

        this.dependencyStorageService =
                dependencyStorageService;

        this.neo4jGraphService =
                neo4jGraphService;

        this.osvVulnerabilityStorageService =
                osvVulnerabilityStorageService;

        this.vulnerabilityResultRepository =
                vulnerabilityResultRepository;

        this.neo4jVulnerabilityService =
                neo4jVulnerabilityService;

        this.riskAssessmentService =
                riskAssessmentService;

        this.scanRiskSummaryService =
                scanRiskSummaryService;
    }

    public Scan executeScan(
            String projectId,
            String scanId) {

        try {

            /*
             * --------------------------------------------------
             * 1. Get project
             * --------------------------------------------------
             */

            Project project =
                    projectRepository
                            .findById(projectId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Project not found: "
                                                    + projectId
                                    )
                            );

            /*
             * --------------------------------------------------
             * 2. Mark scan RUNNING
             * --------------------------------------------------
             */

            scanService.updateScanStatus(
                    scanId,
                    "RUNNING"
            );

            System.out.println(
                    "===== SCAN START ====="
            );

            System.out.println(
                    "Project: " + projectId
            );

            System.out.println(
                    "Scan: " + scanId
            );

            /*
             * --------------------------------------------------
             * 3. Maven analysis
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 1: Maven dependency analysis"
            );

            MavenAnalysisResult analysisResult =
                    mavenAnalyzerService.analyze(
                            project.getRepositoryUrl()
                    );

            System.out.println(
                    "Dependencies resolved: "
                            + analysisResult
                                    .getDependencies()
                                    .size()
            );

            /*
             * --------------------------------------------------
             * 4. Mongo dependency storage
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 2: Saving dependencies to MongoDB"
            );

            dependencyStorageService.saveDependencies(
                    projectId,
                    scanId,
                    analysisResult
            );

            /*
             * --------------------------------------------------
             * 5. Neo4j dependency graph
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 3: Creating Neo4j dependency graph"
            );

            neo4jGraphService.createDependencyGraph(
                    projectId,
                    project.getName(),
                    scanId,
                    analysisResult
            );

            /*
             * --------------------------------------------------
             * 6. OSV vulnerability scan
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 4: Running OSV vulnerability scan"
            );

            List<VulnerabilityResult>
                    storedVulnerabilities =
                            osvVulnerabilityStorageService
                                    .scanAndStore(
                                            projectId,
                                            scanId,
                                            analysisResult
                                                    .getDependencies()
                                    );

            System.out.println(
                    "OSV vulnerability records stored: "
                            + storedVulnerabilities.size()
            );

            /*
             * --------------------------------------------------
             * 7. Read vulnerabilities from MongoDB
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 5: Reading vulnerabilities from MongoDB"
            );

            List<VulnerabilityResult>
                    vulnerabilities =
                            vulnerabilityResultRepository
                                    .findByScanId(
                                            scanId
                                    );

            System.out.println(
                    "Mongo vulnerability count: "
                            + vulnerabilities.size()
            );

            /*
             * --------------------------------------------------
             * 8. Link vulnerabilities to Neo4j
             * --------------------------------------------------
             */

            if (!vulnerabilities.isEmpty()) {

                System.out.println(
                        "STEP 6: Linking vulnerabilities to Neo4j"
                );

                neo4jVulnerabilityService
                        .linkVulnerabilities(
                                projectId,
                                scanId,
                                vulnerabilities
                        );

            } else {

                System.out.println(
                        "STEP 6: No vulnerabilities to link"
                );
            }

            /*
             * --------------------------------------------------
             * 9. Risk assessment
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 7: Calculating risk assessments"
            );

            riskAssessmentService.assessScan(
                    scanId
            );

            /*
             * --------------------------------------------------
             * 10. Scan-level summary
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 8: Generating scan risk summary"
            );

            scanRiskSummaryService.generateSummary(
                    scanId,
                    projectId
            );

            /*
             * --------------------------------------------------
             * 11. Completed
             * --------------------------------------------------
             */

            System.out.println(
                    "STEP 9: Marking scan COMPLETED"
            );

            Scan completedScan =
                    scanService.updateScanStatus(
                            scanId,
                            "COMPLETED"
                    );

            System.out.println(
                    "===== SCAN COMPLETED ====="
            );

            return completedScan;

        } catch (Exception e) {

            System.err.println(
                    "===== SCAN FAILED ====="
            );

            e.printStackTrace();

            scanService.updateScanStatus(
                    scanId,
                    "FAILED"
            );

            throw new RuntimeException(
                    "Scan failed for scan "
                            + scanId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }
}