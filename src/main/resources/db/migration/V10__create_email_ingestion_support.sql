create table email_ingestion_sources (
	id bigserial primary key,
	household_id bigint not null references households(id),
	source_account varchar(160) not null,
	normalized_source_account varchar(160) not null unique,
	label varchar(120),
	active boolean not null default true,
	auto_import_min_confidence numeric(4,3) not null default 0.900,
	review_min_confidence numeric(4,3) not null default 0.650,
	created_at timestamptz not null default now(),
	updated_at timestamptz not null default now(),
	constraint chk_email_ingestion_source_auto_confidence_range
		check (auto_import_min_confidence >= 0 and auto_import_min_confidence <= 1),
	constraint chk_email_ingestion_source_review_confidence_range
		check (review_min_confidence >= 0 and review_min_confidence <= 1),
	constraint chk_email_ingestion_source_confidence_order
		check (auto_import_min_confidence >= review_min_confidence)
);

create index idx_email_ingestion_sources_household on email_ingestion_sources(household_id);

create table email_ingestions (
	id bigserial primary key,
	household_id bigint not null references households(id),
	source_id bigint not null references email_ingestion_sources(id),
	source_account varchar(160) not null,
	normalized_source_account varchar(160) not null,
	external_message_id varchar(255) not null,
	sender varchar(255) not null,
	subject varchar(255) not null,
	received_at timestamptz not null,
	merchant_or_payee varchar(140),
	suggested_category_name varchar(80),
	suggested_subcategory_name varchar(80),
	total_amount numeric(15,2),
	due_date date,
	occurred_on date,
	currency varchar(3) not null,
	summary varchar(500),
	classification varchar(40) not null,
	confidence numeric(4,3) not null,
	raw_reference varchar(500) not null,
	desired_decision varchar(20) not null,
	final_decision varchar(20) not null,
	decision_reason varchar(40) not null,
	fingerprint varchar(64) not null,
	imported_expense_id bigint references expenses(id),
	created_at timestamptz not null default now(),
	updated_at timestamptz not null default now(),
	constraint uq_email_ingestions_source_message unique (normalized_source_account, external_message_id),
	constraint chk_email_ingestions_confidence_range check (confidence >= 0 and confidence <= 1)
);

create index idx_email_ingestions_household_fingerprint on email_ingestions(household_id, fingerprint);
create index idx_email_ingestions_source_received_at on email_ingestions(source_id, received_at desc);

create table email_ingestion_items (
	id bigserial primary key,
	ingestion_id bigint not null references email_ingestions(id) on delete cascade,
	line_number integer not null,
	description varchar(255) not null,
	amount numeric(15,2),
	quantity numeric(12,3),
	constraint uq_email_ingestion_items_line unique (ingestion_id, line_number)
);
