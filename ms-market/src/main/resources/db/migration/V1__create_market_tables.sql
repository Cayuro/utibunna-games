-- ============================================================
-- V1 - Tablas del Marketplace (ms-market)
-- NOTA: sellers.user_id referencia users.id de ms-auth.
--       No ponemos FK cross-service; la integridad la maneja
--       la lógica de negocio (ms-auth valida el JWT).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Vendedores ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sellers (
    seller_id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL UNIQUE,              -- ID del user en ms-auth (sin FK)
    mercado_pago_user_id        VARCHAR(120) UNIQUE,
    mercado_pago_account_id     VARCHAR(120) UNIQUE,
    mercado_pago_access_token   TEXT,
    mercado_pago_refresh_token  TEXT,
    mercado_pago_public_key     TEXT,
    mercado_pago_expires_at     TIMESTAMP,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sellers_user_id ON sellers(user_id);

-- ── Productos ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id   UUID NOT NULL REFERENCES sellers(seller_id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    img_url     TEXT,
    description TEXT NOT NULL,
    stock       INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_seller_id ON products(seller_id);

-- ── Ventas / Órdenes ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sales (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id          UUID NOT NULL,                               -- ID del user en ms-auth (sin FK)
    product_id        UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity          INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price        NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price       NUMERIC(12,2) NOT NULL CHECK (total_price >= 0),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',      -- PENDING | PAID | REJECTED | CANCELLED
    payment_reference VARCHAR(150),                                -- ID del pago en MercadoPago
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sales_buyer_id   ON sales(buyer_id);
CREATE INDEX IF NOT EXISTS idx_sales_product_id ON sales(product_id);
