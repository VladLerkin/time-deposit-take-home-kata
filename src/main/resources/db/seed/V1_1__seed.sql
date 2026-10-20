-- Sample seed data loaded only when running with the 'local' Spring profile
INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic', 45, 1200.00),
    (2, 'student', 100, 2400.00),
    (3, 'premium', 60, 6000.00),
    (4, 'basic', 20, 500.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 1, 100.00, '2026-01-15'),
    (2, 1, 50.00, '2026-02-10'),
    (3, 2, 200.00, '2026-03-01')
ON CONFLICT (id) DO NOTHING;
