CREATE TABLE client
(
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name         VARCHAR(255)                            NOT NULL,
    contact_info VARCHAR(255)                            NOT NULL,
    manager_id   BIGINT                                  NOT NULL,
    CONSTRAINT pk_client PRIMARY KEY (id)
);

CREATE TABLE "order"
(
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    client_id   BIGINT                                  NOT NULL,
    manager_id  BIGINT                                  NOT NULL,
    product     VARCHAR(255)                            NOT NULL,
    total_cost  DECIMAL                                 NOT NULL,
    type        VARCHAR(255)                            NOT NULL,
    description VARCHAR(255)                            NOT NULL,

    CONSTRAINT pk_order PRIMARY KEY (id)
);

CREATE TABLE printing
(
    id       BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    papier   VARCHAR(255),
    colour   VARCHAR(255),
    quantity INTEGER                                 NOT NULL,
    order_id BIGINT                                  NOT NULL,
    CONSTRAINT pk_printing PRIMARY KEY (id)
);

CREATE TABLE stamping
(
    id                            BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    quantity                      INTEGER                                 NOT NULL,
    one_ottisk_price              DECIMAL,
    montage_work_price            DECIMAL,
    one_quadrat_metter_foil_price DECIMAL,
    widthsm                       DOUBLE PRECISION                        NOT NULL,
    lengthsm                      DOUBLE PRECISION                        NOT NULL,
    order_id                      BIGINT                                  NOT NULL,
    CONSTRAINT pk_stamping PRIMARY KEY (id)
);

CREATE TABLE "user"
(
    id       BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    username VARCHAR(255)                            NOT NULL,
    password VARCHAR(255)                            NOT NULL,
    email    VARCHAR(255)                            NOT NULL,
    role     VARCHAR(255)                            NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

ALTER TABLE "user"
    ADD CONSTRAINT uc_user_email UNIQUE (email);

ALTER TABLE "user"
    ADD CONSTRAINT uc_user_username UNIQUE (username);

ALTER TABLE client
    ADD CONSTRAINT FK_CLIENT_ON_MANAGER FOREIGN KEY (manager_id) REFERENCES "user" (id);

ALTER TABLE "order"
    ADD CONSTRAINT FK_ORDER_ON_CLIENT FOREIGN KEY (client_id) REFERENCES client (id);

ALTER TABLE "order"
    ADD CONSTRAINT FK_ORDER_ON_MANAGER FOREIGN KEY (manager_id) REFERENCES "user" (id);

ALTER TABLE printing
    ADD CONSTRAINT FK_PRINTING_ON_ORDER FOREIGN KEY (order_id) REFERENCES "order" (id);

ALTER TABLE stamping
    ADD CONSTRAINT FK_STAMPING_ON_ORDER FOREIGN KEY (order_id) REFERENCES "order" (id);