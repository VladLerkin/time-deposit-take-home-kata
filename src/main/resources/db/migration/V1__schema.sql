-- README: timeDeposits(id, planType, days, balance) -> time_deposits; camelCase -> snake_case (PostgreSQL/Hibernate convention)
CREATE TABLE time_deposits (
    id        INTEGER        PRIMARY KEY,
    plan_type VARCHAR(50)    NOT NULL,
    days      INTEGER        NOT NULL,
    balance   NUMERIC(19, 2) NOT NULL
);

-- README: withdrawals(id, timeDepositId, amount, date) -> withdrawals
CREATE TABLE withdrawals (
    id              INTEGER        PRIMARY KEY,
    time_deposit_id INTEGER        NOT NULL REFERENCES time_deposits (id),
    amount          NUMERIC(19, 2) NOT NULL,
    date            DATE           NOT NULL
);

CREATE INDEX idx_withdrawals_time_deposit_id ON withdrawals (time_deposit_id);
