# Gara: Git Asset Repository Archive

> [!WARNING]
> **Gara is just a fun project**, made for fun, learning and experimenting. It is not a production-grade storage service.
>
> - **Use it responsibly.** Gara keeps your files in GitHub repositories. Excessive bulk uploads or large-scale file
>   hosting can break [GitHub's Terms of Service](https://docs.github.com/en/site-policy/github-terms/github-terms-of-service)
>   and get your account rate-limited or suspended.
> - **Keep it to modest, personal, non-commercial use.**
> - **No guarantees.** It comes with no warranty (see [LICENSE](LICENSE)), so keep your own backups.

Gara is an S3-compatible object store that keeps its files in GitHub repositories.
S3 clients (AWS CLI, AWS SDKs, anything that speaks S3 with path-style URLs) upload and download as usual.
Gara spreads the files over a pool of GitHub repositories and creates new ones as they fill up.

## How it works

- **Buckets** are created and deleted in the admin UI and stored in Postgres. Each bucket starts on one repository (existing, or created for it) and an optional folder inside it. Only empty buckets can be deleted; their repositories stay on GitHub.
- **Volumes**: every GitHub repository behind a bucket is a volume. Uploads go to an idle *active* volume. A pool scheduler (every `gara.volume.pool-check-interval`, Postgres only) keeps at least `gara.volume.min-active` active volumes with room to spare, creating at most one repository per bucket per run and never more than `gara.volume.max-active`; a volume past `gara.volume.nearly-full` no longer counts as having room, so its replacement exists before it fills. A volume that reaches its size or file limit becomes *full* and takes no new keys. Uploads only create a repository themselves when every active volume is busy or none is left.
- **Objects**: each key is one file at the key's path in one repository. Replacing a key overwrites that file in the repository that already holds it, even if that repository is full, so a key never has two copies. Deleting removes the file from the branch head.
- **Size accounting follows git**: a repository's *written bytes* include history, so deletes and replaces never lower it. Only the count of files at the head goes down.
- **Postgres is the source of truth**: the catalog decides which keys exist and where. Gara never reads its catalog back from GitHub: files added or deleted on GitHub outside Gara are invisible to S3 clients, and a file edited on GitHub is served with its new content but the old size and ETag (a later write through Gara still succeeds and fixes it). Back up Postgres: restoring it is the recovery plan.
- **Catalog**: Postgres keeps one row per key: the repository, size, ETag, content type and modified time. GET, HEAD and LIST therefore never search GitHub, and only GET downloads the file.
- **Multipart uploads**: parts wait in Postgres, so they work whichever instance receives them. On completion they are joined and pushed as one file, so a multipart object is still limited to `gara.object.max-size` (100MB). Unfinished uploads are dropped after `gara.object.multipart-expiry`.
- **Concurrency across instances** goes through Postgres:
  - leases (with expiry and renewal) serialize commits per repository and writes per key
  - a shared token bucket keeps all instances under GitHub's commit rate limits
  - a crashed instance's leases expire after `gara.lease.ttl`

## Modules

| Module | Responsibility |
| --- | --- |
| `gara-security` | Account check and AWS Signature V4 verification (no web dependencies) |
| `gara-coordination` | Postgres leases and shared rate budgets |
| `gara-github` | All GitHub access through github-client: file commits with retries, reads up to 100MB, repository creation, commit throttle |
| `gara-catalog` | Volumes, object records, object files (written under the repository's lease), pending multipart parts, volume pool and capacity rules |
| `gara-s3` | S3 HTTP API, signature filter, aws-chunked decoding |
| `gara-admin` | Admin JSON API (overview, object browser), health check, metrics, HTTP Basic |
| `gara-ui` | Static admin console (HTML, CSS, JS only) |
| `gara-sandbox` | Gara plus a public try-it page with daily limits; built on gara-app, which knows nothing about it |
| `gara-app` | Spring Boot application, startup, schedulers, configuration |

Dependencies point one way: `sandbox → app → admin/s3 → catalog → github → coordination`, with `security` used by `s3` and `admin`.

## Running locally

1. **Postgres 17.** Use either of these:
   - An existing server. Create a `gara` database and user, then point `GARA_DB_*` at it.
   - Docker:
     ```powershell
     $env:GARA_DB_PORT = "5433"; docker compose up -d
     ```
2. **Environment**

   | Variable | Meaning | Default |
   | --- | --- | --- |
   | `GARA_USERNAME`, `GARA_PASSWORD` | Gara account: S3 access key and secret, admin UI login | required |
   | `GITHUB_TOKEN` | Token that can create repositories and push to them (fine-grained: Administration, Contents and Metadata read/write; classic: `repo`) | required |
   | `GITHUB_OWNER` | User or organization that owns the repositories | required |
   | `GITHUB_BRANCH` | Branch to write to | `main` |
   | `GARA_DB_PASSWORD` | Postgres password | required |
   | `GARA_DB_HOST`, `GARA_DB_PORT`, `GARA_DB_NAME`, `GARA_DB_USER` | Postgres | `localhost`, `5432`, `gara`, `gara` |

   These can live in a `.env` file (`KEY=value`, no quotes) in the folder Gara runs from: `gara-app/.env` for `./mvnw -pl gara-app spring-boot:run`, `gara-sandbox/.env` for the sandbox, or next to the jar. For Docker Compose: `docker compose --env-file gara-app/.env up -d`.

3. **Buckets.** After starting, open the admin UI and create a bucket: a name, its first repository (an existing one with a README so the branch exists, or tick "create"), and a folder. At startup Gara checks that the token works and that each bucket's first repository exists, and refuses to start otherwise (`gara.github.verify-on-startup=false` turns this off).

4. **Start** one of the two:
   - **Gara** (S3 API and admin console only):
     ```powershell
     ./mvnw -pl gara-app -am spring-boot:run
     ```
     or `./mvnw package` and `java -jar gara-app/target/gara-exec.jar`. (`gara.jar` next to it is the plain library jar that gara-sandbox builds on.)
   - **Gara with the public sandbox page**:
     ```powershell
     ./mvnw -pl gara-sandbox -am spring-boot:run
     ```
     or `./mvnw package` and `java -jar gara-sandbox/target/gara-sandbox.jar`.

Flyway creates the schema on first start.

## Using it

**AWS CLI** (path-style addressing):

```powershell
aws configure set default.s3.addressing_style path
```
```powershell
aws --endpoint-url http://localhost:8080 s3 cp .\logo.png s3://assets/images/logo.png
```

Use `GARA_USERNAME` as the access key id and `GARA_PASSWORD` as the secret access key. Any region works.

**Supported S3 operations**: ListBuckets, HeadBucket, PutObject, CopyObject, GetObject (with Range), HeadObject, DeleteObject, DeleteObjects, ListObjects (V1 and V2, with delimiter, prefix, pagination and `encoding-type=url`), CreateMultipartUpload, UploadPart, CompleteMultipartUpload and AbortMultipartUpload. Signed headers and presigned URLs both work.

**Not supported**: versioning, ACLs and bucket policies, CORS, lifecycle rules, object tags and `x-amz-meta-*` user metadata, creating or deleting buckets over the S3 API (use the admin console), and virtual-hosted bucket URLs.

**Admin console**: `http://localhost:8080/admin` (HTTP Basic, same account), styled after the MinIO console. *Object Browser* walks buckets and folders (upload, download, delete; read from the catalog, so browsing costs no GitHub calls). *Buckets* creates and deletes buckets, with private or public repositories. *Volumes* shows each repository's fill level, and *Monitoring* shows health, the GitHub commit budget and leases.

**Sandbox**: `http://localhost:8080/sandbox` is a public page (no login, only when you run gara-sandbox) where anyone can drag and drop files to try Gara. Each upload goes to the `gara-sandbox` bucket and its public `gara-sandbox` repository, which Gara creates at startup if Postgres has no such bucket. The visitor gets a public link to the file and to the repository. Limits are shared by all visitors and reset at midnight UTC:

| Setting | Default | |
| --- | --- | --- |
| `gara.sandbox.max-uploads-per-day` | 10 | Files accepted per day, from everyone together |
| `gara.sandbox.max-bytes-per-day` | 20MB | Bytes accepted per day, from everyone together |
| `gara.sandbox.blocked-extensions` | exe, msi, dll, scr, com, bat, cmd, ps1, vbs, sh, jar, apk | Refused outright |

Anything uploaded there is public and permanent in git history, so run plain Gara for a private deployment. To remove a file, delete it in the admin console, then rewrite the repository's history (or delete the repository) on GitHub.

**Ops endpoints**
- `GET /actuator/health` is public, for load balancers.
- `/actuator/metrics` requires Basic auth.

## Running in production

- **Put Gara behind HTTPS** (a reverse proxy or load balancer). Signatures protect the S3 secret, but over plain HTTP the files themselves and the admin password (HTTP Basic) travel unencrypted.
- **Client addresses behind a proxy**: set `server.forward-headers-strategy=native` (or `framework`) so the login lockout sees the real client address instead of the proxy's.
- **Login lockout**: after `gara.auth.lockout.max-failures` (10) wrong passwords or signatures within `gara.auth.lockout.duration` (15 minutes), the address is refused for that long. Counted per instance.
- **Memory**: each upload body is held in memory until it is committed. At most `gara.s3.max-concurrent-uploads` (8) uploads run at once per instance; more get `SlowDown`, which S3 clients retry. With the 100MB object limit, budget at least 1GB of heap for uploads, plus downloads in flight.
- **Shutdown** is graceful: in-flight requests (and their commits) finish within 60 seconds, and the instance releases its leases.
- **GitHub calls.** Client requests cost: GET 1 call (by the stored blob sha), PUT/DELETE 1 commit, COPY 1 read + 1 commit, completing a multipart upload 1 commit, HEAD and LIST none. A write that GitHub rejects as stale (the file was edited outside Gara) costs 1 extra read and a retry. GitHub allows 5,000 API calls per hour per token; writes also have their own lower limit, which `gara.github.writes-per-hour` stays under. Calls Gara makes on its own:

  | When | Calls | Switch |
  | --- | --- | --- |
  | Startup check | 1 + 1 per bucket | `gara.github.verify-on-startup` |
  | Sandbox startup (gara-sandbox only) | 0 once its bucket exists, otherwise 1 to 4 | |
  | Pool scheduler check | 0 (Postgres only) | `gara.volume.pool-check-interval` |
  | Creating a repository (scheduler, or upload when none is free) | about 3 | `gara.volume.min-active` / `max-active` |
  | Health, metrics, admin UI, lease renewal, multipart cleanup | 0 (Postgres only) | |

- **Backups**: back up Postgres regularly. It is the only record of which repository holds which key; GitHub keeps the file contents.

## Limits

| Setting | Default | Why |
| --- | --- | --- |
| `gara.object.max-size` | 100MB | GitHub rejects files above 100MB; applies to plain uploads and to completed multipart objects |
| `gara.volume.max-size` × `fill-threshold` | 1GB × 0.9 | GitHub recommends repositories stay under 1GB (history included) |
| `gara.volume.max-files` | 50,000 | Recursive git trees are truncated at 100,000 entries |
| `gara.volume.min-active` / `max-active` | 2 / 3 | Repositories with room kept ready / most active at once; each repository takes one commit at a time |
| `gara.volume.nearly-full` | 0.8 | Share of a repository's limits from which the scheduler prepares its replacement |
| `gara.github.writes-per-hour` / `write-burst` | 450 / 20 | GitHub's secondary limits are roughly 80 content-creating requests per minute and 500 per hour, per account |
| `gara.object.multipart-expiry` | 7 days | Unfinished multipart uploads are dropped from Postgres |
| `gara.s3.max-concurrent-uploads` | 8 | Upload bodies held in memory at once, per instance |

`/admin`, `/actuator` and `/sandbox` (when running gara-sandbox) are reserved paths, so they cannot be used as bucket names.

## Releases and the public mirror

Development happens in the private repository `eyegil/gara`; `hindreaming/gara` is its public mirror.
The `Publish` workflow (`.github/workflows/publish.yml`, runs only in the private repository) does both jobs:

- Every pushed branch and tag is pushed as is to `hindreaming/gara`.
- A tag starting with `v` (for example `v1.0.0`) is also built and tested, and a release is created on `hindreaming/gara`
  with `gara-exec.jar` (Gara) and `gara-sandbox.jar` (Gara with the sandbox page) attached.

It needs one secret in `eyegil/gara`, `PUBLIC_MIRROR_TOKEN`: a fine-grained personal access token of the
`hindreaming` account, for the `hindreaming/gara` repository only, with **Contents** and **Workflows** read and write.

```powershell
git tag v1.0.0; git push origin v1.0.0
```

## License

Apache License 2.0, see [LICENSE](LICENSE).

## Tests

```powershell
./mvnw test
```

Integration tests start the real application on a random port with an embedded Postgres 17 (no Docker needed) and a fake GitHub. They drive it with the AWS SDK.
