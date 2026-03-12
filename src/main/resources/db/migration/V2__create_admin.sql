create table if not exists admin (
    id bigserial primary key,
    username varchar(100) not null,
    password varchar(255) not null,
    email varchar(320) not null,
    first_name varchar(100) null,
    last_name varchar(100) null,
    role varchar(50) not null,
    constraint uq_admin_email unique (email)
);

create index if not exists idx_admin_email on admin (email);
