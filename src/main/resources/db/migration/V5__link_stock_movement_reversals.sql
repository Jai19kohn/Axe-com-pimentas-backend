ALTER TABLE stock_movements
    ADD COLUMN reversal_of_movement_id BIGINT UNIQUE
    REFERENCES stock_movements(id);
