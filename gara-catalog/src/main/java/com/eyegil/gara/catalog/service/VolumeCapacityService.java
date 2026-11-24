package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.config.VolumeProperty;
import com.eyegil.gara.catalog.entity.Volume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeCapacityService {

    private final VolumeProperty volumeProperty;
    private final VolumeService volumeService;

    /**
     * Takes {@code volume} out of the write pool once it reaches its size or file-count limit. Its replacement is
     * normally created ahead of time by the pool scheduler.
     */
    public void retireIfFull(Volume volume) {
        if (!isFull(volume)) {
            return;
        }
        volumeService.markFull(volume);
        log.info("Volume {} of bucket {} is full and no longer takes writes", volume.getRepoName(), volume.getBucketId());
    }

    /**
     * Full by bytes ever written (history included) or by files at the branch head.
     */
    private boolean isFull(Volume volume) {
        Volume current = volumeService.get(volume.getId());
        return current.getWrittenBytes() >= volumeProperty.fillLimitBytes()
                || current.getHeadFiles() >= volumeProperty.maxFiles();
    }
}
