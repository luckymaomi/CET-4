create table menus (
  id bigint primary key auto_increment,
  code varchar(128) not null,
  title varchar(64) not null,
  path varchar(255) not null,
  parent_id bigint null,
  sort_order int not null default 0,
  icon varchar(64) null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint uk_menus_code unique (code),
  constraint fk_menus_parent foreign key (parent_id) references menus (id)
);

create table role_menus (
  role_id bigint not null,
  menu_id bigint not null,
  created_at datetime not null default current_timestamp,
  primary key (role_id, menu_id),
  constraint fk_role_menus_role foreign key (role_id) references roles (id),
  constraint fk_role_menus_menu foreign key (menu_id) references menus (id)
);

insert into menus (id, code, title, path, parent_id, sort_order, icon)
values
  (1, 'dashboard', '工作台', '/dashboard', null, 10, 'Grid'),
  (2, 'users', '用户管理', '/sys/users', null, 40, 'User'),
  (3, 'roles', '角色管理', '/sys/roles', null, 20, 'Key');

insert into role_menus (role_id, menu_id)
select 1, id from menus;
