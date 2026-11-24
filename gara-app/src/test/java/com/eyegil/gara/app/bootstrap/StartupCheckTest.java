package com.eyegil.gara.app.bootstrap;

import com.eyegil.gara.catalog.entity.Bucket;
import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import com.eyegil.integration.github.client.config.GithubProperties;
import com.eyegil.integration.github.client.dto.GithubUser;
import com.eyegil.integration.github.client.service.GithubUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartupCheckTest {

    private final GithubUserService users = mock(GithubUserService.class);
    private final GithubRepoProvisioner repos = mock(GithubRepoProvisioner.class);
    private final GithubProperties clientProperties = new GithubProperties();
    private final BucketService buckets = mock(BucketService.class);
    private final StartupCheck check = new StartupCheck(
            new GithubProperty("owner", "main", 450, 20, Duration.ofSeconds(60), 3, Duration.ofSeconds(2), true),
            clientProperties, users, repos, buckets);

    @BeforeEach
    void oneBucket() {
        when(buckets.findAll()).thenReturn(List.of(new Bucket("assets", "archive", "assets", false)));
    }

    @Test
    void refusesToStartWithoutAToken() {
        assertThatThrownBy(() -> check.run(null)).hasMessageContaining("GITHUB_TOKEN");
    }

    @Test
    void refusesToStartWhenTheFirstRepositoryIsMissing() {
        clientProperties.setToken("token");
        when(users.getAuthenticatedUser()).thenReturn(new GithubUser(1, "owner", null, null, null));

        assertThatThrownBy(() -> check.run(null)).hasMessageContaining("owner/archive");
    }

    @Test
    void startsWhenGithubIsUsable() {
        clientProperties.setToken("token");
        when(users.getAuthenticatedUser()).thenReturn(new GithubUser(1, "owner", null, null, null));
        when(repos.exists("archive")).thenReturn(true);

        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }
}
