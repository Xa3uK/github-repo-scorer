# GitHub Repo Scorer

Spring Boot REST API that returns top 100 most popular GitHub repositories for a given programming language and creation date.

## Tech Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5, Spring MVC |
| Cache | Redis (Spring Cache) |
| Build | Gradle 9.4.1 |
| Testing | JUnit 5, Mockito, Testcontainers |
| Infrastructure | Docker, Docker Compose |

## Running with Docker

```bash
docker compose up --build
```

Starts the app on port `8080` and Redis on port `6379`.

With a GitHub token (~5000 req/hr limit):

```bash
export GITHUB_API_TOKEN=your_token_here
docker compose up --build
```

To run the app locally without Docker (Redis must be running separately):

```bash
./gradlew bootRun
```

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GITHUB_API_TOKEN` | no | empty | GitHub PAT — omit to run unauthenticated (60 req/hr limit) |
| `REDIS_HOST` | no | `localhost` | Redis host |
| `REDIS_PORT` | no | `6379` | Redis port |

## API Reference

### Get popular repositories

```
GET /api/v1/repositories/popular
```

| Parameter | Type | Required | Description |
|---|---|---|---|
| `language` | string | yes | Must start with a letter; letters, digits, `#`, `+`, `.`, `-`, spaces allowed; max 50 characters |
| `createdAfter` | `YYYY-MM-DD` | yes | Minimum repository creation date; must be within the last year, not in the future |

**Example request**

```bash
curl "http://localhost:8080/api/v1/repositories/popular?language=Java&createdAfter=2026-01-31"
```

**Example response**

```json
[
  {
    "id": 1178833585,
    "fullName": "iflytek/skillhub",
    "htmlUrl": "https://github.com/iflytek/skillhub",
    "language": "Java",
    "stars": 1110,
    "forks": 138,
    "createdAt": "2026-03-11T12:17:05Z",
    "pushedAt": "2026-03-31T18:06:16Z",
    "score": 1.0
  }
]
```

**Error responses**

| Condition | Status |
|---|---|
| Missing or invalid parameter | 400 |
| `language` fails format validation | 400 |
| `createdAfter` is in the future or older than 1 year | 400 |
| GitHub rate limit hit | 429 |
| GitHub server error | 502 |
| GitHub unreachable | 503 |

## Scoring Logic

Each repository receives a score in the range `[0.0, 1.0]`:

```
score = 0.65 × starScore + 0.25 × forkScore + 0.10 × recencyScore
```

- **starScore** = `min(1.0, stars / P95_stars)` — normalized against the 95th percentile of the candidate set to reduce outlier impact
- **forkScore** = `min(1.0, forks / P95_forks)` — same normalization
- **recencyScore** — based on `pushed_at`:

| Last pushed | Score |
|---|---|
| 0 – 7 days | 1.0 |
| 8 – 30 days | 0.8 |
| 31 – 90 days | 0.5 |
| 91 – 180 days | 0.2 |
| > 180 days | 0.0 |

Repositories are sorted descending by score. Tie-breakers: stars → forks → `pushedAt` → `createdAt`.

Results are cached in Redis for 30 minutes. Cache key: `popularRepositories::<language>:<createdAfter>`.

## Running Tests

Unit tests (no Docker required):

```bash
./gradlew test --tests "com.koval.githubreposcorer.unit.*"
```

Web layer tests (no Docker required):

```bash
./gradlew test --tests "com.koval.githubreposcorer.web.*"
```

Integration tests (requires Docker for Redis):

```bash
./gradlew test --tests "com.koval.githubreposcorer.integration.*"
```

Full test suite:

```bash
./gradlew test
```

## Project Structure

```
src/main/java/com/koval/githubreposcorer/
├── api/
│   ├── controller/     # REST controllers
│   ├── exception/      # Global exception handler
│   └── response/       # Response DTOs
├── client/             # GitHub HTTP client with retry
├── config/
├── model/
│   ├── github/         # GitHub API response models
│   └── result/         # Scored result models
├── service/            # Business logic and orchestration
├── util/               # Percentile and recency helpers
└── GithubRepoScorerApplication.java
```
