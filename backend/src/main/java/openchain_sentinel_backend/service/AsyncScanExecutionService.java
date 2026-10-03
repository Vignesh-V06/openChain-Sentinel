package openchain_sentinel_backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncScanExecutionService {

    private final ScanOrchestrationService scanOrchestrationService;

    public AsyncScanExecutionService(
            ScanOrchestrationService scanOrchestrationService) {

        this.scanOrchestrationService =
                scanOrchestrationService;
    }

    @Async
    public void executeScan(
            String projectId,
            String scanId) {

        scanOrchestrationService.executeScan(
                projectId,
                scanId
        );
    }
}