---!Ups
create table MemeMetadata(
    id bigserial primary key not null,
    url text not null,
    "user" text not null,
    name text not null,
    comment text
);

create table MemeTemplate(
    id text primary key not null,
    name text not null,
    url text not null,
    width int not null,
    height int not null,
    box_count int not null
);

---!Downs
drop table if exists MemeTemplate;
drop table if exists MemeMetadata;

