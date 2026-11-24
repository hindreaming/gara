package com.eyegil.gara.catalog.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeNamesTest {

    @Test
    void placesFilesInsideTheVolumeDirectory() {
        assertThat(VolumeNames.repoPath("assets", "images/logo.png")).isEqualTo("assets/images/logo.png");
        assertThat(VolumeNames.repoPath("", "images/logo.png")).isEqualTo("images/logo.png");
    }

    @Test
    void numbersVolumesFromTheFirstRepositoryName() {
        assertThat(VolumeNames.numbered("assets-2024", 1)).isEqualTo("assets-2024");
        assertThat(VolumeNames.numbered("assets-2024", 3)).isEqualTo("assets-2024-3");
    }
}
