-- Per-user counter embedded in issued JWTs. A token is only accepted while its version matches
-- the user's current one, so incrementing it (logout) revokes every outstanding token at once.
ALTER TABLE users ADD COLUMN token_version integer NOT NULL DEFAULT 0;
