alter table subcategories
    add constraint fk_subcategories_category_household
    foreign key (category_id, household_id) references categories (id, household_id) on delete restrict;
