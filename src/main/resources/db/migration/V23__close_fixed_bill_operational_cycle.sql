alter table fixed_bills
    add constraint uq_fixed_bills_id_household unique (id, household_id);

alter table expenses
    add column fixed_bill_id bigint;

alter table expenses
    add constraint fk_expenses_fixed_bill
    foreign key (fixed_bill_id)
    references fixed_bills (id)
    on delete restrict;

create index idx_expenses_household_fixed_bill_effective_date_id
    on expenses (household_id, fixed_bill_id, coalesce(due_date, occurred_on) desc, id desc);

create unique index uq_expenses_fixed_bill_due_date_active
    on expenses (fixed_bill_id, due_date)
    where fixed_bill_id is not null and due_date is not null and deleted_at is null;
