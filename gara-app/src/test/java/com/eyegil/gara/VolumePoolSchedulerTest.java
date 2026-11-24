package com.eyegil.gara;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.service.VolumeProvisioningService;
import com.eyegil.gara.catalog.service.VolumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The pool scheduler's check: keep min-active repositories with room to spare, one new repository per run,
 * never more than max-active.
 */
@TestPropertySource(properties = {
        "gara.volume.min-active=2",
        "gara.volume.max-active=3",
        "gara.volume.max-size=1000B",
        "gara.volume.fill-threshold=1.0",
        "gara.volume.nearly-full=0.8",
        "gara.volume.pool-check-interval=PT1H"
})
class VolumePoolSchedulerTest extends GaraIntegrationTest {

    @Autowired
    private VolumeProvisioningService volumeProvisioningService;

    @Autowired
    private VolumeService volumeService;

    @Test
    void createsOneRepositoryPerRunUntilMinActiveHaveRoom() {
        volumeProvisioningService.topUp(BUCKET);
        boolean createdAgain = volumeProvisioningService.topUp(BUCKET);

        assertThat(activeRepos()).containsExactlyInAnyOrder("archive", "archive-2");
        assertThat(createdAgain).isFalse();
    }

    @Test
    void replacesANearlyFullRepositoryAheadOfTimeButStaysWithinMaxActive() {
        volumeProvisioningService.topUp(BUCKET);
        nearlyFill("archive");

        assertThat(volumeProvisioningService.topUp(BUCKET)).isTrue();
        nearlyFill("archive-2");
        assertThat(volumeProvisioningService.topUp(BUCKET)).isFalse();

        assertThat(activeRepos()).containsExactlyInAnyOrder("archive", "archive-2", "archive-3");
        verify(githubRepoProvisioner, times(2)).create(anyString(), anyString(), anyBoolean());
    }

    private void nearlyFill(String repoName) {
        jdbcTemplate.update("UPDATE gara_volume SET written_bytes = 850 WHERE repo_name = ?", repoName);
    }

    private List<String> activeRepos() {
        return volumeService.findActive(BUCKET).stream().map(Volume::getRepoName).toList();
    }
}
