package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.repository.ProjectRepository;
import openchain_sentinel_backend.repository.ScanRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScanService {

    private final ScanRepository scanRepository;

    private final ProjectRepository projectRepository;

    public ScanService(
            ScanRepository scanRepository,
            ProjectRepository projectRepository) {

        this.scanRepository = scanRepository;

        this.projectRepository = projectRepository;
    }


    /*
     * =====================================================
     * CREATE SCAN - USER SCOPED
     * =====================================================
     */

    public Scan createScan(
            String projectId,
            String userId) {

        /*
         * First verify:
         *
         * Does this project belong to the current user?
         */
        boolean ownsProject =
                projectRepository
                        .existsByIdAndUserId(
                                projectId,
                                userId
                        );

        if (!ownsProject) {
            return null;
        }

        Scan scan =
                new Scan(projectId);

        return scanRepository.save(scan);
    }


    /*
     * =====================================================
     * GET ALL SCANS - USER SCOPED
     * =====================================================
     */

    public List<Scan> getScansByProjectId(
            String projectId,
            String userId) {

        /*
         * Do not query scans until ownership
         * of the parent project is verified.
         */
        boolean ownsProject =
                projectRepository
                        .existsByIdAndUserId(
                                projectId,
                                userId
                        );

        if (!ownsProject) {
            return List.of();
        }

        return scanRepository.findByProjectId(
                projectId
        );
    }


    /*
     * =====================================================
     * GET ONE SCAN - USER SCOPED
     * =====================================================
     */

    public Scan getScanById(
            String projectId,
            String scanId,
            String userId) {

        /*
         * Step 1:
         * Verify project ownership.
         */
        boolean ownsProject =
                projectRepository
                        .existsByIdAndUserId(
                                projectId,
                                userId
                        );

        if (!ownsProject) {
            return null;
        }

        /*
         * Step 2:
         * Verify scan belongs to that project.
         */
        return scanRepository
                .findByIdAndProjectId(
                        scanId,
                        projectId
                )
                .orElse(null);
    }


    /*
     * =====================================================
     * CHECK SCAN OWNERSHIP
     * =====================================================
     *
     * This is useful for dependency,
     * vulnerability and risk endpoints.
     */

    public boolean userOwnsScan(
            String projectId,
            String scanId,
            String userId) {

        boolean ownsProject =
                projectRepository
                        .existsByIdAndUserId(
                                projectId,
                                userId
                        );

        if (!ownsProject) {
            return false;
        }

        return scanRepository
                .findByIdAndProjectId(
                        scanId,
                        projectId
                )
                .isPresent();
    }


    /*
     * =====================================================
     * UPDATE SCAN STATUS
     * =====================================================
     *
     * IMPORTANT:
     *
     * This method intentionally remains user-independent.
     *
     * AsyncScanExecutionService uses it internally while
     * a scan is running in the background.
     */

    public Scan updateScanStatus(
            String id,
            String status) {

        Scan scan =
                scanRepository
                        .findById(id)
                        .orElse(null);

        if (scan == null) {
            return null;
        }

        scan.setStatus(status);

        if ("RUNNING".equals(status)) {

            scan.setStartedAt(
                    LocalDateTime.now()
            );
        }

        if ("COMPLETED".equals(status)
                || "FAILED".equals(status)) {

            scan.setCompletedAt(
                    LocalDateTime.now()
            );
        }

        return scanRepository.save(scan);
    }


    /*
     * =====================================================
     * EXISTING INTERNAL METHODS
     * =====================================================
     *
     * Retained so existing backend services don't break.
     */

    public Scan createScan(String projectId) {

        Scan scan =
                new Scan(projectId);

        return scanRepository.save(scan);
    }

    public List<Scan> getScansByProjectId(
            String projectId) {

        return scanRepository.findByProjectId(
                projectId
        );
    }

    public Scan getScanById(String id) {

        return scanRepository
                .findById(id)
                .orElse(null);
    }
    public boolean projectExistsForUser(
        String projectId,
        String userId) {

    return projectRepository.existsByIdAndUserId(
            projectId,
            userId
    );
}
}