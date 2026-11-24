package com.eyegil.gara.admin.service;

import com.eyegil.gara.admin.converter.VolumeViewConverter;
import com.eyegil.gara.admin.dto.BucketView;
import com.eyegil.gara.admin.dto.OverviewResponse;
import com.eyegil.gara.admin.dto.VolumeView;
import com.eyegil.gara.catalog.lock.VolumeLeases;
import com.eyegil.gara.catalog.service.MultipartUploadService;
import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.catalog.service.VolumeService;
import com.eyegil.gara.coordination.dto.ActiveLease;
import com.eyegil.gara.coordination.service.LeaseManager;
import com.eyegil.gara.github.service.GithubWriteThrottle;
import com.eyegil.gara.catalog.service.BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverviewService {

    private final HealthEndpoint healthEndpoint;
    private final JdbcTemplate jdbcTemplate;
    private final BucketService bucketService;
    private final VolumeService volumeService;
    private final ObjectCatalogService objectCatalogService;
    private final MultipartUploadService multipartUploadService;
    private final LeaseManager leaseManager;
    private final GithubWriteThrottle githubWriteThrottle;
    private final VolumeViewConverter volumeViewConverter;

    public OverviewResponse overview() {
        HealthComponent health = healthEndpoint.health();
        List<ActiveLease> leases = leaseManager.active();
        Set<String> heldLeases = leases.stream().map(ActiveLease::name).collect(Collectors.toSet());

        List<VolumeView> volumes = volumeService.findAllOrdered().stream()
                .map(volume -> volumeViewConverter.toView(volume, heldLeases.contains(VolumeLeases.leaseName(volume))))
                .toList();
        List<BucketView> buckets = bucketService.findAll().stream()
                .map(bucket -> new BucketView(bucket.getName(), bucket.getRepository(), bucket.getDirectory(),
                        bucket.isPublicRepos(),
                        objectCatalogService.count(bucket.getName()), objectCatalogService.totalSize(bucket.getName()),
                        volumeService.countActive(bucket.getName())))
                .toList();

        return new OverviewResponse(
                Instant.now(),
                health.getStatus().getCode(),
                healthChecks(health),
                jdbcTemplate.queryForObject("SELECT version()", String.class),
                githubWriteThrottle.budget(),
                buckets,
                volumes,
                leases,
                multipartUploadService.count()
        );
    }

    private static Map<String, String> healthChecks(HealthComponent health) {
        Map<String, String> checks = new LinkedHashMap<>();
        if (health instanceof CompositeHealth composite) {
            composite.getComponents().forEach((name, component) -> checks.put(name, component.getStatus().getCode()));
        }
        return checks;
    }
}
