CREATE TABLE accounts
(
    id       BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name     VARCHAR(255)                            NOT NULL,
    username VARCHAR(255)                            NOT NULL,
    password VARCHAR(255)                            NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE account_roles
(
    account_id BIGINT       NOT NULL,
    roles      VARCHAR(255) NOT NULL
);


CREATE TABLE customer
(
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    username         VARCHAR(255)                            NOT NULL,
    phone_number     VARCHAR(255)                            NOT NULL,
    email            VARCHAR(255)                            NOT NULL,
    manager_username VARCHAR(255),
    account_id       BIGINT                                  NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (id)
);

CREATE TABLE "order"
(
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    customer_id     BIGINT                                  NOT NULL,
    manager_id      BIGINT                                  NOT NULL,
    product         VARCHAR(255)                            NOT NULL,
    postpress_price DECIMAL                                 NOT NULL,
    total_cost      DECIMAL                                 NOT NULL,
    CONSTRAINT pk_order PRIMARY KEY (id)
);

CREATE TABLE post_press
(
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    customer_id BIGINT,
    quantity    INTEGER                                 NOT NULL,
    widthsm     DOUBLE PRECISION                        NOT NULL,
    lengthsm    DOUBLE PRECISION                        NOT NULL,
    total_price DECIMAL,
    CONSTRAINT pk_postpress PRIMARY KEY (id)
);

CREATE TABLE printing
(
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    customer_id BIGINT,
    papier      VARCHAR(255),
    colour      VARCHAR(255),
    quantity    INTEGER,
    total_cost  DOUBLE PRECISION                        NOT NULL,
    CONSTRAINT pk_printing PRIMARY KEY (id)
);

CREATE TABLE setting
(
    id                            BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    montage_work_price            DOUBLE PRECISION                        NOT NULL,
    one_ottisk_price              DOUBLE PRECISION                        NOT NULL,
    one_quadrat_metter_foil_price DOUBLE PRECISION                        NOT NULL,
    price_print                   DOUBLE PRECISION                        NOT NULL,
    CONSTRAINT pk_setting PRIMARY KEY (id)
);


ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_name UNIQUE (name);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_username UNIQUE (username);

ALTER TABLE customer
    ADD CONSTRAINT uc_customer_email UNIQUE (email);

ALTER TABLE customer
    ADD CONSTRAINT uc_customer_phonenumber UNIQUE (phone_number);

ALTER TABLE customer
    ADD CONSTRAINT uc_customer_username UNIQUE (username);

ALTER TABLE customer
    ADD CONSTRAINT FK_CUSTOMER_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE "order"
    ADD CONSTRAINT FK_ORDER_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES customer (id);

ALTER TABLE "order"
    ADD CONSTRAINT FK_ORDER_ON_MANAGER FOREIGN KEY (manager_id) REFERENCES accounts (id);

ALTER TABLE post_press
    ADD CONSTRAINT FK_POSTPRESS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES customer (id);

ALTER TABLE printing
    ADD CONSTRAINT FK_PRINTING_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES customer (id);

ALTER TABLE account_roles
    ADD CONSTRAINT fk_account_roles_on_account FOREIGN KEY (account_id) REFERENCES accounts (id);
