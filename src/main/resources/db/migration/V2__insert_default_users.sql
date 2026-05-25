-- Ensure email is unique for the ON CONFLICT clause
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- Default admin user (password: admin123)
INSERT INTO users (full_name, adress, city, role, email, password, birth_date, user_image_url, payment_status)
VALUES ('Administrador', 'Rua Admin, 123', 'São Paulo', 'ADMIN', 'admin@rotaurbana.com',
        '$2a$12$rMJu04pMIxP2FhyaMxN50Ososp8aVJsd9f76cvKQ.BII76PtY7Rdu',
        '1990-01-01', 'padrao', 'EM_DAY')
ON CONFLICT (email) DO NOTHING;

-- Default driver user (password: driver123)
INSERT INTO users (full_name, adress, city, role, email, password, birth_date, user_image_url, payment_status)
VALUES ('Motorista Padrão', 'Rua do Motorista, 456', 'São Paulo', 'DRIVER', 'driver@rotaurbana.com',
        '$2a$12$Ghc7yD1lyOPe6m9eR7RaL.ZryJI1vh.8w8rg3CUwnEFMDuEQHW6tS',
        '1992-06-15', 'padrao', 'EM_DAY')
ON CONFLICT (email) DO NOTHING;

-- Driver record for the default driver
INSERT INTO driver (licence, fk_user_id)
SELECT '12345678900', id FROM users WHERE email = 'driver@rotaurbana.com'
AND NOT EXISTS (SELECT 1 FROM driver WHERE fk_user_id = (SELECT id FROM users WHERE email = 'driver@rotaurbana.com'));

-- Default passenger user (password: user123)
INSERT INTO users (full_name, adress, city, role, email, password, birth_date, user_image_url, payment_status)
VALUES ('Usuário Padrão', 'Rua do Passageiro, 789', 'São Paulo', 'USER', 'user@rotaurbana.com',
        '$2a$12$ICWRh1HIvmIPWm7kDRG51OZauoRgFGZ3S43lAGNoGFpeayULNus7y',
        '1995-09-20', 'padrao', 'EM_DAY')
ON CONFLICT (email) DO NOTHING;
