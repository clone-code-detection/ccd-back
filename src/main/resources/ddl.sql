-- we don't know how to generate root <with-no-name> (class Root) :(
create table authen."user"
(
    username    varchar(255)
        constraint user_pk2
            unique,
    password    varchar(64),
    id          uuid default gen_random_uuid() not null
        constraint user_pk
            primary key,
    authorities varchar(255)[]
);

alter table authen."user"
    owner to postgres;

create table authen.role
(
    id   uuid default gen_random_uuid() not null
        constraint role_pk
            primary key,
    name varchar(255)
        constraint role_pk2
            unique
);

alter table authen.role
    owner to postgres;

create table authen.authority
(
    id   uuid default gen_random_uuid() not null
        constraint authorities_pk
            primary key,
    name varchar(255)
        constraint authority_pk
            unique
);

alter table authen.authority
    owner to postgres;

create table authen.relation_role_authority
(
    role_id      uuid not null
        constraint relation_role_authority_role_id_fk
            references authen.role,
    authority_id uuid not null
        constraint relation_role_authority_authority_id_fk
            references authen.authority,
    constraint role_authority_pk
        primary key (role_id, authority_id)
);

alter table authen.relation_role_authority
    owner to postgres;

create table authen.relation_user_role
(
    user_id uuid not null
        constraint fkhyqhmdqwhaix7yo4vxfn0ockb
            references authen."user",
    role_id uuid not null
        constraint relation_user_role_role_id_fk
            references authen.role,
    constraint user_role_pk
        primary key (user_id, role_id)
);

alter table authen.relation_user_role
    owner to postgres;

create table public.highlight_report
(
    id           serial
        primary key,
    assigner     varchar(255) not null,
    author       varchar(255) not null,
    course       varchar(255) not null,
    created_at   timestamp(6) not null,
    organization varchar(255) not null,
    project      varchar(255) not null,
    semester     integer      not null,
    updated_at   timestamp(6) not null,
    uri          varchar(255) not null,
    year         integer      not null,
    constraint report_unique_id
        unique (organization, year, semester, course, assigner, project, author)
);

alter table public.highlight_report
    owner to postgres;

create table public.highlight_report_extra_data
(
    highlight_report_id integer not null
        constraint fknxyh0059w61jrv4q6ihex1vw5
            references public.highlight_report,
    extra_data          varchar(255)
);

alter table public.highlight_report_extra_data
    owner to postgres;

create table file.file
(
    id        uuid default gen_random_uuid() not null
        constraint file_pk
            primary key,
    content   bytea                          not null,
    file_name varchar(255)                   not null,
    uid       uuid
        constraint fkpaqaso946elddk9hq85018158
            references authen."user"
);

alter table file.file
    owner to postgres;

create table file.file_meta
(
    created time not null,
    id      uuid not null
        constraint file_meta_pk
            primary key
        constraint file_meta_file_id_fk
            references file.file
);

alter table file.file_meta
    owner to postgres;

create table highlight.highlight_session
(
    id           uuid default gen_random_uuid() not null
        constraint highlight_session_pk
            primary key,
    created_time time default now(),
    user_id      uuid
        constraint highlight_session_user_id_fk
            references authen."user"
);

alter table highlight.highlight_session
    owner to postgres;

create table highlight.highlight_single_document
(
    source_id  uuid                           not null
        constraint highlight_single_document_file_id_fk2
            references file.file,
    target_id  uuid                           not null
        constraint highlight_single_document_file_id_fk
            references file.file,
    id         uuid default gen_random_uuid() not null
        constraint highlight_single_document_pk
            primary key,
    session_id uuid
        constraint highlight_single_document_highlight_session_id_fk
            references highlight.highlight_session
);

alter table highlight.highlight_single_document
    owner to postgres;

create table highlight.highlight_single_document_match
(
    single_document_id uuid
        constraint highlight_single_document_matches_file_id_fk
            references highlight.highlight_single_document,
    start_offset       integer not null,
    end_offset         integer not null,
    id                 uuid    not null
        constraint highlight_single_document_matches_pk
            primary key,
    constraint check_name
        check (start < "end")
);

alter table highlight.highlight_single_document_match
    owner to postgres;

create function authen.get_authorities(uuid) returns text[]
    strict
    language sql
as
$$
select array_agg(auth)
from (select distinct (concat('ROLE_', role.name)) as auth
      from authen."user"
               join authen.relation_user_role rur on "user".id = rur.user_id
               join authen.relation_role_authority rra on rur.role_id = rra.role_id
               join authen.role on rra.role_id = role.id
               join authen.authority on rra.authority_id = authority.id
      where authen."user".id = $1

      UNION ALL

      select distinct authority.name as auth
      from authen."user"
               join authen.relation_user_role rur on "user".id = rur.user_id
               join authen.relation_role_authority rra on rur.role_id = rra.role_id
               join authen.role on rra.role_id = role.id
               join authen.authority on rra.authority_id = authority.id
      where authen."user".id = $1) as urrraaurrraa;
$$;

alter function authen.get_authorities(uuid) owner to postgres;

