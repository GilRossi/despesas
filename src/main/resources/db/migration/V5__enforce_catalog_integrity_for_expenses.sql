alter table subcategories
    add column household_id bigint;

update subcategories s
set household_id = c.household_id
from categories c
where c.id = s.category_id
  and s.household_id is null;

alter table subcategories
    alter column household_id set not null;

alter table subcategories
    add constraint fk_subcategories_household
    foreign key (household_id) references households (id) on delete restrict;

alter table categories
    add constraint uq_categories_id_household unique (id, household_id);

alter table subcategories
    add constraint uq_subcategories_id_household_category unique (id, household_id, category_id);

create index idx_subcategories_household_id_category_id_name
    on subcategories (household_id, category_id, lower(name));

create index idx_subcategories_household_id_id
    on subcategories (household_id, id);

drop index if exists uq_subcategories_category_name_active;

create unique index uq_subcategories_household_category_name_active
    on subcategories (household_id, category_id, lower(name))
    where deleted_at is null and active = true;

alter table expenses
    add column category_id bigint,
    add column subcategory_id bigint;

insert into categories (household_id, name, active)
select distinct e.household_id, trim(e.category), true
from expenses e
left join categories c
    on c.household_id = e.household_id
   and lower(c.name) = lower(trim(e.category))
   and c.deleted_at is null
where c.id is null;

insert into subcategories (household_id, category_id, name, active)
select distinct e.household_id, c.id, trim(e.subcategory), true
from expenses e
join categories c
    on c.household_id = e.household_id
   and lower(c.name) = lower(trim(e.category))
   and c.deleted_at is null
left join subcategories s
    on s.household_id = e.household_id
   and s.category_id = c.id
   and lower(s.name) = lower(trim(e.subcategory))
   and s.deleted_at is null
where s.id is null;

update expenses e
set category_id = c.id
from categories c
where e.category_id is null
  and c.household_id = e.household_id
  and lower(c.name) = lower(trim(e.category))
  and c.deleted_at is null;

update expenses e
set subcategory_id = s.id
from subcategories s
where e.subcategory_id is null
  and s.household_id = e.household_id
  and s.category_id = e.category_id
  and lower(s.name) = lower(trim(e.subcategory))
  and s.deleted_at is null;

do $$
begin
    if exists (
        select 1
        from expenses
        where category_id is null or subcategory_id is null
    ) then
        raise exception 'expenses backfill failed: category_id or subcategory_id is null';
    end if;
end
$$;

alter table expenses
    alter column category_id set not null,
    alter column subcategory_id set not null;

alter table expenses
    add constraint fk_expenses_category_household
    foreign key (category_id, household_id) references categories (id, household_id) on delete restrict;

alter table expenses
    add constraint fk_expenses_subcategory_household_category
    foreign key (subcategory_id, household_id, category_id) references subcategories (id, household_id, category_id) on delete restrict;

create index idx_expenses_household_id_category_fk_due_date_id
    on expenses (household_id, category_id, due_date desc, id desc);

create index idx_expenses_household_id_subcategory_fk_due_date_id
    on expenses (household_id, subcategory_id, due_date desc, id desc);
