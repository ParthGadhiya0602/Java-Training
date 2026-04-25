---
title: Home
nav_order: 0
description: "Beginner to Production-Ready — 59 modules covering core Java through cloud deployment."
permalink: /
---
{% raw %}

# Java Training — Beginner to Production-Ready
{: .no_toc }

A structured, module-by-module Java training curriculum. Each module contains
real, non-trivial examples designed to force genuine understanding — not just
copy-paste familiarity.
{: .fs-5 .fw-300 }

---

## How This Repo Works

- Each module lives in its own directory: `module-NN-topic-name/`
- Every module has a theory document with **diagrams + annotated snippets** — read it first, run the code second
- Code examples are intentionally non-trivial — simple enough to follow, complex enough to be worth studying
- Build tool progression: early modules use **Maven**, later modules introduce **Gradle**
- Work through modules **in order** — later modules build on earlier ones

---

## Curriculum at a Glance

| Phase | Modules | Topics |
|---|---|---|
| [Phase 1 — Fundamentals](phase-1-fundamentals) | 01–11 | Language, OOP, exceptions |
| [Phase 2 — Core APIs](phase-2-core-apis) | 12–24 | Generics, collections, streams, concurrency, JVM |
| Phase 3 — Intermediate Engineering | 25–30 | Design patterns, testing, build tools |
| Phase 4 — Databases & Persistence | 31–35 | JDBC, JPA/Hibernate, Spring Data, migrations |
| Phase 5 — Spring Ecosystem | 36–45 | Boot, REST, security, reactive, batch |
| Phase 6 — Production & Architecture | 46–59 | Microservices, cloud, observability, CI/CD |

> **Total: 59 modules.**  Java 21 LTS throughout.

---

## Prerequisites

- macOS, Linux, or Windows (WSL recommended)
- Basic terminal / command-line comfort
- Nothing else — we install everything from Module 01

---

## Java Version

This training targets **Java 21 LTS**. Modern features used throughout:
records, sealed classes, switch expressions, text blocks, virtual threads, pattern matching.

---

{: .tip }
> **Start here →** [Module 01 — Environment Setup](module-01-environment-setup/module-01-environment-setup)
{% endraw %}
