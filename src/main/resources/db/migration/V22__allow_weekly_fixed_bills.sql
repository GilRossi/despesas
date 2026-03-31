alter table fixed_bills
    drop constraint if exists chk_fixed_bills_frequency;

alter table fixed_bills
    add constraint chk_fixed_bills_frequency
    check (frequency in ('WEEKLY', 'MONTHLY'));
