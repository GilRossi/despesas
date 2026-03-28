alter table users
    add column onboarding_completed boolean not null default false;

alter table users
    add column onboarding_completed_at timestamptz;

alter table users
    add constraint chk_users_onboarding_completion
    check (onboarding_completed = false or onboarding_completed_at is not null);
