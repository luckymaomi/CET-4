insert into menus (id, code, title, path, parent_id, sort_order, icon)
values (11, 'departments', '部门管理', '/sys/departments', null, 30, 'OfficeBuilding');

insert into role_menus (role_id, menu_id)
values (1, 11);
