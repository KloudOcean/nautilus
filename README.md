<div align="center">

<img src="docs/assets/logo.png" alt="KloudOcean Nautilus" width="120" />

# KloudOcean Nautilus

**The Java-native AI Gateway. One API, every model.**

[![Latest Release](https://img.shields.io/github/v/release/kloudocean/nautilus?style=flat-square&label=release&color=0A1628&include_prereleases&display_name=tag&sort=semver)](https://github.com/kloudocean/nautilus/releases)
[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache_2.0-blue?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/status-alpha-orange?style=flat-square)](ROADMAP.md)
[![Good First Issues](https://img.shields.io/github/issues/kloudocean/nautilus/good-first-issue?style=flat-square&label=good%20first%20issues)](https://github.com/kloudocean/nautilus/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)

[Quickstart](#quickstart)  ·  [Features](#features)  ·  [Roadmap](ROADMAP.md)  ·  [Contributing](CONTRIBUTING.md)  ·  [Discussions](https://github.com/kloudocean/nautilus/discussions)

</div>

---

> **One gateway. Many models. Built for the JVM.**

The AI tooling ecosystem is Python and TypeScript heavy. Java and Spring shops that want to adopt LLMs have been stuck stitching together SDKs, writing their own retry logic, and duct-taping observability. **Nautilus** is a first-class, Spring-native gateway that solves that — one idiomatic API to route, cache, rate-limit, observe, and audit calls across every major LLM provider.

Named after the spiral-shelled creature whose design has endured for 500 million years. Built with the same intent.

---

## Features

- **Multi-provider routing** — Claude, OpenAI, Llama, Mistral behind a single API
- **Smart fallback** — automatic failover when a provider errors or rate-limits
- **Semantic cache** — pgvector-backed response cache keyed by embedding similarity
- **Rate limiting** — per-key, per-route, per-model
- **Cost tracking** — token and dollar accounting per request, per team, per key
- **Streaming and non-streaming** — SSE, WebSocket, and blocking responses
- **Observability** — Micrometer metrics, structured logs, OpenTelemetry traces
- **Spring Boot idiomatic** — auto-configuration, properties-based setup, starter dependency

## Why Java-native

- **Zero context switching** for JVM teams — no sidecar, no Python microservice
- **Production defaults built in** — circuit breakers, timeouts, retries, backpressure
- **Enterprise fit** — RBAC-friendly, audit log ready, OIDC-compatible
- **Predictable performance** — JVM tuning you already know

## Quickstart

### With Docker Compose

```bash
git clone https://github.com/kloudocean/nautilus.git
cd nautilus
cp .env.example .env      # add your provider API keys
docker compose up
```

Nautilus is live at `http://localhost:8080`.

### Make a request

```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer $GATEWAY_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "auto",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

`model: "auto"` lets Nautilus pick the best provider based on your routing rules. Or pin it: `"model": "claude-opus-4-6"`.

### Add to a Spring Boot app

```xml
<dependency>
  <groupId>com.kloudocean</groupId>
  <artifactId>nautilus-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

```yaml
nautilus:
  url: http://localhost:8080
  api-key: ${GATEWAY_KEY}
  default-model: auto
```

```java
@Autowired
private NautilusClient nautilus;

ChatResponse response = nautilus.chat()
    .message("Summarize this PR")
    .send();
```

Full docs in [docs/](docs/). Start with the [routing strategies guide](docs/guides/routing-strategies.md), and before going to production read the [log redaction guide](docs/guides/log-redaction.md).

## Architecture

```
          +------------------+
  Apps -->|     Nautilus     |--> Claude
          |  (Spring Boot)   |--> OpenAI
          |                  |--> Llama (Ollama)
          |  - Routing       |--> Mistral
          |  - Cache         |
          |  - Rate limit    |
          |  - Observability |
          +------------------+
                  |
                  v
         PostgreSQL + pgvector
         Redis (cache, limits)
```

## Roadmap

See [ROADMAP.md](ROADMAP.md) for what we're building next. Highlights for v0.1:

- [x] Multi-provider routing (3+ providers)
- [x] Streaming and non-streaming
- [ ] Semantic cache (pgvector)
- [ ] Cost tracking API
- [ ] Admin UI (Angular)

## Contributing

We plan in public and welcome contributors at every level.

- **New here?** Start with a [`good-first-issue`](https://github.com/kloudocean/nautilus/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22).
- **Want to propose something?** Open a [Discussion](https://github.com/kloudocean/nautilus/discussions) or an issue with the `rfc` label.
- **Shipping a PR?** Read [CONTRIBUTING.md](CONTRIBUTING.md) first — it's short.

All contributors are credited in release notes and occasionally featured on the [KloudOcean Academy YouTube channel](https://www.youtube.com/@KloudOceanAcademy).

## License

Apache 2.0 — see [LICENSE](LICENSE).

---

<div align="center">

Built by [KloudOcean](https://www.kloudocean.com) — cloud depth meets AI intelligence.

</div>
