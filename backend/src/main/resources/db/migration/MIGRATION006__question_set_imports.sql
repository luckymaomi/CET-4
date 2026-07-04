create table question_set_imports (
  id bigint primary key auto_increment,
  resource_code varchar(128) not null,
  resource_path varchar(500) not null,
  imported_at datetime not null default current_timestamp,
  constraint uk_question_set_imports_code unique (resource_code)
);
