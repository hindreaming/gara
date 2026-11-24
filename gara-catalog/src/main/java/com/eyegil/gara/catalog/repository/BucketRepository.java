package com.eyegil.gara.catalog.repository;

import com.eyegil.gara.catalog.entity.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BucketRepository extends JpaRepository<Bucket, String> {

    List<Bucket> findAllByOrderByNameAsc();
}
