alter table expenses
    add column legacy_tb_despesa_id bigint;

create unique index uq_expenses_legacy_tb_despesa_id
    on expenses (legacy_tb_despesa_id)
    where legacy_tb_despesa_id is not null;

insert into categories (household_id, name, active)
select distinct 1, trim(t.categoria), true
from tb_despesas t
left join categories c
    on c.household_id = 1
   and lower(c.name) = lower(trim(t.categoria))
   and c.deleted_at is null
where c.id is null;

insert into subcategories (household_id, category_id, name, active)
select distinct 1, c.id, 'Sem Subcategoria', true
from tb_despesas t
join categories c
    on c.household_id = 1
   and lower(c.name) = lower(trim(t.categoria))
   and c.deleted_at is null
left join subcategories s
    on s.household_id = 1
   and s.category_id = c.id
   and lower(s.name) = lower('Sem Subcategoria')
   and s.deleted_at is null
where s.id is null;

insert into expenses (
    household_id,
    description,
    amount,
    due_date,
    context,
    category_name_snapshot,
    subcategory_name_snapshot,
    notes,
    created_at,
    updated_at,
    category_id,
    subcategory_id,
    legacy_tb_despesa_id
)
select
    1,
    trim(t.descricao),
    t.valor,
    t.data,
    'GERAL',
    trim(t.categoria),
    'Sem Subcategoria',
    'Migrated from tb_despesas',
    t.data::timestamp,
    t.data::timestamp,
    c.id,
    s.id,
    t.id
from tb_despesas t
join categories c
    on c.household_id = 1
   and lower(c.name) = lower(trim(t.categoria))
   and c.deleted_at is null
join subcategories s
    on s.household_id = 1
   and s.category_id = c.id
   and lower(s.name) = lower('Sem Subcategoria')
   and s.deleted_at is null
left join expenses e
    on e.legacy_tb_despesa_id = t.id
where e.id is null;
