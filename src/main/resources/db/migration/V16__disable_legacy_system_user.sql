update api_refresh_tokens
set revoked_at = coalesce(revoked_at, now()),
	last_used_at = coalesce(last_used_at, now()),
	revocation_reason = coalesce(revocation_reason, 'USER_STATE_INVALID')
where user_id = 1
	and revoked_at is null;

update household_members
set deleted_at = coalesce(deleted_at, now()),
	updated_at = now()
where user_id = 1
	and deleted_at is null;

update users
set password_hash = '{noop}disabled',
	deleted_at = coalesce(deleted_at, now()),
	updated_at = now()
where id = 1
	and deleted_at is null;
