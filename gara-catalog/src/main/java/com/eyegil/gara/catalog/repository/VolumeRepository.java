package com.eyegil.gara.catalog.repository;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.entity.VolumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VolumeRepository extends JpaRepository<Volume, Long> {

    List<Volume> findAllByBucketIdAndStatus(String bucketId, VolumeStatus status);

    Optional<Volume> findFirstByBucketIdOrderByIdAsc(String bucketId);

    List<Volume> findAllByStatus(VolumeStatus status);

    List<Volume> findAllByOrderByBucketIdAscIdAsc();

    long countByBucketId(String bucketId);

    long countByBucketIdAndStatus(String bucketId, VolumeStatus status);

    boolean existsByRepoName(String repoName);

    @Modifying(clearAutomatically = true)
    @Query("update Volume v set v.writtenBytes = v.writtenBytes + :bytes, v.headFiles = v.headFiles + :newFiles where v.id = :id")
    void addWritten(@Param("id") Long id, @Param("bytes") long bytes, @Param("newFiles") long newFiles);

    @Modifying(clearAutomatically = true)
    @Query("update Volume v set v.headFiles = v.headFiles - 1 where v.id = :id and v.headFiles > 0")
    void removeHeadFile(@Param("id") Long id);
}
