-- Add password column to users table
ALTER TABLE public.users
ADD COLUMN password VARCHAR(255);

-- Create roles table
CREATE TABLE public.roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

-- Create user_roles join table
CREATE TABLE public.user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE
);

-- Insert admin role
INSERT INTO public.roles (role_name) VALUES ('admin');
