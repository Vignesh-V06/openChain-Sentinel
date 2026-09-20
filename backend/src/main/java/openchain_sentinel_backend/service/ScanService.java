package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.Scan;
import openchain_sentinel_backend.repository.ScanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScanService {

    private final ScanRepository scanRepository;

    public ScanService(ScanRepository scanRepository) {
        this.scanRepository = scanRepository;
    }

    // CREATE SCAN
    public Scan createScan(String projectId) {
        Scan scan = new Scan(projectId);
        return scanRepository.save(scan);
    }

    // GET ALL SCANS FOR A PROJECT
    public List<Scan> getScansByProjectId(String projectId) {
        return scanRepository.findByProjectId(projectId);
    }

    // GET ONE SCAN
    public Scan getScanById(String id) {
        return scanRepository.findById(id).orElse(null);
    }

    // UPDATE SCAN STATUS
    public Scan updateScanStatus(String id, String status) {

        Scan scan = scanRepository.findById(id).orElse(null);

        if (scan == null) {
            return null;
        }

        scan.setStatus(status);

        if ("RUNNING".equals(status)) {
            scan.setStartedAt(LocalDateTime.now());
        }

        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            scan.setCompletedAt(LocalDateTime.now());
        }

        return scanRepository.save(scan);
    }
}