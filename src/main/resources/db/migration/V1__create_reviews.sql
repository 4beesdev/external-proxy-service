create table if not exists review (
    id bigserial primary key,
    rating smallint not null check (rating between 1 and 5),
    comment text null,
    username varchar(100) not null,
    email varchar(320) not null,
    ip_hash char(64) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_review_ip_hash on review (ip_hash);
create index if not exists idx_review_created_at on review (created_at desc);

create table if not exists review_like (
    id bigserial primary key,
    review_id bigint not null references review(id) on delete cascade,
    ip_hash char(64) not null,
    created_at timestamptz not null default now(),
    constraint uq_review_like_review_ip unique (review_id, ip_hash)
);

create index if not exists idx_review_like_review_id on review_like (review_id);
