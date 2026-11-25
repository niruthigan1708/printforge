-- When the uploaded model file of a closed (rejected or cancelled) request was deleted by the
-- retention job. NULL while the file is still kept.
ALTER TABLE custom_print_request ADD COLUMN file_purged_at timestamp(6) with time zone;
