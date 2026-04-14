create table household_modules (
    id bigserial primary key,
    household_id bigint not null references households (id) on delete restrict,
    module_key varchar(64) not null,
    enabled boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index uq_household_modules_household_module
    on household_modules (household_id, module_key);

create index idx_household_modules_module_enabled
    on household_modules (module_key, enabled);

insert into household_modules (household_id, module_key, enabled)
select h.id, 'FINANCIAL', true
from households h
where h.deleted_at is null
on conflict (household_id, module_key) do nothing;

insert into household_modules (household_id, module_key, enabled)
select h.id, 'DRIVER', false
from households h
where h.deleted_at is null
on conflict (household_id, module_key) do nothing;
