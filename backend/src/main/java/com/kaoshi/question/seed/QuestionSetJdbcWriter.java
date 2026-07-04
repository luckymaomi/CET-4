package com.kaoshi.question.seed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class QuestionSetJdbcWriter {
    private final JdbcTemplate jdbcTemplate;

    public QuestionSetJdbcWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void importSet(String resourcePath, QuestionSetResource resource) {
        if (isImported(resource.code())) {
            return;
        }
        Map<String, Long> categoryIds = importCategories(resource.categories());
        Map<String, Long> bankIds = importBanks(resource.banks(), categoryIds);
        Map<String, Long> questionIds = importQuestions(resource.questions(), bankIds);
        importExams(resource.exams(), questionIds);
        jdbcTemplate.update(
                "insert into question_set_imports (resource_code, resource_path) values (?, ?)",
                resource.code(),
                resourcePath
        );
    }

    private boolean isImported(String code) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from question_set_imports where resource_code = ?",
                Integer.class,
                code
        );
        return count != null && count > 0;
    }

    private Map<String, Long> importCategories(List<QuestionSetResource.CategoryResource> categories) {
        Map<String, Long> ids = new HashMap<>();
        for (QuestionSetResource.CategoryResource category : categories) {
            Long id = insert("""
                    insert into question_categories (name, description, sort_order)
                    values (?, ?, ?)
                    """, ps -> {
                ps.setString(1, category.name());
                ps.setString(2, category.description());
                ps.setInt(3, valueOrDefault(category.sortOrder(), 0));
            });
            ids.put(category.code(), id);
        }
        return ids;
    }

    private Map<String, Long> importBanks(List<QuestionSetResource.BankResource> banks, Map<String, Long> categoryIds) {
        Map<String, Long> ids = new HashMap<>();
        for (QuestionSetResource.BankResource bank : banks) {
            Long categoryId = required(categoryIds, bank.categoryCode(), "题库分类不存在");
            Long id = insert("""
                    insert into question_banks (category_id, name, description, status)
                    values (?, ?, ?, ?)
                    """, ps -> {
                ps.setLong(1, categoryId);
                ps.setString(2, bank.name());
                ps.setString(3, bank.description());
                ps.setString(4, valueOrDefault(bank.status(), "ACTIVE"));
            });
            ids.put(bank.code(), id);
        }
        return ids;
    }

    private Map<String, Long> importQuestions(List<QuestionSetResource.QuestionResource> questions, Map<String, Long> bankIds) {
        Map<String, Long> ids = new HashMap<>();
        for (QuestionSetResource.QuestionResource question : questions) {
            Long bankId = required(bankIds, question.bankCode(), "题库不存在");
            Long questionId = insert("""
                    insert into questions (bank_id, type, stem, analysis, difficulty, status)
                    values (?, ?, ?, ?, ?, ?)
                    """, ps -> {
                ps.setLong(1, bankId);
                ps.setString(2, question.type());
                ps.setString(3, question.stem());
                ps.setString(4, question.analysis());
                ps.setString(5, valueOrDefault(question.difficulty(), "HARD"));
                ps.setString(6, valueOrDefault(question.status(), "ACTIVE"));
            });
            ids.put(question.code(), questionId);
            importOptions(questionId, question.options());
            importAttachments(questionId, question.attachments());
        }
        return ids;
    }

    private void importOptions(Long questionId, List<QuestionSetResource.OptionResource> options) {
        for (QuestionSetResource.OptionResource option : nullToEmpty(options)) {
            jdbcTemplate.update("""
                    insert into question_options (question_id, option_label, content, is_correct, sort_order)
                    values (?, ?, ?, ?, ?)
                    """,
                    questionId,
                    option.label(),
                    option.content(),
                    option.correct(),
                    valueOrDefault(option.sortOrder(), 0)
            );
        }
    }

    private void importAttachments(Long questionId, List<QuestionSetResource.AttachmentResource> attachments) {
        for (QuestionSetResource.AttachmentResource attachment : nullToEmpty(attachments)) {
            jdbcTemplate.update("""
                    insert into question_attachments (question_id, file_name, file_url, media_type, sort_order)
                    values (?, ?, ?, ?, ?)
                    """,
                    questionId,
                    attachment.fileName(),
                    attachment.fileUrl(),
                    attachment.mediaType(),
                    valueOrDefault(attachment.sortOrder(), 0)
            );
        }
    }

    private void importExams(List<QuestionSetResource.ExamResource> exams, Map<String, Long> questionIds) {
        for (QuestionSetResource.ExamResource exam : exams) {
            Long examId = insert("""
                    insert into exams (title, description, qualify_score, start_time, end_time, duration_minutes, time_limit, attempt_limit, display_mode, question_order_mode, open_type, status)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, ps -> {
                ps.setString(1, exam.title());
                ps.setString(2, exam.description());
                ps.setBigDecimal(3, exam.qualifyScore());
                ps.setObject(4, exam.startTime());
                ps.setObject(5, exam.endTime());
                ps.setInt(6, exam.durationMinutes());
                ps.setBoolean(7, Boolean.TRUE.equals(exam.timeLimit()));
                if (exam.attemptLimit() == null) {
                    ps.setObject(8, null);
                } else {
                    ps.setInt(8, exam.attemptLimit());
                }
                ps.setString(9, exam.displayMode());
                ps.setString(10, exam.questionOrderMode());
                ps.setString(11, exam.openType());
                ps.setString(12, exam.status());
            });
            importPaper(examId, exam.paperQuestions(), questionIds, "DRAFT");
            if ("PUBLISHED".equals(exam.status())) {
                importPaper(examId, exam.paperQuestions(), questionIds, "PUBLISHED");
            }
        }
    }

    private void importPaper(
            Long examId,
            List<QuestionSetResource.PaperQuestionResource> paperQuestions,
            Map<String, Long> questionIds,
            String target
    ) {
        for (QuestionSetResource.PaperQuestionResource paperQuestion : paperQuestions) {
            Long questionId = required(questionIds, paperQuestion.questionCode(), "试卷题目不存在");
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    select q.id, q.bank_id, b.name as bank_name, q.type, q.stem, q.analysis
                    from questions q
                    join question_banks b on b.id = q.bank_id
                    where q.id = ?
                    """, questionId);
            Long snapshotQuestionId = insertPaperQuestion(examId, paperQuestion, row, target);
            copyPaperOptions(snapshotQuestionId, questionId, target);
            copyPaperAttachments(snapshotQuestionId, questionId, target);
        }
    }

    private Long insertPaperQuestion(
            Long examId,
            QuestionSetResource.PaperQuestionResource paperQuestion,
            Map<String, Object> row,
            String target
    ) {
        if ("DRAFT".equals(target)) {
            return insert("""
                    insert into exam_draft_questions (exam_id, source_question_id, bank_id, bank_name, type, stem, analysis, score, sort_order)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, ps -> {
                ps.setLong(1, examId);
                ps.setLong(2, number(row.get("id")));
                ps.setLong(3, number(row.get("bank_id")));
                ps.setString(4, Objects.toString(row.get("bank_name"), ""));
                ps.setString(5, Objects.toString(row.get("type"), ""));
                ps.setString(6, Objects.toString(row.get("stem"), ""));
                ps.setString(7, Objects.toString(row.get("analysis"), null));
                ps.setBigDecimal(8, paperQuestion.score());
                ps.setInt(9, valueOrDefault(paperQuestion.sortOrder(), 0));
            });
        }
        return insert("""
                insert into exam_published_questions (exam_id, source_question_id, type, stem, analysis, score, sort_order)
                values (?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, examId);
            ps.setLong(2, number(row.get("id")));
            ps.setString(3, Objects.toString(row.get("type"), ""));
            ps.setString(4, Objects.toString(row.get("stem"), ""));
            ps.setString(5, Objects.toString(row.get("analysis"), null));
            ps.setBigDecimal(6, paperQuestion.score());
            ps.setInt(7, valueOrDefault(paperQuestion.sortOrder(), 0));
        });
    }

    private void copyPaperOptions(Long snapshotQuestionId, Long sourceQuestionId, String target) {
        String table = "DRAFT".equals(target) ? "exam_draft_options" : "exam_published_options";
        String column = "DRAFT".equals(target) ? "draft_question_id" : "published_question_id";
        jdbcTemplate.update("""
                insert into %s (%s, option_label, content, is_correct, sort_order)
                select ?, option_label, content, is_correct, sort_order
                from question_options
                where question_id = ?
                order by sort_order, id
                """.formatted(table, column), snapshotQuestionId, sourceQuestionId);
    }

    private void copyPaperAttachments(Long snapshotQuestionId, Long sourceQuestionId, String target) {
        String table = "DRAFT".equals(target) ? "exam_draft_attachments" : "exam_published_attachments";
        String column = "DRAFT".equals(target) ? "draft_question_id" : "published_question_id";
        jdbcTemplate.update("""
                insert into %s (%s, file_name, file_url, media_type, sort_order)
                select ?, file_name, file_url, media_type, sort_order
                from question_attachments
                where question_id = ?
                order by sort_order, id
                """.formatted(table, column), snapshotQuestionId, sourceQuestionId);
    }

    private Long insert(String sql, StatementBinder binder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            binder.bind(statement);
            return statement;
        }, keyHolder);
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            return number(keyHolder.getKeys().get("id"));
        }
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private <T> List<T> nullToEmpty(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private <T> T required(Map<String, T> values, String key, String message) {
        T value = values.get(key);
        if (value == null) {
            throw new IllegalStateException(message + ": " + key);
        }
        return value;
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws java.sql.SQLException;
    }
}
