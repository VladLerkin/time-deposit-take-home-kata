---
description: Core architectural, TDD, and backward-compatibility rules for the Time Deposit Kata
---

# Project Rules

1. Follow `AGENTS.md` and `docs/implementation_plan.md` at all times.
2. Never move `TimeDeposit` or `TimeDepositCalculator` out of package `org.ikigaidigital`.
3. Never break the `TimeDeposit` constructor/properties, no-arg `TimeDepositCalculator()` constructor, or `updateBalance(xs: List<TimeDeposit>)` signature.
4. Keep `org.ikigaidigital.domain` and `org.ikigaidigital.application` free of Spring and Jakarta EE dependencies; enforce with ArchUnit.
5. Every commit must be atomic, follow Conventional Commits, and pass `./mvnw verify` with `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
