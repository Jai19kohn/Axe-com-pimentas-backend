DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_finished_product_inventory_quantity'
          AND conrelid = 'public.finished_product_inventory'::regclass
    ) THEN
        ALTER TABLE public.finished_product_inventory
            ADD CONSTRAINT ck_finished_product_inventory_quantity CHECK (quantity >= 0);
    END IF;
END
$$;