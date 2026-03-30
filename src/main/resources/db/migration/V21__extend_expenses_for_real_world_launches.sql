alter table expenses
    alter column due_date drop not null;

alter table expenses
    add column occurred_on date,
    add column space_reference_id bigint;

update expenses
set occurred_on = coalesce(due_date, created_at::date)
where occurred_on is null;

alter table expenses
    alter column occurred_on set not null;

alter table expenses
    add constraint fk_expenses_space_reference_household
    foreign key (space_reference_id, household_id)
    references space_references (id, household_id)
    on delete restrict;

create index idx_expenses_household_effective_date_id
    on expenses (household_id, coalesce(due_date, occurred_on) desc, id desc);

create index idx_expenses_household_space_reference_effective_date_id
    on expenses (household_id, space_reference_id, coalesce(due_date, occurred_on) desc, id desc);
