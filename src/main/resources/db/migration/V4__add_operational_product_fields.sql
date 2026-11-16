ALTER TABLE products ADD COLUMN sku VARCHAR(80);
ALTER TABLE products ADD COLUMN barcode VARCHAR(80);
ALTER TABLE products ADD COLUMN supplier VARCHAR(120);
ALTER TABLE products ADD COLUMN physical_location VARCHAR(120);
ALTER TABLE products ADD COLUMN purchase_cost NUMERIC(10, 2);
ALTER TABLE products ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE products ADD COLUMN variation_color VARCHAR(80);
ALTER TABLE products ADD COLUMN variation_size VARCHAR(80);
ALTER TABLE products ADD COLUMN variation_model VARCHAR(80);

UPDATE products SET sku = 'AXE-LEGACY-' || id WHERE sku IS NULL;

ALTER TABLE products ALTER COLUMN sku SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT uk_products_sku UNIQUE (sku);
ALTER TABLE products ADD CONSTRAINT uk_products_barcode UNIQUE (barcode);
