CREATE TABLE IF NOT EXISTS finished_product_inventory (
    product_id BIGINT PRIMARY KEY,
    quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_finished_product_inventory_quantity CHECK (quantity >= 0)
);

DO $$
BEGIN
    IF to_regclass('public.products') IS NOT NULL
            AND EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'products'
                  AND column_name = 'stock'
            ) THEN
        EXECUTE 'INSERT INTO finished_product_inventory (product_id, quantity)
                 SELECT id, stock FROM products
                 ON CONFLICT (product_id) DO NOTHING';
    END IF;
END
$$;

ALTER TABLE IF EXISTS products DROP COLUMN IF EXISTS stock;