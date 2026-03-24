alter table users
    add column credentials_updated_at timestamp with time zone not null default now();

update users
set credentials_updated_at = coalesce(updated_at, created_at, now())
where credentials_updated_at is null;
