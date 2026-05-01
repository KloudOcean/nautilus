# Redacting sensitive content in logs

Nautilus logs every inbound request, every provider call, and every response. That makes the logs invaluable for debugging — and dangerous if they leak. By default Nautilus redacts the obvious things (API keys, bearer tokens). For everything else, you tell Nautilus what is sensitive and how aggressive to be.

This guide covers what gets redacted out of the box, what you should configure for your deployment, and how the masking works underneath.

---

## What's sensitive

Three categories show up in gateway logs and need different treatment.

| Category                       | Examples                                                                                | Default in Nautilus      |
|--------------------------------|------------------------------------------------------------------------------------------|---------------------------|
| **Credentials**                | `Authorization: Bearer …`, `x-api-key`, provider keys (`OPENAI_API_KEY`, etc.)            | Always redacted           |
| **Prompt content**             | User messages, system prompts, tool inputs — may contain PII, customer data, secrets      | Logged in full by default |
| **Response content**           | Model output — may echo PII or regurgitate prompts                                       | Logged in full by default |
| **Identifiers**                | User IDs, session IDs, tenant IDs                                                         | Logged in full (not redacted) |

Nautilus is conservative on credentials and permissive on content. Most teams need to flip content logging off, or at least redact PII patterns, before going to production.

---

## What's redacted out of the box

The default logger applies these rules with no configuration:

