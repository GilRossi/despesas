create unique index uq_household_members_user_active
    on household_members (user_id)
    where deleted_at is null;
