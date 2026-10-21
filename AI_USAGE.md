# AI-Assisted Development Workflow (`AI_USAGE.md`)

This document describes the AI harness, tools, custom rules, prompts, and spec-driven workflow used to design, review, implement, and verify the XA Bank Time Deposit Refactoring Kata.

---

## 1. Tools & Agentic Setup

| Component | Tool / Configuration | Role in Workflow |
| --- | --- | --- |
| **Agentic Coding Environment** | **Google Antigravity IDE** (Agentic AI pair-programming harness) | Primary execution harness with workspace file tools, terminal execution, and persistent project rules |
| **Planning & Cross-Review LLMs** | **Claude Opus 5.5** & **Gemini 4.0 Argon** | Cross-LLM architectural planning, adversarial plan review (`v1 → v2 → v3 → v4`), and TDD implementation |
| **Repository Guardrails** | [`AGENTS.md`](AGENTS.md) & [`.agents/rules/project-rules.md`](.agents/rules/project-rules.md) | Always-on workspace rules automatically injected into the agent's system context |
| **Executable Specification** | [`docs/implementation_plan.md`](docs/implementation_plan.md) | 25-commit atomic implementation plan with a requirements traceability matrix and changelog of review iterations |
| **Automated Verification** | `./mvnw verify` (JUnit 5, Testcontainers PostgreSQL 16, ArchUnit) | Deterministic feedback gate run before every atomic commit |

---

## 2. Reproducible AI Harness Configuration

To make the AI setup practical and reproducible across any developer machine or agentic IDE:

1. **Always-On Workspace Rules ([`.agents/rules/project-rules.md`](.agents/rules/project-rules.md) and [`AGENTS.md`](AGENTS.md))**:
   - Automatically loaded into the agent context on every turn.
   - Enforces non-negotiable constraints:
     - **Backward compatibility**: `TimeDeposit` and `TimeDepositCalculator` must remain in package `org.ikigaidigital` with unchanged constructor and `updateBalance(xs: List<TimeDeposit>)` signatures, exact `BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).toDouble()` rounding, and Java 17 bytecode target.
     - **Hexagonal Architecture boundaries**: zero Spring or Jakarta EE imports in `org.ikigaidigital` (excluding `Application`) and `org.ikigaidigital.domain..` / `org.ikigaidigital.application..`.
     - **Data integrity**: never cascade-delete or orphan-remove `withdrawals` when updating `time_deposits.balance`; convert `Double` to `BigDecimal` in persistence via `BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP)`.
     - **Atomic TDD commits**: every commit must bundle its test and implementation together and pass `./mvnw verify`.

2. **Spec-Driven Execution ([`docs/implementation_plan.md`](docs/implementation_plan.md))**:
   - Rather than prompting the AI ad-hoc ("build the whole app"), development was divided into two phases:
     1. **Iterative Spec & Adversarial Cross-LLM Review (`v1 → v2 → v3 → v4`)**
     2. **Step-by-Step Atomic Execution (25 commits, each verified by `./mvnw verify`)**

---

## 3. Iterative Plan Refinement & Prompts Used

### Stage 1: Initial Architectural Plan & Cross-LLM Reviews (`v1 → v4`)

Before writing application code, the implementation plan underwent three rounds of adversarial review between LLMs to catch subtle build, framework, and compatibility issues upfront:

- **`v1 → v2`**:
  - Identified that moving `TimeDeposit` or `TimeDepositCalculator` into subpackages would break external evaluation suites importing `org.ikigaidigital.*`.
  - Identified the `BigDecimal(Double)` vs `BigDecimal.valueOf(Double)` IEEE 754 behavior and locked the exact legacy `BigDecimal(double)` + `HALF_UP` rounding with characterization tests (e.g. the `x.xx5` boundary `6.00 * 0.01 / 12 = 0.005 → 0.01`).
  - Replaced entity-graph `saveAll` on balance updates with a targeted `@Modifying` query so existing `withdrawals` can never be accidentally wiped.
- **`v2 → v3`**:
  - Resolved OpenAPI generator class-name collision (`TimeDeposit` domain model vs `TimeDepositDto` REST model via `<modelNameSuffix>Dto</modelNameSuffix>`).
  - Placed `openapi.yaml` in `src/main/resources/static/openapi.yaml` as the single source of truth for both `openapi-generator-maven-plugin` and Swagger UI.
  - Added `clearAutomatically = true, flushAutomatically = true` on `@Modifying` to prevent stale Hibernate 1st-level cache reads within the same transaction.
  - Kept `<java.version>17</java.version>` so the project runs on any JDK 17+ evaluator environment.
