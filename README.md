# Time Deposit Refactoring Kata - Take-Home Assignment

## XA Bank Time Deposit

### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.

---

## Solution Overview & How to Run

### Prerequisites

- **JDK 17+** (targets Java 17 bytecode; tested on JDK 17 and JDK 21)
- **Docker** (required for running Testcontainers during `./mvnw verify` and for local PostgreSQL via `docker compose`)
- Maven Wrapper (`./mvnw`) is included in the repository (no global Maven installation required)

---

### 1. Run All Tests & Architecture Checks

With Docker running, execute:

```bash
./mvnw verify
```

This runs:
- Unit & characterization tests for `TimeDepositCalculator`, `InterestPolicy`, `EligibilityRule`, and application use cases
- `@WebMvcTest` controller slice tests against the OpenAPI-generated contract
- `@DataJpaTest` and `@SpringBootTest` integration tests with a real **PostgreSQL 16** container managed by **Testcontainers** and **Flyway**
- End-to-end API workflow tests (`POST /time-deposits/balance-updates` $\rightarrow$ `GET /time-deposits`)
- **ArchUnit** hexagonal architecture boundary verification

---

### 2. Run the Application Locally

1. **Start PostgreSQL via Docker Compose:**

   ```bash
   docker compose up -d
   ```

2. **Start the Spring Boot application with the `local` profile:**

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

   The `local` profile connects to `jdbc:postgresql://localhost:5432/timedeposit` and automatically runs both the schema migration (`src/main/resources/db/migration/V1__schema.sql`) and sample seed data (`src/main/resources/db/seed/V1_1__seed.sql`) via Flyway.

---

### 3. How to Trigger the Endpoints via Swagger UI

Once the application is running on port `8080`, open the Swagger UI in your browser:

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Static OpenAPI 3.0 Contract:** [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)

#### Step-by-Step Guide in Swagger UI

1. **Retrieve all time deposits (`GET /time-deposits`):**
   - Expand **`GET /time-deposits`** under the **`time-deposits`** tag.
   - Click **"Try it out"**, then click **"Execute"**.
   - Observe the **`200 OK`** response containing the seeded time deposits (`basic`, `student`, `premium`) along with their nested `withdrawals` list.
2. **Update all time deposit balances (`POST /time-deposits/balance-updates`):**
   - Expand **`POST /time-deposits/balance-updates`**.
   - Click **"Try it out"**, then click **"Execute"**.
   - Observe the **`204 No Content`** response indicating that monthly interest has been calculated and persisted for all eligible time deposits.
3. **Verify updated balances (`GET /time-deposits`):**
   - Execute **`GET /time-deposits`** again.
   - Observe that balances for eligible deposits have increased by one month's interest while all associated `withdrawals` remain intact.

#### Equivalent `curl` Commands

```bash
# 1. Retrieve all time deposits
curl -i http://localhost:8080/time-deposits

# 2. Trigger balance update for all time deposits
curl -i -X POST http://localhost:8080/time-deposits/balance-updates

# 3. Retrieve all time deposits again to inspect updated balances
curl -i http://localhost:8080/time-deposits
```

---

### 4. Architecture & Design Highlights

- **Hexagonal Architecture (Ports & Adapters)** enforced by `HexagonalArchitectureTest` (ArchUnit):
  - `org.ikigaidigital` & `org.ikigaidigital.domain`: Pure domain layer with zero framework dependencies. Preserves `TimeDeposit` and `TimeDepositCalculator` in the root `org.ikigaidigital` package with identical public signatures and IEEE 754 `Double` rounding semantics (`RoundingMode.HALF_UP` to 2 decimal places).
  - `org.ikigaidigital.application`: Framework-free inbound use case ports (`GetAllTimeDepositsUseCase`, `UpdateAllBalancesUseCase`), outbound repository ports (`TimeDepositRepository`, `TimeDepositReadRepository`), and use case services.
  - `org.ikigaidigital.adapter.rest`: Contract-first REST controller implementing the `TimeDepositsApi` interface generated from `src/main/resources/static/openapi.yaml` via `openapi-generator-maven-plugin`.
  - `org.ikigaidigital.adapter.persistence`: Spring Data JPA adapter using targeted `@Modifying` balance updates and a 2-query read strategy (`time_deposits` + `withdrawals WHERE time_deposit_id IN (...)`) to avoid N+1 queries and ensure withdrawals are never lost during balance updates.
  - `org.ikigaidigital.config`: Spring `@Configuration` wiring and `TransactionalUpdateAllBalances` decorator keeping `@Transactional` out of the `application` layer.
- **Extensible Interest Calculation**: Composable `EligibilityRule` (`afterDays`, `upToDays`, `and`) and `RateInterestPolicy` registered in `InterestPolicyRegistry` allow adding new plan types or tiered rules without modifying existing calculator logic (Open/Closed Principle).

---

### 5. Documented Assumptions

All assumptions are also documented in code/configuration comments:
1. **Endpoint Path & Verb**: `POST /time-deposits/balance-updates` returning `204 No Content` is used for updating all balances because the operation is non-idempotent (each invocation applies one monthly interest accrual without incrementing `days`).
2. **Database Naming**: PostgreSQL tables and columns use standard `snake_case` (`time_deposits`, `withdrawals`, `plan_type`, `time_deposit_id`), mapped to the `camelCase` JSON contract (`planType`, `withdrawals`) in the REST layer.
3. **Withdrawals & Balance Updates**: Withdrawals are historical records associated with a time deposit; they do not alter the monthly interest formula and are preserved untouched across balance updates.
4. **Numeric Precision**: Domain `TimeDeposit.balance` remains `Double` for backward compatibility, while database columns use `NUMERIC(19, 2)` converted via `BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP)`.
5. **Strictly Two Business API Endpoints**: Only `GET /time-deposits` and `POST /time-deposits/balance-updates` are exposed; Swagger UI and `/openapi.yaml` serve the API contract documentation, and Spring Boot Actuator is intentionally omitted.

---

### 6. AI-Assisted Development Documentation

- **[AI_USAGE.md](AI_USAGE.md)**: Detailed documentation of the AI harness, tools, iterative cross-LLM specification workflow (`v1 → v2 → v3 → v4`), and a breakdown of AI-assisted work.
- **[AGENTS.md](AGENTS.md)** & **[.agents/rules/project-rules.md](.agents/rules/project-rules.md)**: Architectural guardrails and TDD rules used by the AI coding agent.
- **[docs/implementation_plan.md](docs/implementation_plan.md)**: The 25-commit specification and architectural plan executed for this repository.
