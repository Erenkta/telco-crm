-- Customers: SINGLE_TABLE inheritance. INDIVIDUAL and CORPORATE rows share one table;
-- customer_type is the JPA discriminator column.
CREATE TABLE customers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_type         VARCHAR(20)  NOT NULL,
    email                 VARCHAR(255) NOT NULL,
    phone_number          VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    kyc_status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    kyc_rejection_reason  VARCHAR(500),
    kyc_status_changed_at TIMESTAMP WITH TIME ZONE,

    -- INDIVIDUAL only
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    national_id           VARCHAR(11),

    -- CORPORATE only
    company_name          VARCHAR(200),
    tax_number            VARCHAR(10),
    tax_office            VARCHAR(100),

    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version               BIGINT NOT NULL DEFAULT 0, -- Optimistic locking + If-Match / ETag

    CONSTRAINT uk_customers_email       UNIQUE (email),
    CONSTRAINT uk_customers_national_id UNIQUE (national_id),
    CONSTRAINT uk_customers_tax_number  UNIQUE (tax_number),

    CONSTRAINT ck_customers_type        CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE')),
    CONSTRAINT ck_customers_status      CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_customers_kyc_status  CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT ck_customers_email_lower CHECK (email = lower(email)),

    -- SINGLE_TABLE cannot make subclass columns NOT NULL, so each type's required columns are enforced here.
    -- The other type's columns must stay empty, so a row can never be half individual, half corporate.
    CONSTRAINT ck_customers_type_fields CHECK (
        (customer_type = 'INDIVIDUAL'
            AND first_name IS NOT NULL AND last_name IS NOT NULL AND national_id IS NOT NULL
            AND company_name IS NULL AND tax_number IS NULL AND tax_office IS NULL)
        OR
        (customer_type = 'CORPORATE'
            AND company_name IS NOT NULL AND tax_number IS NOT NULL AND tax_office IS NOT NULL
            AND first_name IS NULL AND last_name IS NULL AND national_id IS NULL)
    ),
    CONSTRAINT ck_customers_national_id_format CHECK (national_id ~ '^[0-9]{11}$'),
    CONSTRAINT ck_customers_tax_number_format  CHECK (tax_number ~ '^[0-9]{10}$'),
    CONSTRAINT ck_customers_rejection_reason   CHECK (kyc_status <> 'REJECTED' OR kyc_rejection_reason IS NOT NULL)
);

-- Addresses: owned by a customer (1-N). FK lives here, so Address is the owning side in JPA.
CREATE TABLE addresses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    title        VARCHAR(50)  NOT NULL, -- e.g. "Home", "Billing"
    line         VARCHAR(255) NOT NULL,
    district     VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(10),
    country      VARCHAR(2)   NOT NULL DEFAULT 'TR', -- ISO 3166-1 alpha-2
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Documents: metadata only, the file itself lives in external storage (MinIO is in the parking lot).
CREATE TABLE documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    document_type VARCHAR(30)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    storage_key   VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_documents_type CHECK (document_type IN ('ID_CARD', 'PASSPORT', 'TAX_CERTIFICATE', 'SIGNATURE_CIRCULAR', 'OTHER')),
    CONSTRAINT ck_documents_size CHECK (size_bytes > 0)
);

-- Postgres does NOT index foreign keys automatically. Without these, loading a customer's addresses
-- and the ON DELETE CASCADE above would scan the whole child table.
CREATE INDEX idx_addresses_customer_id ON addresses (customer_id);
CREATE INDEX idx_documents_customer_id ON documents (customer_id);
