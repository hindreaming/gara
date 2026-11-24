package com.eyegil.gara.catalog.repository;

import com.eyegil.gara.catalog.entity.StoredObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoredObjectRepository extends JpaRepository<StoredObject, Long> {

    Optional<StoredObject> findByBucketIdAndObjectKey(String bucketId, String objectKey);

    List<StoredObject> findByBucketIdAndObjectKeyStartingWithAndObjectKeyGreaterThanOrderByObjectKeyAsc(
            String bucketId, String prefix, String startAfter, Pageable page);

    long countByBucketId(String bucketId);

    @Query("select coalesce(sum(o.size), 0) from StoredObject o where o.bucketId = :bucketId")
    long sumSizeByBucketId(@Param("bucketId") String bucketId);

    long countByBucketIdAndObjectKeyStartingWith(String bucketId, String prefix);

    @Query("select coalesce(sum(o.size), 0) from StoredObject o where o.bucketId = :bucketId and o.objectKey like concat(:prefix, '%')")
    long sumSizeByBucketIdAndPrefix(@Param("bucketId") String bucketId, @Param("prefix") String prefix);
}
