create table persisted_audit_events (
	id bigserial primary key,
	occurred_at timestamp with time zone not null default now(),
	purge_after timestamp with time zone not null,
	category varchar(32) not null,
	event_type varchar(80) not null,
	status varchar(24) not null,
	user_id bigint null,
	household_id bigint null,
	actor_role varchar(40) null,
	source_key varchar(120) null,
	request_method varchar(8) null,
	request_path varchar(255) null,
	primary_reference varchar(120) null,
	secondary_reference varchar(120) null,
	detail_code varchar(80) null,
	safe_context_json text null
);

create index idx_persisted_audit_events_occurred_at on persisted_audit_events (occurred_at);
create index idx_persisted_audit_events_purge_after on persisted_audit_events (purge_after);
create index idx_persisted_audit_events_category_type on persisted_audit_events (category, event_type);
create index idx_persisted_audit_events_user_occurred on persisted_audit_events (user_id, occurred_at);
create index idx_persisted_audit_events_household_occurred on persisted_audit_events (household_id, occurred_at);

create table rate_limit_counters (
	id bigserial primary key,
	scope varchar(64) not null,
	scope_key varchar(180) not null,
	window_start timestamp with time zone not null,
	window_end timestamp with time zone not null,
	request_count integer not null,
	constraint uq_rate_limit_counters_scope_key_window unique (scope, scope_key, window_start)
);

create index idx_rate_limit_counters_window_end on rate_limit_counters (window_end);
create index idx_rate_limit_counters_scope_key on rate_limit_counters (scope, scope_key);
