CREATE TABLE trades (
    id UUID PRIMARY KEY,
    buy_order_id UUID NOT NULL REFERENCES orders(id),
    sell_order_id UUID NOT NULL REFERENCES orders(id),
    price DECIMAL(18, 8) NOT NULL,
    quantity DECIMAL(18, 8) NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trades_buy_order_id ON trades(buy_order_id);
CREATE INDEX idx_trades_sell_order_id ON trades(sell_order_id);