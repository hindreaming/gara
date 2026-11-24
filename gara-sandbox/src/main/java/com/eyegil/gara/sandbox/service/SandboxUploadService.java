package com.eyegil.gara.sandbox.service;

import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.catalog.service.VolumeService;
import com.eyegil.gara.coordination.exception.LeaseTimeoutException;
import com.eyegil.gara.coordination.model.Lease;
import com.eyegil.gara.coordination.service.LeaseManager;
import com.eyegil.gara.s3.service.S3ObjectService;
import com.eyegil.gara.sandbox.config.SandboxConfig;
import com.eyegil.gara.sandbox.config.SandboxProperty;
import com.eyegil.gara.sandbox.dto.SandboxQuota;
import com.eyegil.gara.sandbox.dto.SandboxUpload;
import com.eyegil.gara.sandbox.exception.SandboxException;
import com.eyegil.gara.sandbox.util.SandboxFileNames;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Stores one anonymous upload in the sandbox bucket under {@code yyyy-MM-dd/<random>-<name>}. Uploads run one at a
 * time across all instances (under a lease), so racing uploads can never overshoot the daily limits.
 */
@Service
@RequiredArgsConstructor
public class SandboxUploadService {

    private static final String LEASE = "sandbox:upload";
    private static final Duration LEASE_WAIT = Duration.ofSeconds(30);

    private final SandboxProperty sandboxProperty;
    private final SandboxQuotaService sandboxQuotaService;
    private final S3ObjectService s3ObjectService;
    private final ObjectCatalogService objectCatalogService;
    private final VolumeService volumeService;
    private final LeaseManager leaseManager;
    private final SandboxLinks sandboxLinks;

    /**
     * @param size the request's Content-Length; the body is not read when the upload is refused
     */
    public SandboxUpload upload(String fileName, String contentType, long size, InputStream body) throws IOException {
        String name = SandboxFileNames.sanitize(fileName);
        requireAcceptable(name, size);
        try (Lease ignored = leaseManager.acquire(LEASE, LEASE_WAIT).orElseThrow(() -> new LeaseTimeoutException(LEASE))) {
            SandboxQuota quota = sandboxQuotaService.quota();
            if (!quota.allows(size)) {
                throw new SandboxException(HttpStatus.TOO_MANY_REQUESTS, limitMessage(quota));
            }
            byte[] content = body.readNBytes((int) size);
            if (content.length != size) {
                throw new SandboxException(HttpStatus.BAD_REQUEST, "The upload ended before the whole file arrived.");
            }
            String key = SandboxQuotaService.dayPrefix(sandboxQuotaService.today())
                    + UUID.randomUUID().toString().substring(0, 8) + "-" + name;
            s3ObjectService.put(SandboxConfig.BUCKET, key, content, contentType);

            StoredObject object = objectCatalogService.find(SandboxConfig.BUCKET, key).orElseThrow();
            Volume volume = volumeService.get(object.getVolumeId());
            return new SandboxUpload(name, size, sandboxLinks.file(volume, key), sandboxLinks.repository(volume),
                    sandboxQuotaService.quota());
        }
    }

    private void requireAcceptable(String name, long size) {
        if (size < 0) {
            throw new SandboxException(HttpStatus.LENGTH_REQUIRED, "The upload must say how large the file is.");
        }
        if (size == 0) {
            throw new SandboxException(HttpStatus.BAD_REQUEST, "The file is empty.");
        }
        if (size > sandboxProperty.maxBytesPerDay().toBytes()) {
            throw new SandboxException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Files in the sandbox can be at most " + megabytes(sandboxProperty.maxBytesPerDay().toBytes()) + ".");
        }
        String extension = SandboxFileNames.extension(name);
        if (sandboxProperty.blockedExtensions().contains(extension)) {
            throw new SandboxException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "." + extension + " files are not accepted in the sandbox.");
        }
    }

    private static String limitMessage(SandboxQuota quota) {
        if (quota.uploadsUsed() >= quota.maxUploads()) {
            return "The sandbox has taken its " + quota.maxUploads() + " uploads for today. It opens again at midnight UTC.";
        }
        return "This file does not fit in what is left of today's " + megabytes(quota.maxBytes())
                + ". Try a smaller file, or come back after midnight UTC.";
    }

    private static String megabytes(long bytes) {
        return bytes / (1024 * 1024) + " MB";
    }
}
