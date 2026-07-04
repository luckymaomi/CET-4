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
  writing_count int not null default 0,
  writing_score decimal(6,2) not null default 0,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_rules_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_rules_bank foreign key (bank_id) references question_banks (id),
  constraint uk_exam_rules_bank unique (exam_id, bank_id),
  index idx_exam_rules_exam (exam_id)
);

create table exam_draft_questions (
  id bigint primary key auto_increment,
  exam_id bigint not null,
  source_question_id bigint not null,
  bank_id bigint not null,
  bank_name varchar(128) not null,
  type varchar(32) not null,
  stem text not null,
  analysis text null,
  score decimal(6,2) not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_draft_questions_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_draft_questions_source foreign key (source_question_id) references questions (id),
  constraint uk_exam_draft_questions_source unique (exam_id, source_question_id),
  index idx_exam_draft_questions_exam (exam_id)
);

create table exam_draft_options (
  id bigint primary key auto_increment,
  draft_question_id bigint not null,
  option_label varchar(8) not null,
  content text not null,
  is_correct bit not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_draft_options_question foreign key (draft_question_id) references exam_draft_questions (id),
  index idx_exam_draft_options_question (draft_question_id)
);

create table exam_draft_attachments (
  id bigint primary key auto_increment,
  draft_question_id bigint not null,
  file_name varchar(255) not null,
  file_url varchar(1000) not null,
  media_type varchar(32) not null,
  sort_order int not null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_draft_attachments_question foreign key (draft_question_id) references exam_draft_questions (id),
  index idx_exam_draft_attachments_question (draft_question_id)
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
  selected_labels varchar(255) null,
  answer_text text null,
  is_correct bit null,
  score decimal(6,2) not null default 0,
  review_comment varchar(1000) null,
  reviewed_by bigint null,
  reviewed_at datetime null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  constraint fk_exam_answers_attempt foreign key (attempt_id) references exam_attempts (id),
  constraint fk_exam_answers_question foreign key (attempt_question_id) references exam_attempt_questions (id),
  constraint fk_exam_answers_reviewer foreign key (reviewed_by) references users (id),
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
  objective_score decimal(7,2) not null default 0,
  subjective_score decimal(7,2) not null default 0,
  correct_count int not null,
  question_count int not null,
  grading_status varchar(20) not null default 'FINAL',
  submitted_at datetime not null,
  reviewed_at datetime null,
  created_at datetime not null default current_timestamp,
  constraint fk_exam_results_attempt foreign key (attempt_id) references exam_attempts (id),
  constraint fk_exam_results_exam foreign key (exam_id) references exams (id),
  constraint fk_exam_results_user foreign key (user_id) references users (id),
  constraint uk_exam_results_attempt unique (attempt_id),
  index idx_exam_results_exam (exam_id),
  index idx_exam_results_user (user_id)
);

insert into menus (id, code, title, path, parent_id, sort_order, icon)
values
  (6, 'question-banks', '????', '/exam/repo', null, 60, 'Collection'),
  (7, 'exams', '????', '/exam/manage', null, 70, 'Timer');

insert into role_menus (role_id, menu_id)
select 1, id from menus where id in (6, 7);
