CREATE TABLE app_user (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          password_hash VARCHAR(255) NOT NULL,
                          role VARCHAR(30) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL
);

ALTER TABLE person
    ADD COLUMN user_id BIGINT;

ALTER TABLE person
    ADD CONSTRAINT uk_person_user
        UNIQUE (user_id);

ALTER TABLE person
    ADD CONSTRAINT fk_person_user
        FOREIGN KEY (user_id)
            REFERENCES app_user(id);