# Roadmap

Public, honest, and always changing. If something is uncertain, it lives in **Later** or is marked `(RFC)`.

---

## Now — v0.1 (alpha)

Foundational features for a credible first release.

- [ ] Multi-provider routing — Claude, OpenAI, Llama (Ollama), Mistral
- [ ] Streaming (SSE) and non-streaming responses
- [ ] Provider fallback on error or rate-limit
- [ ] Request and response logging with API-key redaction
- [ ] Basic cost tracking — tokens and dollars per request
- [ ] Health check and readiness endpoints
- [ ] Docker + docker-compose quickstart
- [ ] Spring Boot starter (`ai-gateway-starter`)
- [ ] README with 5-minute quickstart
- [ ] 15+ `good-first-issue`s seeded

## Next — v0.2

Production readiness and the first admin surface.

- [ ] Semantic cache backed by pgvector
- [ ] Rate limiting — per-key, per-route, per-model
- [ ] Admin UI (Angular) — keys, usage, cost dashboard
- [ ] OpenTelemetry traces
- [ ] RBAC for admin API
- [ ] Helm chart
- [ ] Prometheus metrics dashboard (Grafana JSON)

## Later — v0.3+

Aspirational and RFC-stage.

- [ ] Prompt templates and versioning `(RFC)`
- [ ] Evals integration — run eval suites through the gateway `(RFC)`
- [ ] Bring-your-own provider SDK (plugin architecture) `(RFC)`
- [ ] Multi-tenant with per-tenant routing rules `(RFC)`
- [ ] Fine-grained content filtering `(RFC)`
- [ ] GraphQL facade `(RFC)`

---

## How we prioritize

1. **Unblocks a real user** — an open issue with clear pain.
2. **Differentiates us** — Java-native capabilities Python gateways cannot easily match.
3. **Pays down debt** — reliability, tests, docs.
4. **Fun and interesting** — but only after the first three.

## Proposing a roadmap change

Open a Discussion or an issue labelled `rfc`. Describe the problem, the proposed solution, and what you would build if no one else picked it up.
