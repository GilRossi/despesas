alter table expenses
    rename column category to category_name_snapshot;

alter table expenses
    rename column subcategory to subcategory_name_snapshot;

drop index if exists idx_expenses_household_id_category_id_due_date_id;

create index idx_expenses_household_id_category_snapshot_due_date_id
    on expenses (household_id, category_name_snapshot, due_date desc, id desc);
