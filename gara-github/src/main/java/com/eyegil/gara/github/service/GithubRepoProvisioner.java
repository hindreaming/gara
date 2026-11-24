package com.eyegil.gara.github.service;

import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.integration.github.client.config.error.GithubApiException;
import com.eyegil.integration.github.client.dto.CreateRepositoryRequest;
import com.eyegil.integration.github.client.service.GithubRepositoryService;
import com.eyegil.integration.github.client.service.GithubUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Creates the GitHub repositories that back new volumes, under the configured owner
 * (the token's own account or an organization).
 */
@Service
@RequiredArgsConstructor
public class GithubRepoProvisioner {

    private static final int NOT_FOUND = 404;

    private final GithubRepositoryService githubRepositoryService;
    private final GithubUserService githubUserService;
    private final GithubProperty githubProperty;

    public boolean exists(String repoName) {
        try {
            githubRepositoryService.getRepository(githubProperty.owner(), repoName);
            return true;
        } catch (GithubApiException e) {
            if (e.getStatusCode() != null && e.getStatusCode().value() == NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Creates {@code repoName}, initialised with a README so the branch exists before the first file is committed.
     */
    public void create(String repoName, String description, boolean publicRepo) {
        String owner = githubProperty.owner();
        CreateRepositoryRequest request = CreateRepositoryRequest.builder()
                .name(repoName)
                .description(description)
                .isPrivate(!publicRepo)
                .autoInit(true)
                .build();

        if (owner.equalsIgnoreCase(githubUserService.getAuthenticatedUser().login())) {
            githubRepositoryService.createRepository(request);
        } else {
            githubRepositoryService.createOrganizationRepository(owner, request);
        }
    }
}
