insert into questions (id, bank_id, type, stem, analysis, difficulty, status)
values
  (4, 1, 'MULTIPLE_CHOICE', 'Look at the chart and choose the correct statements.', 'The chart highlights daily reading and listening practice.', 'HARD', 'ACTIVE');

insert into question_options (question_id, option_label, content, is_correct, sort_order)
values
  (4, 'A', 'The learner practiced reading.', true, 10),
  (4, 'B', 'The learner practiced listening.', true, 20),
  (4, 'C', 'The learner skipped all practice.', false, 30),
  (4, 'D', 'The chart is about cooking.', false, 40);

insert into question_attachments (question_id, file_name, file_url, media_type, sort_order)
values
  (2, 'noun-example.png', '/local-assets/noun-example.png', 'IMAGE', 10),
  (3, 'improve-card.jpg', '/local-assets/improve-card.jpg', 'IMAGE', 10),
  (4, 'practice-chart.png', '/local-assets/practice-chart.png', 'IMAGE', 10),
  (4, 'dog-wolf-friendship.mp3', '/local-assets/dog-wolf-friendship.mp3', 'AUDIO', 20);

update exam_rules
set multiple_count = 2
where exam_id = 1 and bank_id = 1;

update exams
set qualify_score = 12.00,
    updated_at = current_timestamp
where id = 1;

insert into exam_draft_questions (exam_id, source_question_id, bank_id, bank_name, type, stem, analysis, score, sort_order)
values (1, 4, 1, '英语基础题库', 'MULTIPLE_CHOICE', 'Look at the chart and choose the correct statements.', 'The chart highlights daily reading and listening practice.', 5.00, 40);

insert into exam_draft_options (draft_question_id, option_label, content, is_correct, sort_order)
select edq.id, qo.option_label, qo.content, qo.is_correct, qo.sort_order
from exam_draft_questions edq
join question_options qo on qo.question_id = edq.source_question_id
where edq.source_question_id = 4;

insert into exam_draft_attachments (draft_question_id, file_name, file_url, media_type, sort_order)
select edq.id, qa.file_name, qa.file_url, qa.media_type, qa.sort_order
from exam_draft_questions edq
join question_attachments qa on qa.question_id = edq.source_question_id
where edq.source_question_id = 4;

insert into exam_published_questions (id, exam_id, source_question_id, type, stem, analysis, score, sort_order)
values (4, 1, 4, 'MULTIPLE_CHOICE', 'Look at the chart and choose the correct statements.', 'The chart highlights daily reading and listening practice.', 5.00, 40);

insert into exam_published_options (published_question_id, option_label, content, is_correct, sort_order)
select epq.id, qo.option_label, qo.content, qo.is_correct, qo.sort_order
from exam_published_questions epq
join question_options qo on qo.question_id = epq.source_question_id
where epq.source_question_id = 4;

insert into exam_published_attachments (published_question_id, file_name, file_url, media_type, sort_order)
select epq.id, qa.file_name, qa.file_url, qa.media_type, qa.sort_order
from exam_published_questions epq
join question_attachments qa on qa.question_id = epq.source_question_id
where epq.source_question_id in (2, 3, 4);

insert into exam_attempt_questions (attempt_id, published_question_id, source_question_id, type, stem, analysis, score, sort_order, display_order)
select ea.id, epq.id, epq.source_question_id, epq.type, epq.stem, epq.analysis, epq.score, epq.sort_order, epq.sort_order
from exam_attempts ea
join exam_published_questions epq on epq.exam_id = ea.exam_id and epq.source_question_id = 4
where ea.exam_id = 1;

insert into exam_attempt_options (attempt_question_id, option_label, content, is_correct, sort_order)
select eaq.id, epo.option_label, epo.content, epo.is_correct, epo.sort_order
from exam_attempt_questions eaq
join exam_published_options epo on epo.published_question_id = eaq.published_question_id
where eaq.source_question_id = 4;

insert into exam_attempt_attachments (attempt_question_id, file_name, file_url, media_type, sort_order)
select eaq.id, epa.file_name, epa.file_url, epa.media_type, epa.sort_order
from exam_attempt_questions eaq
join exam_published_attachments epa on epa.published_question_id = eaq.published_question_id
where eaq.source_question_id = 4;

insert into exam_answers (attempt_id, attempt_question_id, selected_labels, is_correct, score)
select eaq.attempt_id,
       eaq.id,
       case when mod(eaq.attempt_id, 2) = 0 then 'A,B' else 'C' end,
       case when mod(eaq.attempt_id, 2) = 0 then true else false end,
       case when mod(eaq.attempt_id, 2) = 0 then eaq.score else 0 end
from exam_attempt_questions eaq
where eaq.source_question_id = 4;

update exam_attempts
set total_score = 20.00,
    obtained_score = case when mod(id, 2) = 0 then 20.00 else 0.00 end
where exam_id = 1 and id between 1 and 10;

update exam_results
set total_score = 20.00,
    obtained_score = case when mod(attempt_id, 2) = 0 then 20.00 else 0.00 end,
    correct_count = case when mod(attempt_id, 2) = 0 then 4 else 0 end,
    question_count = 4
where exam_id = 1 and attempt_id between 1 and 10;
