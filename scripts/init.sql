-- Runs once, only when the postgres_data volume is empty (first start or after `docker compose down -v`).
-- Runs as POSTGRES_USER (superuser). Creates one database + one owner user per service (ADR-003, DB-per-service).
-- Tables are NOT created here: each service creates its own schema with Flyway, connecting as its own user.
-- Passwords are local-dev defaults and match the defaults in config-files/<service>/application-dev.properties.

-- identity-service
CREATE USER identity_user WITH PASSWORD 'identity_password';
CREATE DATABASE identity OWNER identity_user;
REVOKE ALL ON DATABASE identity FROM PUBLIC;

-- customer-service
CREATE USER customer_user WITH PASSWORD 'customer_password';
CREATE DATABASE customer OWNER customer_user;
REVOKE ALL ON DATABASE customer FROM PUBLIC;

-- order-service
CREATE USER orders_user WITH PASSWORD 'orders_password';
CREATE DATABASE orders OWNER orders_user;
REVOKE ALL ON DATABASE orders FROM PUBLIC;
