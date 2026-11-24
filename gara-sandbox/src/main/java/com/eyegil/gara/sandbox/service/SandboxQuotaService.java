package com.eyegil.gara.sandbox.service;

import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.sandbox.config.SandboxConfig;
import com.eyegil.gara.sandbox.config.SandboxProperty;
import com.eyegil.gara.sandbox.dto.SandboxQuota;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Today's usage is read from the catalog: every sandbox upload is stored under a {@code yyyy-MM-dd/} prefix of the
 * UTC day it arrived, so no separate counter is kept.
 */
@Service
@RequiredArgsConstructor
public class SandboxQuotaService {

    private final SandboxProperty sandboxProperty;
    private final ObjectCatalogService objectCatalogService;

    public LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public SandboxQuota quota() {
        LocalDate day = today();
        String prefix = dayPrefix(day);
        return new SandboxQuota(
                (int) objectCatalogService.count(SandboxConfig.BUCKET, prefix),
                sandboxProperty.maxUploadsPerDay(),
                objectCatalogService.totalSize(SandboxConfig.BUCKET, prefix),
                sandboxProperty.maxBytesPerDay().toBytes(),
                day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static String dayPrefix(LocalDate day) {
        return day + "/";
    }
}
