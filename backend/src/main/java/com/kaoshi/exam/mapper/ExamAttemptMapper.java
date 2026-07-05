package com.kaoshi.exam.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

interface ExamAttemptMapper {
    @Select("""
            select *
            from exam_attempts
            where exam_id = #{examId}
              and user_id = #{userId}
              and status = 'IN_PROGRESS'
            order by id desc
            limit 1
            """)
    Map<String, Object> findInProgressAttempt(@Param("examId") Long examId, @Param("userId") Long userId);

    @Select("select count(*) from exam_attempts where exam_id = #{examId} and user_id = #{userId} and status = 'SUBMITTED'")
    int countSubmittedAttempts(@Param("examId") Long examId, @Param("userId") Long userId);

    @Insert("""
            insert into exam_attempts (exam_id, user_id, status, total_score, obtained_score, duration_seconds)
            values (#{examId}, #{userId}, 'IN_PROGRESS', 0, 0, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertAttempt(Map<String, Object> attempt);

    @Select("select count(*) from exam_attempt_questions where attempt_id = #{attemptId}")
    int countAttemptQuestions(@Param("attemptId") Long attemptId);

    @Insert("""
            insert into exam_attempt_questions (
              attempt_id, published_question_id, source_question_id, type, stem,
              analysis, score, sort_order, display_order
            )
            values (
              #{attemptId}, #{publishedQuestionId}, #{sourceQuestionId}, #{type}, #{stem},
              #{analysis}, #{score}, #{sortOrder}, #{displayOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertAttemptQuestion(Map<String, Object> question);

    @Insert("""
            insert into exam_attempt_options (attempt_question_id, option_label, content, is_correct, sort_order)
            select #{attemptQuestionId}, option_label, content, is_correct, sort_order
            from exam_published_options
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    void copyAttemptOptions(@Param("attemptQuestionId") Long attemptQuestionId, @Param("publishedQuestionId") Long publishedQuestionId);

    @Insert("""
            insert into exam_attempt_answer_labels (attempt_question_id, answer_label, sort_order)
            select #{attemptQuestionId}, answer_label, sort_order
            from exam_published_answer_labels
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    void copyAttemptAnswerLabels(@Param("attemptQuestionId") Long attemptQuestionId, @Param("publishedQuestionId") Long publishedQuestionId);

    @Insert("""
            insert into exam_attempt_attachments (attempt_question_id, file_name, file_url, media_type, sort_order)
            select #{attemptQuestionId}, file_name, file_url, media_type, sort_order
            from exam_published_attachments
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    void copyAttemptAttachments(@Param("attemptQuestionId") Long attemptQuestionId, @Param("publishedQuestionId") Long publishedQuestionId);

    @Select("""
            select aq.*,
                   ea.selected_labels as selectedLabels,
                   ea.answer_text as answerText
            from exam_attempt_questions aq
            left join exam_answers ea on ea.attempt_question_id = aq.id
            where aq.attempt_id = #{attemptId}
            order by aq.display_order, aq.id
            """)
    List<Map<String, Object>> findAttemptQuestions(@Param("attemptId") Long attemptId);

    @Select("""
            select id, option_label as label, content, sort_order as sortOrder
            from exam_attempt_options
            where attempt_question_id = #{attemptQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findAttemptOptions(@Param("attemptQuestionId") Long attemptQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_attempt_attachments
            where attempt_question_id = #{attemptQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findAttemptAttachments(@Param("attemptQuestionId") Long attemptQuestionId);

    @Select("""
            select option_label
            from exam_attempt_options
            where attempt_question_id = #{attemptQuestionId} and is_correct = true
            union
            select answer_label
            from exam_attempt_answer_labels
            where attempt_question_id = #{attemptQuestionId}
            order by option_label
            """)
    List<String> findAttemptCorrectLabels(@Param("attemptQuestionId") Long attemptQuestionId);

    @Insert("""
            insert into exam_answers (attempt_id, attempt_question_id, selected_labels, answer_text, is_correct, score)
            values (#{attemptId}, #{attemptQuestionId}, #{selectedLabels}, #{answerText}, #{correct}, #{score})
            on duplicate key update
              selected_labels = values(selected_labels),
              answer_text = values(answer_text),
              is_correct = values(is_correct),
              score = values(score),
              updated_at = current_timestamp
            """)
    void upsertAnswer(
            @Param("attemptId") Long attemptId,
            @Param("attemptQuestionId") Long attemptQuestionId,
            @Param("selectedLabels") String selectedLabels,
            @Param("answerText") String answerText,
            @Param("correct") boolean correct,
            @Param("score") BigDecimal score
    );

    @Insert("""
            insert into exam_answers (attempt_id, attempt_question_id, selected_labels, answer_text, is_correct, score)
            values (#{attemptId}, #{attemptQuestionId}, null, #{answerText}, null, 0)
            on duplicate key update
              answer_text = values(answer_text),
              updated_at = current_timestamp
            """)
    void upsertWritingAnswer(
            @Param("attemptId") Long attemptId,
            @Param("attemptQuestionId") Long attemptQuestionId,
            @Param("answerText") String answerText
    );

    @Select("select selected_labels from exam_answers where attempt_question_id = #{attemptQuestionId}")
    String findSelectedLabels(@Param("attemptQuestionId") Long attemptQuestionId);

    @Select("select answer_text from exam_answers where attempt_question_id = #{attemptQuestionId}")
    String findAnswerText(@Param("attemptQuestionId") Long attemptQuestionId);

    @Update("""
            update exam_attempts
            set status = 'SUBMITTED',
                submitted_at = #{submittedAt},
                total_score = #{totalScore},
                obtained_score = #{obtainedScore},
                duration_seconds = #{durationSeconds},
                updated_at = current_timestamp
            where id = #{attemptId} and status = 'IN_PROGRESS'
            """)
    int submitAttempt(
            @Param("attemptId") Long attemptId,
            @Param("submittedAt") LocalDateTime submittedAt,
            @Param("totalScore") BigDecimal totalScore,
            @Param("obtainedScore") BigDecimal obtainedScore,
            @Param("durationSeconds") int durationSeconds
    );
}
