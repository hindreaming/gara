package com.eyegil.gara.sandbox.dto;

import java.time.Instant;

/**
 * Today's sandbox allowance, shared by every visitor.
 */
public record SandboxQuota(int uploadsUsed, int maxUploads, long bytesUsed, long maxBytes, Instant resetsAt) {

    public boolean allows(long size) {
        return uploadsUsed < maxUploads && bytesUsed + size <= maxBytes;
    }
}
