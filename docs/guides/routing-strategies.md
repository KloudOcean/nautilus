# Choosing a routing strategy

Nautilus sits between your application and one or more LLM providers. Every request goes through a **routing strategy** that picks which provider (and model) handles it. The strategy you choose changes cost, latency, reliability, and how you reason about behaviour.

This guide explains the strategies Nautilus ships with, when each is the right call, and how to combine them with fallback to keep things resilient.

---

## At a glance

| Strategy       | What it does                                                        | When to pick it                                                                  |
|----------------|---------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `priority`     | Picks the highest-priority healthy provider                         | You have a clear preferred provider and use the others as backups                 |
| `round-robin`  | Distributes requests evenly in turn across the eligible providers    | You want predictable load balancing across providers of similar capability        |
| `random`       | Picks an eligible provider at random (uniform or weighted)          | You want statistical A/B testing across providers, or simple load spreading       |
| `cost-aware`   | Picks the cheapest eligible provider for the request profile *(planned)* | You are cost-sensitive and accept slightly higher variance in latency or quality |
| `latency-aware`| Picks the provider with the lowest recent p95 latency *(planned)*    | You are latency-sensitive and want adaptive routing                              |

Strategies marked *(planned)* are on the roadmap. The first three are the ones to plan around for the v0.1 release.

---

## `priority` — preferred provider with deterministic fallback

The default and simplest strategy. Each provider entry has a `priority` value (lower number = higher priority). Nautilus walks the priority list and picks the first provider that is **healthy** and **within rate limits**.

```yaml
nautilus:
  routing:
    strategy: priority
    providers:
      - name: claude
        priority: 1
      - name: openai
        priority: 2
      - name: ollama
        priority: 3
```

**Behaviour:**
- Claude handles every request when healthy.
- If Claude is rate-limited, errors, or is marked unhealthy, OpenAI takes over.
- If OpenAI also fails, traffic flows to Ollama.

**Pick this when:**
- You have a clear preferred provider for quality, contract, or compliance reasons.
- You want failover behaviour that is easy to reason about and easy to debug — the same provider serves traffic until it can't.
- You are running a single-tenant deployment where consistency matters more than load distribution.

**Watch out for:**
- The top-priority provider absorbs 100% of healthy traffic. Cost optimisation is hard to layer on top.
- "Sticky" failure modes — if your primary provider is degrading slowly (high latency but no errors), `priority` will keep using it. Pair with health checks tuned for latency, or move to `latency-aware` once available.

---

## `round-robin` — even load across equal providers

Distributes requests in turn across all eligible providers. Each request advances a per-route cursor, so requests `1, 2, 3, 4` go to providers `A, B, C, A` and so on.

```yaml
nautilus:
  routing:
    strategy: round-robin
    providers:
      - name: claude
      - name: openai
      - name: gemini
```

**Behaviour:**
- Each provider receives roughly `1 / N` of the traffic.
- Unhealthy providers are skipped automatically; the cursor advances to the next healthy one.
- Cursor state lives in memory per gateway instance — see "Multi-instance behaviour" below.

**Pick this when:**
- Your providers are roughly interchangeable for the request profile (same model class, similar pricing, similar quality).
- You want predictable, even load — useful for staying inside per-provider quotas without dynamic accounting.
- You want to reduce the blast radius of a single provider's outage to `1 / N` of requests rather than 100%.

**Watch out for:**
- "Roughly interchangeable" is doing a lot of work. If providers differ in quality, response shape, or model availability, round-robin will produce inconsistent UX.
- Per-instance cursors mean three Nautilus instances each running round-robin will not produce a globally even split — distribution is even *per instance*. Acceptable for most workloads; not acceptable if you have strict per-provider quotas. Use a centralised dispatcher (or per-key rate limits) in that case.

---

## `random` — statistical spread, with optional weights

Picks an eligible provider at random. Supports uniform (default) and weighted distribution.

```yaml
nautilus:
  routing:
    strategy: random
    providers:
      - name: claude
        weight: 0.5
      - name: openai
        weight: 0.3
      - name: gemini
        weight: 0.2
```

