package com.eyegil.gara.catalog.service;

import com.eyegil.gara.catalog.entity.Bucket;
import com.eyegil.gara.catalog.exception.BucketRuleException;
import com.eyegil.gara.catalog.exception.BucketRuleException.Reason;
import com.eyegil.gara.catalog.repository.BucketRepository;
import com.eyegil.gara.catalog.util.BucketNames;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Buckets live in Postgres and are created and deleted at runtime; every instance sees a change immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BucketService {

    private final BucketRepository bucketRepository;
    private final VolumeService volumeService;
    private final ObjectCatalogService objectCatalogService;
    private final GithubRepoProvisioner githubRepoProvisioner;
    private final TransactionTemplate transactionTemplate;

    public boolean exists(String name) {
        return bucketRepository.existsById(name);
    }

    public Optional<Bucket> find(String name) {
        return bucketRepository.findById(name);
    }

    public List<Bucket> findAll() {
        return bucketRepository.findAllByOrderByNameAsc();
    }

    public List<String> names() {
        return findAll().stream().map(Bucket::getName).toList();
    }

    /**
     * Creates a bucket on {@code repository}: an existing repository, or a new one when {@code createRepository}
     * is set. {@code publicRepos} decides the visibility of every repository Gara creates for the bucket.
     */
    public Bucket create(String name, String repository, String directory, boolean createRepository, boolean publicRepos) {
        String folder = BucketNames.trimSlashes(directory);
        requireValid(name, repository, folder);
        if (exists(name)) {
            throw new BucketRuleException(Reason.CONFLICT, "Bucket " + name + " already exists");
        }
        if (volumeService.isRepoNameTaken(repository)) {
            throw new BucketRuleException(Reason.CONFLICT, "Repository " + repository + " already belongs to a bucket");
        }
        boolean repositoryExists = githubRepoProvisioner.exists(repository);
        if (createRepository && repositoryExists) {
            throw new BucketRuleException(Reason.CONFLICT, "Repository " + repository + " already exists on GitHub");
        }
        if (!createRepository && !repositoryExists) {
            throw new BucketRuleException(Reason.NOT_FOUND,
                    "Repository " + repository + " does not exist or the token cannot see it");
        }
        if (createRepository) {
            githubRepoProvisioner.create(repository, "Gara volume for bucket " + name, publicRepos);
        }

        Bucket bucket = transactionTemplate.execute(status -> {
            Bucket saved = bucketRepository.save(new Bucket(name, repository, folder, publicRepos));
            volumeService.register(name, repository, folder);
            return saved;
        });
        log.info("Created bucket {} on repository {}", name, repository);
        return bucket;
    }

    /**
     * Deletes an empty bucket. Its GitHub repositories are kept.
     */
    public void delete(String name) {
        Bucket bucket = find(name)
                .orElseThrow(() -> new BucketRuleException(Reason.NOT_FOUND, "Bucket " + name + " does not exist"));
        if (objectCatalogService.count(name) > 0) {
            throw new BucketRuleException(Reason.CONFLICT, "Bucket " + name + " is not empty");
        }
        bucketRepository.delete(bucket);
        log.info("Deleted bucket {}; its repositories stay on GitHub", name);
    }

    private static void requireValid(String name, String repository, String directory) {
        if (!BucketNames.isValidBucket(name)) {
            throw new BucketRuleException(Reason.INVALID,
                    "Bucket names are 3-63 lowercase letters, digits, dots or hyphens, starting and ending with a letter or digit");
        }
        if (!BucketNames.isValidRepository(repository)) {
            throw new BucketRuleException(Reason.INVALID, "Repository names are letters, digits, '.', '-' or '_'");
        }
        if (!BucketNames.isValidDirectory(directory)) {
            throw new BucketRuleException(Reason.INVALID, "The folder must be empty or folder names separated by '/'");
        }
    }
}