- **`v3 → v4` (Final Approved Plan)**:
  - Set `<documentationProvider>none</documentationProvider>` and `<useBeanValidation>false</useBeanValidation>` in `openapi-generator-maven-plugin` so generated interfaces compile cleanly without extra validation/annotation dependencies.
  - Made inbound use-case ports Kotlin `fun interface` types (`UpdateAllBalancesUseCase`, `GetAllTimeDepositsUseCase`) so `TransactionalUpdateAllBalances` can decorate `UpdateAllBalancesService` without fighting Kotlin's `final`-by-default classes.
  - Placed `Application.kt` in root package `org.ikigaidigital` for seamless `@SpringBootApplication` component scanning while explicitly excluding `Application` in the root-package ArchUnit rule.

### Representative Prompts Used

- **Adversarial Review Prompt**:
  > *"Review version N of the implementation plan against the README requirements and existing codebase. Check specifically for hidden build failures, Kotlin + Spring Boot + OpenAPI generator pitfalls, JPA/Hibernate caching or cascade issues, and any violation of README constraints."*
- **Harness Initialization Prompt**:
  > *"Commit the approved English implementation plan (`docs/implementation_plan.md`) and agent rules (`AGENTS.md`, `.agents/rules/project-rules.md`) as the first commit, then execute the plan strictly step-by-step from Commit #2 to Commit #25, running `./mvnw verify` before every commit."*

---

## 4. Summary of AI-Assisted Parts & Rationale

| Phase / Component | Commits | How AI Was Used | Why AI Was Effective Here |
| --- | --- | --- | --- |
| **1. AI Harness & Build Modernization** | `#1–#4` | Created `AGENTS.md`, `.agents/rules/project-rules.md`, `docs/implementation_plan.md`, `.gitignore`, Maven Wrapper (`mvnw`), and upgraded Kotlin to `2.0.21` / Surefire to `3.5.3` targeting Java 17 bytecode. | Automates boilerplate build setup and establishes persistent guardrails before code changes begin. |
| **2. Legacy Characterization & Domain Refactoring** | `#5–#8` | Generated 40 parameterized & edge-case characterization tests for `TimeDepositCalculator`, extracted rounding helper, and introduced composable `EligibilityRule`, `InterestPolicy`, and `InterestPolicyRegistry`. | Exhaustively covers boundary days (`0, 29, 30, 31, 45, 46, 365, 366, 1000` × every plan) and IEEE 754 rounding edge cases to guarantee zero regression during refactoring. |
| **3. Spring Boot, Domain Models & Use Cases** | `#9–#12` | Bootstrapped Spring Boot 3.5 + Testcontainers, added `Withdrawal`, `TimeDepositView`, repository port interfaces, and pure Kotlin use case services (`UpdateAllBalancesService`, `GetAllTimeDepositsService`) with unit tests. | Keeps application logic 100% framework-free and rapidly scaffolds unit tests with in-memory fake repositories. |
| **4. Database Schema, JPA Adapter & Seed Data** | `#13–#16` | Authored Flyway `V1__schema.sql`, JPA entities/repositories, 2-query read strategy in `TimeDepositPersistenceAdapter`, local seed data `V1_1__seed.sql`, `@DataJpaTest` with Testcontainers PostgreSQL, and `TransactionalUpdateAllBalances` decorator. | Quickly verifies real SQL/JPA behavior (including same-transaction cache eviction and withdrawal preservation) against a live PostgreSQL container. |
| **5. Contract-First OpenAPI & REST Adapter** | `#17–#20` | Authored `static/openapi.yaml`, configured `openapi-generator-maven-plugin`, implemented `TimeDepositController` and `TimeDepositRestMapper`, added `@WebMvcTest` slice tests, and configured Swagger UI. | Eliminates manual DTO/controller drift by generating the API contract types directly from `openapi.yaml`. |
| **6. E2E Verification, ArchUnit & Documentation** | `#21–#25` | Created `TimeDepositEndToEndTest`, `HexagonalArchitectureTest` (ArchUnit), `docker-compose.yml`, `README.md` run guide, and `AI_USAGE.md`. | Proves end-to-end correctness and architectural purity via automated tests rather than manual inspection alone. |

---

## 5. Human Oversight & Verification

While the AI agent generated the code, tests, and documentation across the 25 commits, human engineering judgment directed:
- **Architectural trade-offs** (e.g., choosing a 2-query read strategy over JPA `@OneToMany` to guarantee `withdrawals` cannot be mutated or deleted by balance updates).
- **Strict scope discipline** (rejecting unnecessary endpoints, Spring Boot Actuator, or custom exception handlers to honor the kata's *"exactly two API endpoints"* and *"handling invalid input or exceptions is not required"* instructions).
- **Multi-stage plan verification** before writing code, ensuring every commit in `git log --oneline` is atomic, self-contained, and passes `./mvnw verify`.
