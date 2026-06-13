CREATE TABLE orders (
    id            UUID PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    order_date    TIMESTAMP    NOT NULL,
    status        VARCHAR(32)  NOT NULL
);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    price        NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_customer_name ON orders (customer_name);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
