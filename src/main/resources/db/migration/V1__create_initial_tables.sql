CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    adress VARCHAR(255),
    city VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    email VARCHAR(255),
    password VARCHAR(255),
    birth_date DATE,
    user_image_url VARCHAR(255),
    payment_status VARCHAR(50) NOT NULL DEFAULT 'EM_DAY'
);

CREATE TABLE driver (
    id_driver BIGSERIAL PRIMARY KEY,
    licence VARCHAR(255),
    fk_user_id BIGINT UNIQUE REFERENCES users(id)
);

CREATE TABLE bus (
    id_bus BIGSERIAL PRIMARY KEY,
    brand VARCHAR(255),
    model VARCHAR(255),
    color VARCHAR(255),
    sign VARCHAR(255),
    mileage DOUBLE PRECISION NOT NULL DEFAULT 0,
    bus_image_url VARCHAR(255),
    code VARCHAR(6) UNIQUE,
    fk_id_driver BIGINT UNIQUE REFERENCES driver(id_driver)
);

CREATE TABLE routes (
    id_route BIGSERIAL PRIMARY KEY,
    destination_time TIME,
    departure_time TIME,
    destiny VARCHAR(255),
    departure_point VARCHAR(255),
    departure_address VARCHAR(255),
    destination_address VARCHAR(255),
    estimated_duration VARCHAR(255),
    departure_latitude DOUBLE PRECISION,
    departure_longitude DOUBLE PRECISION,
    destination_latitude DOUBLE PRECISION,
    destination_longitude DOUBLE PRECISION,
    code VARCHAR(6) UNIQUE,
    fk_id_bus BIGINT REFERENCES bus(id_bus)
);

CREATE TABLE presence (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    route_id BIGINT NOT NULL REFERENCES routes(id),
    presence_date DATE,
    presence_type VARCHAR(255)
);

CREATE TABLE bus_passengers (
    bus_id BIGINT NOT NULL REFERENCES bus(id_bus),
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (bus_id, user_id)
);

CREATE TABLE route_passengers (
    route_id BIGINT NOT NULL REFERENCES routes(id_route),
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (route_id, user_id)
);
