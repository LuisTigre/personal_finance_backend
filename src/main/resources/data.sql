-- Update existing users to have default values for new columns
UPDATE users SET active = true WHERE active IS NULL;
UPDATE users SET created_at = current_timestamp WHERE created_at IS NULL;
UPDATE users SET first_name = 'Default' WHERE first_name IS NULL OR first_name = '';
UPDATE users SET last_name = 'User' WHERE last_name IS NULL OR last_name = '';

