-- 1. UUID Üretimi için Gerekli Eklenti (PostgreSQL 13 öncesi için fallback, 13+ dahili gen_random_uuid kullanır)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Kullanıcılar Tablosu (Users)
CREATE TABLE users (
                       id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       customer_id       UUID NULL, -- CRM tarafındaki Müşteri Varlığı (Customer Entity) ile ilişki
                       username          VARCHAR(100) NOT NULL UNIQUE,
                       email             VARCHAR(255) NOT NULL UNIQUE,
                       full_name         VARCHAR(150) NOT NULL,
                       phone_number      VARCHAR(20),
                       status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, BLOCKED
                       keycloak_user_id  VARCHAR(255) UNIQUE, -- Keycloak tarafındaki Sub / User ID eşleşmesi
                       created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       version           BIGINT NOT NULL DEFAULT 0 -- Optimistic Locking
);

-- 3. Rol Yönetimi (Roles)
CREATE TABLE roles (
                       id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name         VARCHAR(50) NOT NULL UNIQUE, -- E.g. ROLE_CUSTOMER, ROLE_ADMIN, ROLE_FIELD_DEALER
                       description  VARCHAR(255),
                       created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Yetki Yönetimi (Permissions)
CREATE TABLE permissions (
                             id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             name         VARCHAR(100) NOT NULL UNIQUE, -- E.g. USER_READ, USER_WRITE, BILL_VIEW
                             description  VARCHAR(255),
                             created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Kullanıcı - Rol İlişkisi (User-Role Cross Table)
CREATE TABLE user_roles (
                            id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id      UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            assigned_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            assigned_by  VARCHAR(100) NOT NULL,
                            CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- 6. Rol - Yetki İlişkisi (Role-Permission Cross Table)
CREATE TABLE role_permissions (
                                  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  role_id        UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id  UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                  assigned_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  assigned_by    VARCHAR(100) NOT NULL,
                                  CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 7. Audit Logları (Identity Audit Logs)
CREATE TABLE identity_audit_logs (
                                     id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     entity_type   VARCHAR(50) NOT NULL, -- USER, ROLE, PERMISSION
                                     entity_id     UUID NOT NULL,
                                     action        VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN_SUCCESS, PASSWORD_CHANGE
                                     detail        VARCHAR(500),
                                     performed_by  VARCHAR(100) NOT NULL,
                                     created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Transactional Outbox Tablosu (Eventual Consistency & Event-Driven Architecture)
CREATE TABLE outbox (
                        id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        aggregate_type  VARCHAR(100) NOT NULL, -- E.g. USER_EVENT
                        aggregate_id    VARCHAR(255) NOT NULL, -- E.g. User ID
                        topic           VARCHAR(100) NOT NULL, -- Kafka / RabbitMQ Topic (E.g. identity.user-created)
                        payload         TEXT NOT NULL,         -- Event JSON verisi
                        created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Processed Events (Idempotency için Tüketilen Event Kaydı)
CREATE TABLE processed_events (
                                  event_id      UUID PRIMARY KEY,
                                  processed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak ON users(keycloak_user_id);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);


-- Temel Rol Yapılandırması (Telco CRM Aktörleri)
INSERT INTO roles (id, name, description) VALUES
                                              (gen_random_uuid(), 'ROLE_CUSTOMER', 'Kendi hesabını, faturalarını ve taleplerini self-servis olarak yönetir'),
                                              (gen_random_uuid(), 'ROLE_CALL_CENTER_AGENT', 'Müşteri taleplerini karşılar, sipariş ve destek talebi oluşturur'),
                                              (gen_random_uuid(), 'ROLE_FIELD_DEALER', 'Sahada yeni müşteri kaydı ve sipariş oluşturma işlemlerini yürütür'),
                                              (gen_random_uuid(), 'ROLE_MARKETING_MANAGER', 'Kampanya ve ürün kataloğu yönetimi yapar'),
                                              (gen_random_uuid(), 'ROLE_SYSTEM_ADMIN', 'Sistem genelinde kullanıcı, rol ve yetki yönetimini yürütür'),
                                              (gen_random_uuid(), 'ROLE_BILLING_OPERATOR', 'Fatura oluşturma, düzeltme ve tahsilat işlemlerini yönetir'),
                                              (gen_random_uuid(), 'ROLE_SYSTEM_SERVICE', 'Servisler arası otomatik (service-to-service) işlemleri gerçekleştirir');