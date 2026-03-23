create table api_refresh_tokens (
	id bigserial primary key,
	token_id varchar(36) not null,
	family_id varchar(36) not null,
	user_id bigint not null,
	token_hash varchar(255) not null,
	expires_at timestamp with time zone not null,
	last_used_at timestamp with time zone,
	revoked_at timestamp with time zone,
	replaced_by_token_id varchar(36),
	revocation_reason varchar(32),
	created_at timestamp with time zone not null default now(),
	updated_at timestamp with time zone not null default now(),
	constraint uq_api_refresh_tokens_token_id unique (token_id),
	constraint fk_api_refresh_tokens_user foreign key (user_id) references users (id)
);

create index idx_api_refresh_tokens_family on api_refresh_tokens (family_id);
create index idx_api_refresh_tokens_user_active on api_refresh_tokens (user_id, revoked_at, expires_at);
