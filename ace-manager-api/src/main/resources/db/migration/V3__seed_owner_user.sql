INSERT INTO app_users (id, email, password, role)
VALUES (
    gen_random_uuid(),
    'lucasmelofavaretto@hotmail.com',
    '$2a$10$SQohiv.7J2wZ.32Ccq6ZB.ITb5w3ft9IVbajo.VY8e6vAAC6kY9cC',
    'ROLE_OWNER'
)
ON CONFLICT (email) DO NOTHING;
