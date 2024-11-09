CREATE TABLE managers
(
    id       SERIAL PRIMARY KEY,
    name     VARCHAR(50)         NOT NULL,
    email    VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE  NOT NULL,
    password VARCHAR(16)         NOT NULL
);

CREATE TABLE clients
(
    id         SERIAL PRIMARY KEY,
    manager_id INTEGER             REFERENCES managers (id) ON DELETE SET NULL,
    name       VARCHAR(255)        NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    phone      VARCHAR(20),
    username   VARCHAR(255)        NOT NULL,
    password   VARCHAR(255)        NOT NULL
);

CREATE TABLE orders
(
    id                 SERIAL PRIMARY KEY,
    client_id          INTEGER REFERENCES clients (id) ON DELETE CASCADE,
    created_order_date TIMESTAMP,
    total_price        DECIMAL(15, 2) NOT NULL
);

CREATE TABLE products
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255),
    price       DECIMAL(15, 2),
    description TEXT,
    image       VARCHAR(255),
    category    VARCHAR(255)
);

CREATE TABLE printing
(
    id       SERIAL PRIMARY KEY,
    papier   VARCHAR(255) NOT NULL,
    colour   VARCHAR(50)  NOT NULL,
    quantity INTEGER      NOT NULL,
    order_id INTEGER REFERENCES orders (id) ON DELETE CASCADE
);

CREATE TABLE stamping
(
    id                            SERIAL PRIMARY KEY,
    product_id                    INTEGER          REFERENCES products (id) ON DELETE SET NULL,
    quantity                      INTEGER          NOT NULL,
    one_ottisk_price              DECIMAL(15, 2)   NOT NULL,
    montage_work_price            DECIMAL(15, 2)   NOT NULL,
    one_quadrat_metter_foil_price DECIMAL(15, 2)   NOT NULL,
    width_sm                      DOUBLE PRECISION NOT NULL,
    length_sm                     DOUBLE PRECISION NOT NULL,
    order_id                      INTEGER REFERENCES orders (id) ON DELETE CASCADE
);