# AI Agent Guidelines & Architectural Rules (`AGENTS.md`)

This repository uses a spec-driven, TDD-first agentic workflow. All AI coding agents working on this project must strictly adhere to the rules below and follow [docs/implementation_plan.md](docs/implementation_plan.md).

---

## 1. Strict Backward Compatibility Constraints (Non-Negotiable)

1. **Original Package Placement**: `TimeDeposit` and `TimeDepositCalculator` **must remain in package `org.ikigaidigital`**. Do not move them to subpackages (e.g., `org.ikigaidigital.domain`), as external evaluation tests import them from `org.ikigaidigital`.
2. **`TimeDeposit` Data Class**: Do not modify fields, types (`id: Int`, `planType: String`, `var balance: Double`, `val days: Int`), visibility, or constructor parameter order.
3. **`TimeDepositCalculator` Constructor & Method Signature**:
   - Must remain instantiable with **no arguments**: `TimeDepositCalculator()`. Any injected dependencies must define a default value (e.g., `class TimeDepositCalculator(private val policies: InterestPolicyRegistry = InterestPolicyRegistry.default())`).
   - `fun updateBalance(xs: List<TimeDeposit>)` signature and in-place list mutation semantics must remain unchanged.
4. **Exact Interest Rounding Formula**:
   - Preserve `BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).toDouble()` in `TimeDepositCalculator` so rounding behavior on IEEE 754 `Double` inputs remains 100% identical to the legacy implementation.
5. **Bytecode Target**: Keep Java 17 bytecode compatibility (`<java.version>17</java.version>`, `jvmTarget=17`) so the project builds and runs on any JDK 17+ environment.

---

## 2. Architecture & Layer Boundaries (Hexagonal Architecture)

```
org.ikigaidigital                         ← Legacy core: TimeDeposit, TimeDepositCalculator + Application.kt
org.ikigaidigital.domain                  ← Domain models (Withdrawal, TimeDepositView), InterestPolicy, EligibilityRule
org.ikigaidigital.application             ← Inbound ports (use case interfaces), use case services, outbound ports (no Spring!)
org.ikigaidigital.adapter.rest            ← REST controllers & DTO mappers
org.ikigaidigital.adapter.rest.api        ← OpenAPI-generated controller interfaces
org.ikigaidigital.adapter.rest.dto        ← OpenAPI-generated DTOs (TimeDepositDto, WithdrawalDto)
org.ikigaidigital.adapter.persistence     ← JPA entities, Spring Data repositories, persistence port adapters
org.ikigaidigital.config                  ← Spring bean wiring (UseCaseConfig) & TransactionalUpdateAllBalances decorator
```

- **Domain Purity**: Classes in `org.ikigaidigital` (excluding `Application`) and `org.ikigaidigital.domain..` must have zero dependencies on `org.springframework..`, `jakarta..`, `..application..`, `..adapter..`, or `..config..`.
- **Framework-Free Application Layer**: `org.ikigaidigital.application..` must not depend on `org.springframework..`, `jakarta..`, `..adapter..`, or `..config..`. Transactions are applied via `TransactionalUpdateAllBalances` in `org.ikigaidigital.config`.
- **Adapter Isolation**: `..adapter.rest..` and `..adapter.persistence..` must not depend on each other.
- **API Scope**: Expose **only** the two required REST endpoints (`GET /time-deposits` and `POST /time-deposits/balance-updates`). Do not add extra endpoints or Spring Boot Actuator.

---

## 3. Persistence & Data Integrity Rules

- **Database Naming**: Use standard PostgreSQL `snake_case` (`time_deposits`, `withdrawals`, `plan_type`, `time_deposit_id`) managed by Flyway (`V1__schema.sql`), with `spring.jpa.hibernate.ddl-auto=validate`.
- **Never Lose Withdrawals on Balance Update**:
  - `TimeDepositEntity` must not define cascading `@OneToMany` or `orphanRemoval` on withdrawals.
  - `TimeDepositRepository.updateBalances` updates **only** the `balance` column using `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
- **`Double` to `BigDecimal` Conversion in Persistence**:
  - Convert domain `Double` balances to database `NUMERIC(19, 2)` via `BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP)` (never `BigDecimal(double)`).

---

## 4. Development Workflow, TDD & Atomic Commits

- **Legacy Refactoring**: Write characterization tests first (green), then refactor green-to-green.
- **New Features**: Follow TDD (`Red -> Green -> Refactor` within each step) and commit the test and implementation together in a **single atomic commit** so every commit in history passes `./mvnw verify`.
- **Commit Format**: Use Conventional Commits (`feat:`, `refactor:`, `test:`, `build:`, `chore:`, `docs:`).
- **Local Build Environment**:
  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ./mvnw verify
  ```
