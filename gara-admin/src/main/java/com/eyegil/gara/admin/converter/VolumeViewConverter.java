package com.eyegil.gara.admin.converter;

import com.eyegil.gara.admin.dto.VolumeView;
import com.eyegil.gara.catalog.config.VolumeProperty;
import com.eyegil.gara.catalog.entity.Volume;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VolumeViewConverter {

    private final VolumeProperty volumeProperty;

    public VolumeView toView(Volume volume, boolean busy) {
        double fillPercent = 100.0 * volume.getWrittenBytes() / Math.max(1, volumeProperty.fillLimitBytes());
        return new VolumeView(
                volume.getId(),
                volume.getBucketId(),
                volume.getRepoName(),
                volume.getDirectory(),
                volume.getStatus().name(),
                volume.getWrittenBytes(),
                volume.getHeadFiles(),
                Math.round(fillPercent * 10) / 10.0,
                busy
        );
    }
}
