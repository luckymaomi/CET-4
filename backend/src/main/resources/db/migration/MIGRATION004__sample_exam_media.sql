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
