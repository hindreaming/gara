package com.eyegil.gara.admin.dto;

import com.eyegil.gara.coordination.dto.ActiveLease;
import com.eyegil.gara.github.dto.WriteBudget;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Everything the admin dashboard shows, in one response.
 *
 * @param health                  overall status and the status of each health check
 * @param database                Postgres product and version
 * @param writeBudget             shared GitHub commit budget
 * @param pendingMultipartUploads multipart uploads started but not completed or aborted
 */
public record OverviewResponse(
        Instant generatedAt,
        String health,
        Map<String, String> healthChecks,
        String database,
        WriteBudget writeBudget,
        List<BucketView> buckets,
        List<VolumeView> volumes,
        List<ActiveLease> leases,
        long pendingMultipartUploads
) {
}
