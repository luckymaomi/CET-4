create table question_categories (
  id bigint primary key auto_increment,
  name varchar(64) not null,
  description varchar(255) null,
  sort_order int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint uk_question_categories_name unique (name)
);

create table question_banks (
  id bigint primary key auto_increment,
  category_id bigint not null,
  name varchar(128) not null,
  description varchar(500) null,
  status varchar(20) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint fk_question_banks_category foreign key (category_id) references question_categories (id),
  index idx_question_banks_category (category_id),
  index idx_question_banks_status (status)
);

create table questions (
  id bigint primary key auto_increment,
  bank_id bigint not null,
  type varchar(32) not null,
  stem text not null,
  analysis text null,
  difficulty varchar(20) not null,
  status varchar(20) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint fk_questions_bank foreign key (bank_id) references question_banks (id),
  index idx_questions_bank (bank_id),
  index idx_questions_type (type),
  index idx_questions_status (status)
);

create table question_options (
  id bigint primary key auto_increment,
  question_id bigint not null,
  option_label varchar(8) not null,
  content text not null,
  is_correct bit not null,
  sort_order int not null default 0,
  created_at datetime not null default current_timestamp,
  constraint fk_question_options_question foreign key (question_id) references questions (id),
  constraint uk_question_options_label unique (question_id, option_label),
  index idx_question_options_question (question_id)
);

create table question_attachments (
  id bigint primary key auto_increment,
  question_id bigint not null,
  file_name varchar(255) not null,
  file_url varchar(1000) not null,
  media_type varchar(32) not null,
  sort_order int not null default 0,
  created_at datetime not null default current_timestamp,
  constraint fk_question_attachments_question foreign key (question_id) references questions (id),
  index idx_question_attachments_question (question_id)
);

create table exams (
  id bigint primary key auto_increment,
  title varchar(128) not null,
  description varchar(500) null,
  qualify_score decimal(7,2) not null default 0,
  start_time datetime not null,
  end_time datetime not null,
  duration_minutes int not null,
  time_limit bit not null default true,
  attempt_limit int null,
  display_mode varchar(20) not null default 'PAGED',
  question_order_mode varchar(20) not null default 'FIXED',
  open_type varchar(20) not null default 'PUBLIC',
  status varchar(20) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_exams_status (status),
  index idx_exams_time (start_time, end_time)
);

create table exam_rules (
  id bigint primary key auto_increment,
  exam_id bigint not null,
  bank_id bigint not null,
  single_count int not null default 0,
  single_score decimal(6,2) not null default 0,
  multiple_count int not null default 0,
  multiple_score decimal(6,2) not null default 0,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_rules_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_rules_bank foreign key (bank_id) references question_banks (id),
  constraint uk_exam_rules_bank unique (exam_id, bank_id),
  index idx_exam_rules_exam (exam_id)
);

create table exam_published_questions (
  id bigint primary key auto_increment,
  exam_id bigint not null,
  source_question_id bigint not null,
  type varchar(32) not null,
  stem text not null,
  analysis text null,
  score decimal(6,2) not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_published_questions_exam foreign key (exam_id) references exams (id),
  index idx_exam_published_questions_exam (exam_id)
);

create table exam_published_options (
  id bigint primary key auto_increment,
  published_question_id bigint not null,
  option_label varchar(8) not null,
  content text not null,
  is_correct bit not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_published_options_question foreign key (published_question_id) references exam_published_questions (id),
  index idx_exam_published_options_question (published_question_id)
);

create table exam_published_attachments (
  id bigint primary key auto_increment,
  published_question_id bigint not null,
  file_name varchar(255) not null,
  file_url varchar(1000) not null,
  media_type varchar(32) not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_published_attachments_question foreign key (published_question_id) references exam_published_questions (id),
  index idx_exam_published_attachments_question (published_question_id)
);

create table exam_departments (
  exam_id bigint not null,
  department_id bigint not null,
  created_at datetime not null default current_timestamp,
  primary key (exam_id, department_id),
  constraint fk_exam_departments_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_departments_department foreign key (department_id) references departments (id)
);

create table exam_attempts (
  id bigint primary key auto_increment,
  exam_id bigint not null,
  user_id bigint not null,
  status varchar(20) not null,
  started_at datetime not null default current_timestamp,
  submitted_at datetime null,
  total_score decimal(7,2) not null default 0,
  obtained_score decimal(7,2) not null default 0,
  duration_seconds int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint fk_exam_attempts_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_attempts_user foreign key (user_id) references users (id),
  index idx_exam_attempts_exam_user (exam_id, user_id),
  index idx_exam_attempts_user (user_id),
  index idx_exam_attempts_status (status)
);

create table exam_attempt_questions (
  id bigint primary key auto_increment,
  attempt_id bigint not null,
  published_question_id bigint not null,
  source_question_id bigint not null,
  type varchar(32) not null,
  stem text not null,
  analysis text null,
  score decimal(6,2) not null,
  sort_order int not null,
  display_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_attempt_questions_attempt foreign key (attempt_id) references exam_attempts (id),
  index idx_exam_attempt_questions_attempt (attempt_id)
);

create table exam_attempt_options (
  id bigint primary key auto_increment,
  attempt_question_id bigint not null,
  option_label varchar(8) not null,
  content text not null,
  is_correct bit not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_attempt_options_question foreign key (attempt_question_id) references exam_attempt_questions (id),
  index idx_exam_attempt_options_question (attempt_question_id)
);

