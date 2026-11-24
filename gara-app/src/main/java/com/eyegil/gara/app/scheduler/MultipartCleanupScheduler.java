package com.eyegil.gara.app.scheduler;

import com.eyegil.gara.s3.service.S3MultipartService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipartCleanupScheduler {

    private final S3MultipartService s3MultipartService;

    @Scheduled(cron = "${gara.object.multipart-cleanup-cron:0 30 3 * * *}")
    public void abortExpiredUploads() {
        s3MultipartService.abortExpired();
    }
}