1. **HTTP `Authorization` header** — replaced with `Bearer ***` (the `***` carries 6 characters of randomness so two redacted lines from the same key don't collide visually, but the secret itself is gone).
2. **`x-api-key` and any header matching `*-api-key`** — replaced with `***`.
3. **Provider API keys in upstream request bodies** — Nautilus injects the configured provider key right before sending; that key is never in the inbound or outbound log entry.
4. **Cookie headers** — masked entirely.
5. **`Set-Cookie` response headers** — masked entirely.
6. **Configuration values flagged as secrets** — anything bound through `@ConfigurationProperties` with `@Sensitive` (or matching `nautilus.config.secret-property-patterns`) is masked in the startup banner and `/actuator/configprops` output.

Headers redaction happens at the access-log layer; configuration redaction happens in Spring Boot's `Sanitizer` chain. Both run before any log line is emitted.

---

## Recommended production configuration

For production, the minimum to set:

```yaml
nautilus:
  logging:
    redact:
      # Redact request and response bodies entirely.
      request-body: hash       # one of: full | hash | length-only | off
      response-body: hash

      # Mask common PII patterns wherever they appear in logs.
      patterns:
        - email
        - phone
        - credit-card
        - ssn
        - ip-address       # opt-in — useful for GDPR posture

      # Truncate any single log field to bound disk impact.
      max-field-length: 2048
```

### `request-body` and `response-body` modes

| Mode           | What gets logged                                                                 | When to use it                                                                  |
|----------------|----------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `full`         | Body verbatim                                                                    | Local dev only                                                                   |
| `hash`         | A SHA-256 hash plus length and content type — `body=sha256:abc1234… len=412`     | Default for staging/prod; lets you correlate identical bodies without leaking content |
| `length-only`  | Length and content type, no hash                                                 | Strictest mode for highly regulated environments                                 |
| `off`          | The body field is omitted entirely                                               | When you want zero metadata about prompts in logs                                |

### Pattern-based PII masking

Pattern names map to bundled regexes in `com.kloudocean.nautilus.logging.redact.patterns`:

| Pattern        | Matches                                                                               |
|----------------|----------------------------------------------------------------------------------------|
| `email`        | RFC 5322-ish email addresses                                                           |
| `phone`        | E.164 + common international/US national formats                                       |
| `credit-card`  | 13–19 digit PANs, with optional spaces or dashes, validated by Luhn                   |
| `ssn`          | US Social Security Numbers                                                             |
| `ip-address`   | IPv4 and IPv6                                                                          |
| `iban`         | International Bank Account Numbers                                                     |

Matched substrings are replaced with `[REDACTED:<pattern>]`. So an email becomes `[REDACTED:email]`. The replacement keeps the original length only if you set `redact.preserve-length: true`.

---

## Adding your own patterns

You can register custom regexes via configuration:

```yaml
nautilus:
  logging:
    redact:
      custom-patterns:
        - name: customer-id
          # match cust-XXXX where X is alphanumeric
          regex: 'cust-[A-Za-z0-9]{4,}'
          replacement: '[REDACTED:customer-id]'

        - name: jwt
          regex: 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+'
          replacement: '[REDACTED:jwt]'
```

**Notes:**
- Regex flavour is Java's `java.util.regex` (Perl-compatible). `(?i)` for case-insensitive.
- Patterns run in the order declared. If two patterns both match, the first wins.
- Custom patterns apply on top of (not instead of) the bundled patterns. Disable a bundled pattern by listing it in `redact.disabled-patterns: [ip-address]`.

For complex masking (e.g. partial unmasking — "show last four digits of card"), implement `RedactionRule` and register it as a Spring bean. The interface is two methods:

```java
public interface RedactionRule {
  String name();
  String redact(String input);
}
```

Beans are picked up automatically and run after the configured patterns.

---

## Field-level redaction

Pattern masking runs over every log message body. For finer control over which JSON fields get redacted, use `field-redaction`:

```yaml
nautilus:
  logging:
    redact:
      json-fields:
        # JSON pointer paths in known event payloads
        - $.request.headers.authorization
        - $.request.body.messages[*].content
        - $.response.body.choices[*].message.content
        - $.metadata.user.email
```

The path syntax is JSONPath. Matching nodes are replaced with `"[REDACTED]"` before the message is rendered. Field-level rules run before pattern rules — so once a field is masked, patterns won't see the original content.

---

## Per-route and per-tenant overrides

Some routes carry sensitive content that should never be logged in any form; some carry pure metadata that's fine to log fully. Override the defaults per route:

```yaml
nautilus:
  logging:
    redact:
      response-body: hash       # default
      routes:
        - match:
            path: /v1/embeddings
          response-body: length-only   # embeddings vectors are huge and useless in logs
        - match:
            path: /v1/audit/*
          response-body: off            # never log audit responses
        - match:
            tenant: pii-tenant-*
          patterns: [email, phone, credit-card, ssn, iban]
```

First-match-wins, like routing strategies.

---

## Verifying redaction

Before going to production, **read your logs**. Run a representative request load locally with prod-like logging config and grep for things that should be gone:

```bash
docker compose logs nautilus | grep -E '(Bearer [A-Za-z0-9]|sk-[A-Za-z0-9]|[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,})' \
  || echo "no obvious leaks"
```

If you have a staging environment, do the same against accumulated logs there. CI can include a smoke test that asserts redaction — see the example in `src/test/java/.../logging/RedactionSmokeTest.java` *(planned).*

---

## What Nautilus does not redact (yet)

- **Stack traces.** Exception messages may quote untrusted input. Until structured exception masking lands (planned, see [#15 follow-ups](https://github.com/kloudocean/nautilus/issues?q=label%3Aarea%3Aobservability+exception+redact)), assume stack traces can leak content. If your error handler echoes user input into the message, sanitise at the source.
- **Database query logs.** If you run with `spring.jpa.show-sql=true`, parameter values are not redacted. Leave SQL logging off in production.
- **Provider error responses.** Some providers echo your prompt back in error bodies. Nautilus logs the upstream error verbatim by default — pattern masking will catch the obvious things, but content-mode `hash` on `response-body` does not currently apply to error responses. Track [the issue](https://github.com/kloudocean/nautilus/issues) if this matters for you.

---

## Compliance posture cheatsheet

| Posture                | Recommended config                                                                                  |
|------------------------|-----------------------------------------------------------------------------------------------------|
| Local dev              | Defaults are fine                                                                                   |
| Internal staging       | `request-body: hash`, `response-body: hash`, bundled PII patterns on                               |
| Customer-facing prod   | All of the above + custom patterns for your identifiers + per-route off-mode for sensitive routes  |
| HIPAA / PCI / strict   | `request-body: length-only`, `response-body: off`, all patterns on, structured field-level masking |

When in doubt: log less. You can always add a debug toggle for ad-hoc investigation; you cannot un-log a leak.

---

## Related

- [docs/guides/routing-strategies.md](./routing-strategies.md) — how requests are routed (lands in #3)
- Issue [#7](https://github.com/kloudocean/nautilus/issues/7) — structured logging for provider latency
- Issue [#19](https://github.com/kloudocean/nautilus/issues/19) — cost tracking persistence
- Spring Boot Sanitizer reference: [Spring Boot Actuator config sanitization](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
