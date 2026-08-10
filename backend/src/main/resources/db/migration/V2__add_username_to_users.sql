-- Flyway migration V2
-- Añadir columna username (CITEXT) de forma idempotente

-- 1. Activar la extensión citext si no está activa
CREATE EXTENSION IF NOT EXISTS citext;

-- 2. Añadir la columna a la tabla users como CITEXT
-- Usamos IF NOT EXISTS para que no falle si la columna ya fue añadida manualmente.
ALTER TABLE users ADD COLUMN IF NOT EXISTS username CITEXT;

-- 3. Añadir un índice único sobre username
-- Se usa CREATE UNIQUE INDEX IF NOT EXISTS en lugar de ADD CONSTRAINT UNIQUE
-- ya que ADD CONSTRAINT no soporta IF NOT EXISTS de forma directa.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username ON users (username);

-- 4. Hacer la columna obligatoria (NOT NULL)
-- Este comando es seguro de repetir si la columna ya es NOT NULL en la BD real.
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
