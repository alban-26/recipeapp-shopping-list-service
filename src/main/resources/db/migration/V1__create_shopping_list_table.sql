-- Shopping list table
CREATE TABLE IF NOT EXISTS shopping_list (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at DATE NOT NULL,
    user_id VARCHAR NOT NULL,
    product_order VARCHAR NOT NULL
);

-- Shopping item catalog table
CREATE TABLE IF NOT EXISTS shopping_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    product_category VARCHAR(255) NOT NULL
);

-- Association table: entries inside a shopping list
CREATE TABLE IF NOT EXISTS shopping_list_entry (
    id BIGSERIAL PRIMARY KEY,  -- optional; could also use composite (shopping_list_id, shopping_item_id)
    shopping_list_id BIGINT NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
    shopping_item_id BIGINT NOT NULL REFERENCES shopping_item(id) ON DELETE CASCADE,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    rank INTEGER NOT NULL
);

-- Indexes for faster lookups
CREATE INDEX idx_shopping_list_entry_list_id ON shopping_list_entry(shopping_list_id);
CREATE INDEX idx_shopping_list_entry_item_id ON shopping_list_entry(shopping_item_id);
