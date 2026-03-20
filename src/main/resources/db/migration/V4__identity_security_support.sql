create index idx_household_members_user_id_id on household_members (user_id, id);

update users
set password_hash = '{noop}password'
where id = 1;
