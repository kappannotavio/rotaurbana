-- Ensure email is unique for the ON CONFLICT clause
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- Default admin user (password: admin123)
INSERT INTO users (full_name, adress, city, role, email, password, birth_date, user_image_url, payment_status)
VALUES ('Administrador', 'Rua Admin, 123', 'São Paulo', 'ADMIN', 'admin@rotaurbana.com',
        '$2a$12$rMJu04pMIxP2FhyaMxN50Ososp8aVJsd9f76cvKQ.BII76PtY7Rdu',
        '1990-01-01', 'padrao', 'EM_DAY')
ON CONFLICT (email) DO NOTHING;

-- Default passenger user (password: user123)
INSERT INTO users (full_name, adress, city, role, email, password, birth_date, user_image_url, payment_status)
VALUES ('Usuário Padrão', 'Rua do Passageiro, 789', 'São Paulo', 'USER', 'user@rotaurbana.com',
        '$2a$12$ICWRh1HIvmIPWm7kDRG51OZauoRgFGZ3S43lAGNoGFpeayULNus7y',
        '1995-09-20', 'padrao', 'EM_DAY')
ON CONFLICT (email) DO NOTHING;
