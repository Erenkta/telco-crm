-- Plans: fixed tariffs owned by order-service in the MVP (moves to product-catalog-service in S9).
-- code is the business key that orders refer to.
CREATE TABLE plans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    monthly_fee  NUMERIC(10, 2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_plans_code        UNIQUE (code),
    CONSTRAINT ck_plans_monthly_fee CHECK (monthly_fee >= 0)
);

-- Orders: customer_id points to customer-service's DB, so it can NOT be a foreign key (DB-per-service).
-- The customer is validated over Feign when the order is created.
CREATE TABLE orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID          NOT NULL,
    plan_code    VARCHAR(50)   NOT NULL REFERENCES plans (code),
    amount       NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      BIGINT        NOT NULL DEFAULT 0, -- Optimistic locking

    CONSTRAINT ck_orders_status CHECK (status IN ('CREATED', 'CANCELLED')),
    CONSTRAINT ck_orders_amount CHECK (amount >= 0)
);

-- GET /orders?customerId= filters on this column; Postgres does not index it automatically.
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

-- Seed tariffs (MVP).
INSERT INTO plans (code, name, monthly_fee) VALUES
    ('BASIC_10GB',     'Basic 10 GB',     199.90),
    ('STANDARD_25GB',  'Standard 25 GB',  299.90),
    ('PREMIUM_50GB',   'Premium 50 GB',   449.90),
    ('UNLIMITED',      'Unlimited',       649.90);
