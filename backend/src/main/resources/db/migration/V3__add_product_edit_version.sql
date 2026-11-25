-- Counter bumped by admin edits to a product's details, so a save made from a stale edit form
-- is rejected instead of silently overwriting another admin's changes. Not bumped by orders
-- changing stock (stock has its own expected-value check).
ALTER TABLE product ADD COLUMN edit_version bigint NOT NULL DEFAULT 0;
