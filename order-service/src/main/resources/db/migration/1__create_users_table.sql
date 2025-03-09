-- src/main/resources/db/migration/V1__create_users_table.sql
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE users_order_ids (
    user_id VARCHAR(255) NOT NULL REFERENCES users(id),
    order_ids VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, order_ids)
);

-- Ajouter quelques utilisateurs de test (optionnel)
INSERT INTO users (id, name, email) VALUES
    ('1', 'John Doe', 'john.doe@example.com'),
    ('2', 'Jane Smith', 'jane.smith@example.com');