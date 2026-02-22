-- Insert test shopping lists
INSERT INTO shopping_list (title, created_at, user_id, product_order)
VALUES ('Test Shopping List', CURRENT_DATE, 1, 'STANDARD');

INSERT INTO shopping_list (title, created_at, user_id, product_order)
VALUES ('To Delete', CURRENT_DATE, 1, 'COMMON_ORDER');

-- Insert more catalog items (generic product definitions)
INSERT INTO shopping_item (name, product_category)
VALUES
  ('Eggs', 'DAIRY'),
  ('Flour', 'BAKERY'),
  ('Milk', 'DAIRY'),
  ('Bread', 'BAKERY'),
  ('Butter', 'DAIRY'),
  ('Sugar', 'BAKERY'),
  ('Salt', 'SPICES'),
  ('Pepper', 'SPICES');

-- Link shopping items to shopping lists with quantities etc.
-- Attach items to Test Shopping List (id = 1)
INSERT INTO shopping_list_entry (shopping_list_id, shopping_item_id, quantity, unit, checked, rank)
VALUES
  (1, 1, 12, 'PIECE', false, 3),  -- Eggs
  (1, 3, 2, 'LITER', false, 2),     -- Milk
  (1, 5, 250, 'GRAM', false, 1);   -- Butter

-- Attach items to To Delete list (id = 2)
INSERT INTO shopping_list_entry (shopping_list_id, shopping_item_id, quantity, unit, checked, rank)
VALUES
  (2, 2, 1, 'KILOGRAM', false, 1),    -- Flour
  (2, 4, 1, 'LEAF', false, 2),  -- Bread
  (2, 6, 500, 'GRAM', false, 3);   -- Sugar
