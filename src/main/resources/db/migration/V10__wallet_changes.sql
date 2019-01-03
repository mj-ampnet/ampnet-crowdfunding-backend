DROP TABLE transaction;

ALTER TABLE wallet DROP COLUMN owner_id;
ALTER TABLE wallet ADD COLUMN address VARCHAR(42) UNIQUE NOT NULL;
ALTER TABLE wallet ADD COLUMN type VARCHAR(8) NOT NULL;

ALTER TABLE app_user ADD COLUMN wallet_id INT REFERENCES wallet(id);
