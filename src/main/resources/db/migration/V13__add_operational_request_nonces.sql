create table operational_request_nonces (
	id bigserial primary key,
	key_id varchar(80) not null,
	nonce_hash varchar(64) not null,
	request_method varchar(8) not null,
	request_path varchar(255) not null,
	created_at timestamp with time zone not null default now(),
	expires_at timestamp with time zone not null,
	constraint uq_operational_request_nonces_key_nonce unique (key_id, nonce_hash)
);

create index idx_operational_request_nonces_expires_at on operational_request_nonces (expires_at);
