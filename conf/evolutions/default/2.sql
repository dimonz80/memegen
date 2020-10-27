---!Ups
create table "User"(
    name text primary key not null,
    apiKey text not null
);

---!Downs
drop table if exists "User";

