---!Ups
create table "User"(
    name text primary key not null,
    apiKey text not null
);


insert into "User"(name,apiKey) values ("User1",111);
insert into "User"(name,apiKey) values ("User2",222);
insert into "User"(name,apiKey) values ("User3",333);
insert into "User"(name,apiKey) values ("User4",444);

---!Downs
drop table if exists "User";