**Behaviour:**
- With no weights, every healthy provider is equally likely.
- With weights, providers are picked proportionally. Weights do not need to sum to 1.0 — they are normalised across the *currently healthy* set.
- Useful for A/B testing: set `weight: 0.05` on a new provider and route 5% of traffic to it without a code change.

**Pick this when:**
- You are evaluating a new provider or model and want a controlled traffic share. Pair with metrics tags to compare quality and cost between buckets.
- You want to spread load across many providers without a coordinator.
- You explicitly want statistical mixing — for example, to avoid a single provider seeing all of one user's traffic.

**Watch out for:**
- Random is harder to reason about during incidents. "Why did this request go to provider X?" has no causal answer beyond the seed.
- Without sticky session keys, the same user can hit different providers across requests. If your downstream relies on conversation continuity (some streaming patterns, some agentic loops), that's a problem. Use `priority` or sticky-routing for those flows.

---

## Combining strategies with fallback

Routing strategies pick the **first** provider for a request. Fallback decides what happens when the first pick errors or is rate-limited.

Nautilus enables fallback by default — if the strategy's chosen provider fails, the request retries against the next eligible provider in the strategy's order. You can shape this with:

```yaml
nautilus:
  routing:
    strategy: priority
    fallback:
      enabled: true
      max-attempts: 3
      retry-on:
        - rate-limit
        - timeout
        - server-error
      backoff:
        initial: 100ms
        multiplier: 2
        max: 2s
```

**Notes on fallback:**
- Fallback is **per-request**, not per-deployment. The strategy still owns the steady-state distribution.
- Combining `random` with fallback is fine — on first-pick failure, Nautilus walks the remaining providers in their declared order, not at random.
- Streaming requests have their own concerns: fallback after the first chunk is sent is usually a bad idea (you would emit two partial responses). Nautilus disables mid-stream fallback by default; opt in with `fallback.streaming: replay`.

---

## Multi-instance behaviour

If you run more than one Nautilus instance behind a load balancer, the strategy still works — but state is local to each instance:

| Strategy      | Local state                                | Implication                                                    |
|---------------|--------------------------------------------|-----------------------------------------------------------------|
| `priority`    | None — strategy is a pure function          | Identical behaviour across instances                            |
| `round-robin` | Per-instance cursor                         | Even per-instance, not globally even                            |
| `random`      | None (PRNG state is per-request)            | Statistically even across the deployment as request count grows |

For workloads needing globally even distribution (strict per-provider quotas, regulatory load balancing), pair the strategy with rate-limit policies that operate on the cluster level — see [Rate limiting](./rate-limiting.md) *(planned doc).*

---

## Picking a strategy: a short decision tree

1. **Do you have one preferred provider and treat the rest as backups?** → `priority`.
2. **Are providers interchangeable and you want even load?** → `round-robin`.
3. **Are you running an A/B test or want statistical mixing?** → `random` (with weights).
4. **Do you care primarily about cost, and are willing to accept variance?** → `cost-aware` *(planned)*.
5. **Do you care primarily about p95 latency?** → `latency-aware` *(planned)*.

When in doubt, start with `priority` — it is the easiest to reason about during incidents.

---

## Configuring per-route strategies

The top-level `nautilus.routing.strategy` sets the default. You can override per route:

```yaml
nautilus:
  routing:
    strategy: priority
    providers: [...]
    routes:
      - match:
          path: /v1/chat/completions
          model: cheap-*
        strategy: cost-aware     # planned
      - match:
          path: /v1/embeddings
        strategy: round-robin
```

Route matching is first-match-wins. The default strategy applies to anything not matched.

---

## Where the strategies live in the code

The strategy interface and built-in implementations live under `com.kloudocean.nautilus.routing.strategy`. Each strategy is a Spring bean discovered by name; bringing your own is a matter of implementing `RoutingStrategy` and registering a bean. Plugin-style strategies are tracked in [#22](https://github.com/kloudocean/nautilus/issues/22).

---

## Related

- [ROADMAP.md](../../ROADMAP.md) — what's coming for routing in v0.2
- Issue [#8](https://github.com/kloudocean/nautilus/issues/8) — round-robin implementation
- Issue [#9](https://github.com/kloudocean/nautilus/issues/9) — `model: random` for A/B testing
- Issue [#22](https://github.com/kloudocean/nautilus/issues/22) — plugin architecture for custom strategies
