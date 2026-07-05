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

interface ExamResultMapper {
    @Insert("""
            insert into exam_results (attempt_id, exam_id, user_id, total_score, obtained_score, objective_score, subjective_score, correct_count, question_count, grading_status, submitted_at)
            values (#{attemptId}, #{examId}, #{userId}, #{totalScore}, #{obtainedScore}, #{objectiveScore}, #{subjectiveScore}, #{correctCount}, #{questionCount}, #{gradingStatus}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertResult(Map<String, Object> result);

    @Select("""
            select r.*, e.title as examTitle,
                   u.username as username,
                   u.display_name as userName,
                   d.name as departmentName,
                   case when r.grading_status = 'FINAL' and r.obtained_score >= e.qualify_score then true else false end as passed
            from exam_results r
            join exams e on e.id = r.exam_id
            join users u on u.id = r.user_id
            left join departments d on d.id = u.department_id
            where #{examId} is null or r.exam_id = #{examId}
            order by r.id desc
            """)
    List<Map<String, Object>> findResults(@Param("examId") Long examId);

    @Select("""
            select r.*, e.title as examTitle,
                   u.username as username,
                   u.display_name as userName,
                   d.name as departmentName,
                   case when r.grading_status = 'FINAL' and r.obtained_score >= e.qualify_score then true else false end as passed
            from exam_results r
            join exams e on e.id = r.exam_id
            join users u on u.id = r.user_id
            left join departments d on d.id = u.department_id
            where r.user_id = #{userId}
            order by r.id desc
            """)
    List<Map<String, Object>> findResultsByUser(@Param("userId") Long userId);

    @Select("""
            select r.*, e.title as examTitle,
                   u.username as username,
                   u.display_name as userName,
                   d.name as departmentName,
                   case when r.grading_status = 'FINAL' and r.obtained_score >= e.qualify_score then true else false end as passed
            from exam_results r
            join exams e on e.id = r.exam_id
            join users u on u.id = r.user_id
            left join departments d on d.id = u.department_id
            where r.id = #{resultId}
            """)
    Map<String, Object> findResultById(@Param("resultId") Long resultId);

    @Select("""
            select aq.id as attemptQuestionId,
                   aq.source_question_id as questionId,
                   aq.type as type,
                   aq.stem as stem,
                   aq.analysis as analysis,
                   aq.score as score,
                   ea.score as obtainedScore,
                   aq.display_order as sortOrder,
                   ea.selected_labels as selectedLabels,
                   ea.answer_text as answerText,
                   ea.is_correct as correct,
                   ea.review_comment as reviewComment,
                   ea.reviewed_at as reviewedAt,
                   reviewer.display_name as reviewerName
            from exam_attempt_questions aq
            join exam_answers ea on ea.attempt_question_id = aq.id
            left join users reviewer on reviewer.id = ea.reviewed_by
            where aq.attempt_id = #{attemptId}
            order by aq.display_order, aq.id
            """)
    List<Map<String, Object>> findResultQuestions(@Param("attemptId") Long attemptId);

    @Select("""
            select aq.*, ea.id as answerId, ea.score as answerScore, ea.answer_text as answerText
            from exam_results r
            join exam_attempt_questions aq on aq.attempt_id = r.attempt_id
            left join exam_answers ea on ea.attempt_question_id = aq.id
            where r.id = #{resultId}
              and aq.source_question_id = #{questionId}
            """)
    Map<String, Object> findResultQuestionForReview(
            @Param("resultId") Long resultId,
            @Param("questionId") Long questionId
    );

    @Update("""
            update exam_answers
            set score = #{score},
                review_comment = #{comment},
                reviewed_by = #{reviewerId},
                reviewed_at = #{reviewedAt},
                updated_at = current_timestamp
            where attempt_question_id = #{attemptQuestionId}
            """)
    int updateWritingReview(
            @Param("attemptQuestionId") Long attemptQuestionId,
            @Param("score") BigDecimal score,
            @Param("comment") String comment,
            @Param("reviewerId") Long reviewerId,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    @Select("""
            select count(*)
            from exam_attempt_questions aq
            join exam_results r on r.attempt_id = aq.attempt_id
            join exam_answers ea on ea.attempt_question_id = aq.id
            where r.id = #{resultId}
              and aq.type = 'WRITING'
              and ea.reviewed_at is null
            """)
    int countPendingWritingReviews(@Param("resultId") Long resultId);

    @Select("""
            select count(*)
            from exam_attempt_questions aq
            join exam_results r on r.attempt_id = aq.attempt_id
            where r.id = #{resultId}
              and aq.type = 'WRITING'
            """)
    int countWritingQuestionsByResult(@Param("resultId") Long resultId);

    @Select("""
            select coalesce(sum(case when aq.type <> 'WRITING' then ea.score else 0 end), 0)
            from exam_attempt_questions aq
            join exam_results r on r.attempt_id = aq.attempt_id
            join exam_answers ea on ea.attempt_question_id = aq.id
            where r.id = #{resultId}
            """)
    BigDecimal sumObjectiveScore(@Param("resultId") Long resultId);

    @Select("""
            select coalesce(sum(case when aq.type = 'WRITING' then ea.score else 0 end), 0)
            from exam_attempt_questions aq
            join exam_results r on r.attempt_id = aq.attempt_id
            join exam_answers ea on ea.attempt_question_id = aq.id
            where r.id = #{resultId}
            """)
    BigDecimal sumSubjectiveScore(@Param("resultId") Long resultId);

    @Select("""
            select count(*)
            from exam_attempt_questions aq
            join exam_results r on r.attempt_id = aq.attempt_id
            join exam_answers ea on ea.attempt_question_id = aq.id
            where r.id = #{resultId}
              and aq.type <> 'WRITING'
              and ea.is_correct = true
            """)
    int countCorrectObjectiveAnswers(@Param("resultId") Long resultId);

    @Update("""
            update exam_results
            set obtained_score = #{obtainedScore},
                objective_score = #{objectiveScore},
                subjective_score = #{subjectiveScore},
                correct_count = #{correctCount},
                grading_status = #{gradingStatus},
                reviewed_at = #{reviewedAt}
            where id = #{resultId}
            """)
    int updateResultAfterReview(
            @Param("resultId") Long resultId,
            @Param("obtainedScore") BigDecimal obtainedScore,
            @Param("objectiveScore") BigDecimal objectiveScore,
            @Param("subjectiveScore") BigDecimal subjectiveScore,
            @Param("correctCount") int correctCount,
            @Param("gradingStatus") String gradingStatus,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );
}