create table exam_attempt_attachments (
  id bigint primary key auto_increment,
  attempt_question_id bigint not null,
  file_name varchar(255) not null,
  file_url varchar(1000) not null,
  media_type varchar(32) not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_attempt_attachments_question foreign key (attempt_question_id) references exam_attempt_questions (id),
  index idx_exam_attempt_attachments_question (attempt_question_id)
);

create table exam_answers (
  id bigint primary key auto_increment,
  attempt_id bigint not null,
  attempt_question_id bigint not null,
  selected_labels varchar(255) not null,
  is_correct bit not null,
  score decimal(6,2) not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint fk_exam_answers_attempt foreign key (attempt_id) references exam_attempts (id),
  constraint fk_exam_answers_question foreign key (attempt_question_id) references exam_attempt_questions (id),
  constraint uk_exam_answers_question unique (attempt_question_id),
  index idx_exam_answers_attempt (attempt_id)
);

create table exam_results (
  id bigint primary key auto_increment,
  attempt_id bigint not null,
  exam_id bigint not null,
  user_id bigint not null,
  total_score decimal(7,2) not null,
  obtained_score decimal(7,2) not null,
  correct_count int not null,
  question_count int not null,
  submitted_at datetime not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_results_attempt foreign key (attempt_id) references exam_attempts (id),
  constraint fk_exam_results_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_results_user foreign key (user_id) references users (id),
  constraint uk_exam_results_attempt unique (attempt_id),
  index idx_exam_results_exam (exam_id),
  index idx_exam_results_user (user_id)
);

insert into question_categories (id, name, description, sort_order)
values (1, '默认试题分类', '系统初始化试题分类', 10);

insert into question_banks (id, category_id, name, description, status)
values (1, 1, '英语基础题库', '系统初始化题库，用于验证单选和多选主链路', 'ACTIVE');

insert into questions (id, bank_id, type, stem, analysis, difficulty, status)
values
  (1, 1, 'SINGLE_CHOICE', 'Choose the correct sentence.', 'Subject and verb agreement: He goes to school every day.', 'EASY', 'ACTIVE'),
  (2, 1, 'MULTIPLE_CHOICE', 'Which words are nouns?', 'Book and teacher are nouns.', 'EASY', 'ACTIVE'),
  (3, 1, 'SINGLE_CHOICE', 'Which option means “提高”?', 'Improve means 提高.', 'EASY', 'ACTIVE');

insert into question_options (question_id, option_label, content, is_correct, sort_order)
values
  (1, 'A', 'He go to school every day.', false, 10),
  (1, 'B', 'He goes to school every day.', true, 20),
  (1, 'C', 'He going to school every day.', false, 30),
  (1, 'D', 'He gone to school every day.', false, 40),
  (2, 'A', 'book', true, 10),
  (2, 'B', 'quickly', false, 20),
  (2, 'C', 'teacher', true, 30),
  (2, 'D', 'beautiful', false, 40),
  (3, 'A', 'improve', true, 10),
  (3, 'B', 'borrow', false, 20),
  (3, 'C', 'forget', false, 30),
  (3, 'D', 'remain', false, 40);

insert into question_attachments (question_id, file_name, file_url, media_type, sort_order)
values (1, 'dog-wolf-friendship.mp3', '/local-assets/dog-wolf-friendship.mp3', 'AUDIO', 10);

insert into exams (id, title, description, qualify_score, start_time, end_time, duration_minutes, time_limit, attempt_limit, display_mode, question_order_mode, open_type, status)
values (1, '英语基础模拟考试', '系统初始化考试，用于验证考试端作答闭环', 9.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 30, true, null, 'PAGED', 'FIXED', 'PUBLIC', 'PUBLISHED');

insert into exam_rules (exam_id, bank_id, single_count, single_score, multiple_count, multiple_score, sort_order)
values (1, 1, 2, 5.00, 1, 5.00, 10);

insert into exam_published_questions (id, exam_id, source_question_id, type, stem, analysis, score, sort_order)
values
  (1, 1, 1, 'SINGLE_CHOICE', 'Choose the correct sentence.', 'Subject and verb agreement: He goes to school every day.', 5.00, 10),
  (2, 1, 3, 'SINGLE_CHOICE', 'Which option means “提高”?', 'Improve means 提高.', 5.00, 20),
  (3, 1, 2, 'MULTIPLE_CHOICE', 'Which words are nouns?', 'Book and teacher are nouns.', 5.00, 30);

insert into exam_published_options (published_question_id, option_label, content, is_correct, sort_order)
select epq.id, qo.option_label, qo.content, qo.is_correct, qo.sort_order
from exam_published_questions epq
join question_options qo on qo.question_id = epq.source_question_id;

insert into exam_published_attachments (published_question_id, file_name, file_url, media_type, sort_order)
select epq.id, qa.file_name, qa.file_url, qa.media_type, qa.sort_order
from exam_published_questions epq
join question_attachments qa on qa.question_id = epq.source_question_id;

insert into menus (id, code, title, path, parent_id, sort_order, icon)
values
  (6, 'question-banks', '题库管理', '/exam/repo', null, 60, 'Collection'),
  (7, 'questions', '试题管理', '/exam/qu', null, 70, 'Document'),
  (8, 'exams', '考试管理', '/exam/manage', null, 80, 'Timer');

insert into role_menus (role_id, menu_id)
select 1, id from menus where id between 6 and 8;
