package com.eyegil.gara.app.bootstrap;

import com.eyegil.gara.catalog.entity.Bucket;
import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import com.eyegil.integration.github.client.config.GithubProperties;
import com.eyegil.integration.github.client.service.GithubUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start when GitHub is not usable: no token, a token GitHub rejects, or a bucket whose first repository
 * the token cannot see.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupCheck implements ApplicationRunner {

    private final GithubProperty githubProperty;
    private final GithubProperties githubClientProperties;
    private final GithubUserService githubUserService;
    private final GithubRepoProvisioner githubRepoProvisioner;
    private final BucketService bucketService;

    @Override
    public void run(ApplicationArguments args) {
        if (!githubProperty.verifyOnStartup()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        if (!StringUtils.hasText(githubClientProperties.getToken())) {
            problems.add("github.token (GITHUB_TOKEN) is not set");
        } else {
            checkGithub(problems);
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Gara cannot start: " + String.join("; ", problems));
        }
    }

    private void checkGithub(List<String> problems) {
        try {
            log.info("GitHub token belongs to {}", githubUserService.getAuthenticatedUser().login());
        } catch (RuntimeException e) {
            problems.add("GitHub rejected the token: " + e.getMessage());
            return;
        }
        for (Bucket bucket : bucketService.findAll()) {
            if (!githubRepoProvisioner.exists(bucket.getRepository())) {
                problems.add("repository " + githubProperty.owner() + "/" + bucket.getRepository() + " of bucket "
                        + bucket.getName() + " does not exist or the token cannot see it");
            }
        }
    }
}
