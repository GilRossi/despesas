alter table users
    add column platform_role varchar(32) not null default 'STANDARD_USER';

alter table users
    add constraint chk_users_platform_role
    check (platform_role in ('STANDARD_USER', 'PLATFORM_ADMIN'));
