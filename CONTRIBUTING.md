# Contributing to Nautilus

Thanks for your interest in contributing. This doc covers everything you need to ship your first PR.

## Ways to contribute

- **Code** — pick a [`good-first-issue`](https://github.com/kloudocean/nautilus/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) or an item from the [roadmap](ROADMAP.md).
- **Docs** — docs PRs are first-class. Fix a typo, add an example, write a guide.
- **Triage** — reproduce bugs, label issues, answer questions in discussions.
- **RFCs** — propose a feature via a Discussion before a large PR.

## Local setup

Requirements:

- JDK 21 (Temurin recommended)
- Docker + Docker Compose
- Gradle 8.10+ (or use the included `./gradlew` wrapper)

```bash
git clone https://github.com/kloudocean/nautilus.git
cd ai-gateway
cp .env.example .env            # add provider keys
docker compose up -d postgres redis
./gradlew bootRun
```

Run the tests:

```bash
./gradlew check
```

## Branch and PR flow

1. Fork the repo and create a branch off `main`:
   `feat/<short-description>` or `fix/<short-description>`
2. Make small, logical commits.
3. Run `./gradlew check` before you push.
4. Open a PR against `main`. Fill in the PR template.
5. A maintainer will review within a few days. Expect feedback — that's the point.

## Commit format

We use conventional commits:

```
type(scope): short description

Optional longer body explaining why (not what).
```

Types: `feat` `fix` `docs` `refactor` `test` `chore` `build` `ci` `perf`.

Examples:

```
feat(routing): add Mistral provider adapter
fix(cache): handle null embeddings in lookup
docs: clarify semantic cache TTL behavior
```

Keep commits small — reviewers should be able to understand each one in isolation.

## Code style

- **Java** — follow standard Google Java Style. Run `./gradlew spotlessApply` before committing.
- **Gradle** — Kotlin DSL (`build.gradle.kts`). Keep dependencies grouped and commented where non-obvious.
- **Spring** — prefer constructor injection, records for DTOs, `@ConfigurationProperties` over `@Value`.
- **Tests** — every non-trivial change needs a test. Integration tests live in `src/test/java/.../integration`; unit tests beside the class under test.
- **No breaking changes to the public API** without an RFC discussion first.

## Pull request checklist

- [ ] Code compiles (`./gradlew compileJava`)
- [ ] Tests pass (`./gradlew check`)
- [ ] Formatted (`./gradlew spotlessApply`)
- [ ] New features have tests
- [ ] New user-facing features have docs
- [ ] Commit messages follow conventional commits
- [ ] PR description links the related issue (if any)

## Reporting bugs

Use the Bug issue template. A good report includes:

- AI Gateway version
- Java version and OS
- Minimal reproduction steps
- Expected vs. actual behavior
- Relevant logs (redact API keys)

## Proposing features

1. Open a Discussion or an issue with the `rfc` label.
2. Describe the problem first, the proposed solution second.
3. Wait for maintainer feedback before investing in a big PR — we'll save you time.

## Code of Conduct

All contributors are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md). Be kind, be patient, be precise.

## License

By contributing, you agree your contributions will be licensed under Apache 2.0, the same license as this project.
