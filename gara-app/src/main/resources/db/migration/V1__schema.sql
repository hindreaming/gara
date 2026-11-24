-- Gara schema. Postgres is the source of truth for which repository holds each object key.

-- Coordination between Gara instances.

-- A held lease on a named resource (a repository, an object key, a bucket's provisioning).
-- A lease whose expires_at has passed is free again, so a crashed instance cannot hold it forever.
CREATE TABLE gara_lease
(
    name       VARCHAR(1200) PRIMARY KEY,
    owner_id   VARCHAR(255)             NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Token buckets shared by all instances, e.g. the GitHub commit budget of the account.
CREATE TABLE gara_rate_budget
(
    name        VARCHAR(255) PRIMARY KEY,
    tokens      DOUBLE PRECISION         NOT NULL,
    refilled_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Catalog: the repositories (volumes) behind each bucket, and which one holds each object key.
-- An object is one file at its key's path inside its volume. Files changed on GitHub outside Gara are not picked up.

-- Buckets, created and deleted from the admin UI. Each starts on one repository (its first volume).
CREATE TABLE gara_bucket
(
    name       VARCHAR(63) PRIMARY KEY,
    repository VARCHAR(255)             NOT NULL,
    directory  VARCHAR(255)             NOT NULL,
    -- Visibility of the repositories Gara creates for this bucket
    public_repos BOOLEAN                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE gara_volume
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bucket_id     VARCHAR(63)              NOT NULL REFERENCES gara_bucket (name) ON DELETE CASCADE,
    repo_name     VARCHAR(255)             NOT NULL,
    directory     VARCHAR(255)             NOT NULL,
    status        VARCHAR(16)              NOT NULL,
    -- Bytes ever committed. Never decreases: deleted and replaced files stay in git history.
    written_bytes BIGINT                   NOT NULL DEFAULT 0,
    -- Files currently at the branch head (what a recursive git tree has to list).
    head_files    BIGINT                   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_gara_volume_repo UNIQUE (bucket_id, repo_name)
);

CREATE INDEX ix_gara_volume_bucket_status ON gara_volume (bucket_id, status);

-- One row per object key: the repository that holds it, plus the metadata S3 clients read,
-- so HEAD and LIST never call GitHub.
CREATE TABLE gara_object
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bucket_id    VARCHAR(63)              NOT NULL,
    -- "C" collation orders keys by bytes, as S3 lists them, independent of the database locale.
    object_key   VARCHAR(1024) COLLATE "C" NOT NULL,
    volume_id    BIGINT                   NOT NULL REFERENCES gara_volume (id),
    size         BIGINT                   NOT NULL,
    etag         VARCHAR(64)              NOT NULL,
    content_type VARCHAR(255)             NOT NULL,
    -- Git blob sha of the file, needed to overwrite or delete it without asking GitHub first.
    git_sha      VARCHAR(40)              NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_gara_object UNIQUE (bucket_id, object_key)
);

CREATE INDEX ix_gara_object_volume ON gara_object (volume_id);

-- Multipart uploads in progress. Parts wait here (shared by all instances) until the upload completes,
-- then they are joined and pushed to GitHub as one file and these rows are removed.
CREATE TABLE gara_multipart_upload
(
    id           VARCHAR(36) PRIMARY KEY,
    bucket_id    VARCHAR(63)              NOT NULL REFERENCES gara_bucket (name) ON DELETE CASCADE,
    object_key   VARCHAR(1024)            NOT NULL,
    content_type VARCHAR(255)             NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE gara_upload_part
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    upload_id   VARCHAR(36) NOT NULL REFERENCES gara_multipart_upload (id) ON DELETE CASCADE,
    part_number INTEGER     NOT NULL,
    content     BYTEA       NOT NULL,
    size        BIGINT      NOT NULL,
    etag        VARCHAR(32) NOT NULL,
    CONSTRAINT uk_gara_upload_part UNIQUE (upload_id, part_number)
);
