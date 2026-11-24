package com.eyegil.gara.sandbox.bootstrap;

import com.eyegil.gara.catalog.exception.BucketRuleException;
import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import com.eyegil.gara.sandbox.config.SandboxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Makes sure the sandbox bucket exists. Postgres is asked first; GitHub only when the bucket is missing, to reuse
 * the repository if it is already there or to create it as a public repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxBucketBootstrap implements ApplicationRunner {

    private static final String REPOSITORY = SandboxConfig.BUCKET;

    private final BucketService bucketService;
    private final GithubRepoProvisioner githubRepoProvisioner;

    @Override
    public void run(ApplicationArguments args) {
        if (bucketService.exists(SandboxConfig.BUCKET)) {
            return;
        }
        boolean createRepository = !githubRepoProvisioner.exists(REPOSITORY);
        try {
            bucketService.create(SandboxConfig.BUCKET, REPOSITORY, "", createRepository, true);
            log.info("Created the sandbox bucket on {} repository {}", createRepository ? "new public" : "existing",
                    REPOSITORY);
        } catch (BucketRuleException e) {
            // Another instance starting at the same time may have created it first
            if (!bucketService.exists(SandboxConfig.BUCKET)) {
                throw e;
            }
        }
    }
}
