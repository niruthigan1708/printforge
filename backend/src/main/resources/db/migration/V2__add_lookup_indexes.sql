-- Indexes for the lookups the API performs on every page load.
-- PostgreSQL does not index foreign keys automatically, so "my orders", order items,
-- "products in a category" and the newest-first admin lists all scanned whole tables.

CREATE INDEX IF NOT EXISTS idx_customer_orders_customer_id ON customer_orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_orders_created_at ON customer_orders (created_at);
CREATE INDEX IF NOT EXISTS idx_customer_orders_status ON customer_orders (status);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_id ON order_item (product_id);
CREATE INDEX IF NOT EXISTS idx_custom_print_request_customer_id ON custom_print_request (customer_id);
CREATE INDEX IF NOT EXISTS idx_custom_print_request_created_at ON custom_print_request (created_at);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product (category_id);
CREATE INDEX IF NOT EXISTS idx_product_active_created_at ON product (active, created_at);
