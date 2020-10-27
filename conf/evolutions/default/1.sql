---!Ups

create table "User"(
    name text primary key not null,
    apiKey text not null
);


insert into "User"(name,apiKey) values ("User1",111);
insert into "User"(name,apiKey) values ("User2",222);
insert into "User"(name,apiKey) values ("User3",333);
insert into "User"(name,apiKey) values ("User4",444)


create table MemeMetadata(
    id bigserial primary key not null,
    url text not null,
    "user" text not null references "User"(name),
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

drop table if exists "User";

