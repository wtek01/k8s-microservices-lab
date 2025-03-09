-- src/main/resources/db/migration/V1__create_orders_table.sql
CREATE TABLE orders (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'CREATED'
);

-- Index pour améliorer les recherches par user_id
CREATE INDEX idx_orders_user_id ON orders(user_id);