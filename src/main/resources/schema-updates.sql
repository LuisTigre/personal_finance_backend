-- Add new columns to transactions table
ALTER TABLE transactions 
ADD COLUMN merchant VARCHAR(120) NULL,
ADD COLUMN is_itemized BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for is_itemized
CREATE INDEX idx_transactions_is_itemized ON transactions(is_itemized);

-- Create transaction_items table
CREATE TABLE transaction_items (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    amount NUMERIC(38,2) NOT NULL,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_transaction_items_transaction_id FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE CASCADE
);

-- Create index for transaction_id in items table
CREATE INDEX idx_transaction_items_transaction_id ON transaction_items(transaction_id);
