package com.kaoshi.exam.mapper;

import com.kaoshi.exam.domain.Exam;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamMapper {
    @Select("select count(*) from exams where #{keyword} is null or title like #{keyword}")
    long countExams(@Param("keyword") String keyword);

    @Select("""
            select *
            from exams
            where #{keyword} is null or title like #{keyword}
            order by id desc
            limit #{size} offset #{offset}
            """)
    List<Exam> findExams(@Param("keyword") String keyword, @Param("size") int size, @Param("offset") int offset);

    @Select("select * from exams where id = #{id}")
    Exam findExamById(@Param("id") Long id);

    @Insert("""
            insert into exams (title, description, qualify_score, start_time, end_time, duration_minutes, time_limit, attempt_limit, display_mode, question_order_mode, open_type, status)
            values (#{title}, #{description}, #{qualifyScore}, #{startTime}, #{endTime}, #{durationMinutes}, #{timeLimit}, #{attemptLimit}, #{displayMode}, #{questionOrderMode}, #{openType}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertExam(Exam exam);

    @Update("""
            update exams
            set title = #{title},
                description = #{description},
                qualify_score = #{qualifyScore},
                start_time = #{startTime},
                end_time = #{endTime},
                duration_minutes = #{durationMinutes},
                time_limit = #{timeLimit},
                attempt_limit = #{attemptLimit},
                display_mode = #{displayMode},
                question_order_mode = #{questionOrderMode},
                open_type = #{openType},
                status = #{status},
                updated_at = current_timestamp
            where id = #{id}
            """)
    int updateExam(Exam exam);

    @Update("update exams set status = #{status}, updated_at = current_timestamp where id = #{examId}")
    int updateExamStatus(@Param("examId") Long examId, @Param("status") String status);

    @Select("select count(*) from exam_attempts where exam_id = #{examId}")
    int countAttemptsByExam(@Param("examId") Long examId);

    @Select("select count(*) from exam_results where exam_id = #{examId}")
    int countResultsByExam(@Param("examId") Long examId);

    @Select("select department_id from users where id = #{userId} and deleted_at is null")
    Long findUserDepartmentId(@Param("userId") Long userId);

    @Select("select count(*) from departments where id = #{departmentId} and status = 'ACTIVE'")
    int countActiveDepartmentById(@Param("departmentId") Long departmentId);

    @Delete("delete from exam_departments where exam_id = #{examId}")
    void deleteExamDepartments(@Param("examId") Long examId);

    @Insert("""
            insert into exam_departments (exam_id, department_id)
            values (#{examId}, #{departmentId})
            """)
    void insertExamDepartment(@Param("examId") Long examId, @Param("departmentId") Long departmentId);

    @Select("""
            select department_id
            from exam_departments
            where exam_id = #{examId}
            order by department_id
            """)
    List<Long> findExamDepartmentIds(@Param("examId") Long examId);

    @Select("select count(*) from question_banks where id = #{bankId} and status = 'ACTIVE'")
    int countActiveBankById(@Param("bankId") Long bankId);

    @Select("select name from question_banks where id = #{bankId}")
    String findBankName(@Param("bankId") Long bankId);

    @Delete("delete from exam_rules where exam_id = #{examId}")
    void deleteExamRules(@Param("examId") Long examId);

    @Insert("""
            insert into exam_rules (exam_id, bank_id, single_count, single_score, multiple_count, multiple_score, sort_order)
            values (#{examId}, #{bankId}, #{singleCount}, #{singleScore}, #{multipleCount}, #{multipleScore}, #{sortOrder})
            """)
    void insertExamRule(Map<String, Object> rule);

    @Select("""
            select *
            from exam_rules
            where exam_id = #{examId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findExamRules(@Param("examId") Long examId);

    @Select("""
            select count(*)
            from questions
            where bank_id = #{bankId}
              and type = #{type}
              and status = 'ACTIVE'
            """)
    int countActiveQuestions(@Param("bankId") Long bankId, @Param("type") String type);

    @Select("""
            select id as questionId, type, stem, analysis
            from questions
            where bank_id = #{bankId}
              and type = #{type}
              and status = 'ACTIVE'
            order by id
            limit #{limit}
            """)
    List<Map<String, Object>> findQuestionsForPublish(
            @Param("bankId") Long bankId,
            @Param("type") String type,
            @Param("limit") int limit
    );

    @Delete("delete from exam_draft_questions where exam_id = #{examId}")
    void deleteDraftQuestions(@Param("examId") Long examId);

    @Insert("""
            insert into exam_draft_questions (exam_id, source_question_id, bank_id, bank_name, type, stem, analysis, score, sort_order)
            values (#{examId}, #{sourceQuestionId}, #{bankId}, #{bankName}, #{type}, #{stem}, #{analysis}, #{score}, #{sortOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDraftQuestion(Map<String, Object> question);

    @Delete("""
            delete from exam_draft_attachments
            where draft_question_id in (
              select id from exam_draft_questions where exam_id = #{examId}
            )
            """)
    void deleteDraftAttachments(@Param("examId") Long examId);

    @Delete("""
            delete from exam_draft_options
            where draft_question_id in (
              select id from exam_draft_questions where exam_id = #{examId}
            )
            """)
    void deleteDraftOptions(@Param("examId") Long examId);

    @Insert("""
            insert into exam_draft_options (draft_question_id, option_label, content, is_correct, sort_order)
            select #{draftQuestionId}, option_label, content, is_correct, sort_order
            from question_options
            where question_id = #{sourceQuestionId}
            order by sort_order, id
            """)
    void copyDraftOptionsFromSource(@Param("draftQuestionId") Long draftQuestionId, @Param("sourceQuestionId") Long sourceQuestionId);

    @Insert("""
            insert into exam_draft_attachments (draft_question_id, file_name, file_url, media_type, sort_order)
            select #{draftQuestionId}, file_name, file_url, media_type, sort_order
            from question_attachments
            where question_id = #{sourceQuestionId}
            order by sort_order, id
            """)
    void copyDraftAttachmentsFromSource(@Param("draftQuestionId") Long draftQuestionId, @Param("sourceQuestionId") Long sourceQuestionId);

    @Select("""
            select edq.id,
                   edq.exam_id as examId,
                   edq.source_question_id as questionId,
                   edq.type,
                   edq.score,
                   edq.sort_order as sortOrder,
                   edq.bank_id as bankId,
                   edq.bank_name as bankName,
                   edq.stem,
                   edq.analysis,
                   q.status
            from exam_draft_questions edq
            left join questions q on q.id = edq.source_question_id
            where edq.exam_id = #{examId}
            order by edq.sort_order, edq.id
            """)
    List<Map<String, Object>> findDraftQuestions(@Param("examId") Long examId);

    @Select("""
            select q.id as questionId,
                   q.bank_id as bankId,
                   qb.name as bankName,
                   q.type,
                   q.stem,
                   q.analysis,
                   q.status
            from questions q
            join question_banks qb on qb.id = q.bank_id
            where q.id = #{questionId}
            """)
    Map<String, Object> findSourceQuestion(@Param("questionId") Long questionId);

    @Select("""
            select id, option_label as label, content, is_correct as correct, sort_order as sortOrder
            from question_options
            where question_id = #{questionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findSourceOptions(@Param("questionId") Long questionId);

    @Select("""
            select id, option_label as label, content, is_correct as correct, sort_order as sortOrder
            from exam_draft_options
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findDraftOptions(@Param("draftQuestionId") Long draftQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_draft_attachments
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findDraftAttachments(@Param("draftQuestionId") Long draftQuestionId);

    @Delete("""
            delete from exam_published_attachments
            where published_question_id in (
              select id from exam_published_questions where exam_id = #{examId}
            )
            """)
    void deletePublishedAttachments(@Param("examId") Long examId);

    @Delete("""
            delete from exam_published_options
            where published_question_id in (
              select id from exam_published_questions where exam_id = #{examId}
            )
            """)
    void deletePublishedOptions(@Param("examId") Long examId);

    @Delete("delete from exam_published_questions where exam_id = #{examId}")
    void deletePublishedQuestions(@Param("examId") Long examId);

    @Insert("""
            insert into exam_published_questions (exam_id, source_question_id, type, stem, analysis, score, sort_order)
            values (#{examId}, #{sourceQuestionId}, #{type}, #{stem}, #{analysis}, #{score}, #{sortOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPublishedQuestion(Map<String, Object> question);

    @Select("""
            select edq.id as draftQuestionId,
                   edq.source_question_id as questionId,
                   edq.type,
                   edq.stem,
                   edq.analysis,
                   edq.score,
                   edq.sort_order as sortOrder,
                   q.status
            from exam_draft_questions edq
            left join questions q on q.id = edq.source_question_id
            where edq.exam_id = #{examId}
              and q.status = 'ACTIVE'
            order by edq.sort_order, edq.id
            """)
    List<Map<String, Object>> findDraftQuestionsForPublish(@Param("examId") Long examId);

    @Insert("""
            insert into exam_published_options (published_question_id, option_label, content, is_correct, sort_order)
            select #{publishedQuestionId}, option_label, content, is_correct, sort_order
            from exam_draft_options
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    void copyPublishedOptions(@Param("publishedQuestionId") Long publishedQuestionId, @Param("draftQuestionId") Long draftQuestionId);

    @Insert("""
            insert into exam_published_attachments (published_question_id, file_name, file_url, media_type, sort_order)
            select #{publishedQuestionId}, file_name, file_url, media_type, sort_order
            from exam_draft_attachments
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    void copyPublishedAttachments(@Param("publishedQuestionId") Long publishedQuestionId, @Param("draftQuestionId") Long draftQuestionId);

    @Select("""
            select coalesce(sum(score), 0)
            from exam_published_questions
            where exam_id = #{examId}
            """)
    BigDecimal findPublishedTotalScore(@Param("examId") Long examId);

    @Select("select count(*) from exam_published_questions where exam_id = #{examId}")
    int countPublishedQuestions(@Param("examId") Long examId);

    @Select("""
            select epq.*,
                   qb.name as bankName
            from exam_published_questions epq
            left join questions q on q.id = epq.source_question_id
            left join question_banks qb on qb.id = q.bank_id
            where epq.exam_id = #{examId}
            order by epq.sort_order, epq.id
            """)
    List<Map<String, Object>> findPublishedQuestions(@Param("examId") Long examId);

    @Select("""
            select id, option_label as label, content, is_correct as correct, sort_order as sortOrder
            from exam_published_options
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findPublishedOptions(@Param("publishedQuestionId") Long publishedQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_published_attachments
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findPublishedAttachments(@Param("publishedQuestionId") Long publishedQuestionId);

    @Select("""
            select *
            from exams
            where status = 'PUBLISHED'
            order by start_time desc, id desc
            """)
    List<Exam> findPublishedExams();

    @Select("""
            select e.*
            from exams e
            where e.status = 'PUBLISHED'
              and (
                e.open_type = 'PUBLIC'
                or exists (
                  select 1
                  from exam_departments ed
                  where ed.exam_id = e.id
                    and ed.department_id = #{departmentId}
                )
              )
            order by e.start_time desc, e.id desc
            """)
    List<Exam> findPublishedExamsByDepartment(@Param("departmentId") Long departmentId);

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

    @Select("""
            select count(*)
            from exam_attempts
            where exam_id = #{examId}
              and user_id = #{userId}
              and status = 'SUBMITTED'
            """)
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
            insert into exam_attempt_questions (attempt_id, published_question_id, source_question_id, type, stem, analysis, score, sort_order, display_order)
            values (#{attemptId}, #{publishedQuestionId}, #{sourceQuestionId}, #{type}, #{stem}, #{analysis}, #{score}, #{sortOrder}, #{displayOrder})
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
            insert into exam_attempt_attachments (attempt_question_id, file_name, file_url, media_type, sort_order)
            select #{attemptQuestionId}, file_name, file_url, media_type, sort_order
            from exam_published_attachments
            where published_question_id = #{publishedQuestionId}
            order by sort_order, id
            """)
    void copyAttemptAttachments(@Param("attemptQuestionId") Long attemptQuestionId, @Param("publishedQuestionId") Long publishedQuestionId);

    @Select("""
            select aq.*,
                   ea.selected_labels as selectedLabels
            from exam_attempt_questions aq
            left join exam_answers ea on ea.attempt_question_id = aq.id
            where aq.attempt_id = #{attemptId}
            order by aq.display_order, aq.id
            """)
    List<Map<String, Object>> findAttemptQuestions(@Param("attemptId") Long attemptId);

    @Select("""
            select aq.*
            from exam_attempt_questions aq
            where aq.attempt_id = #{attemptId}
              and aq.source_question_id = #{questionId}
            """)
    Map<String, Object> findAttemptQuestionBySource(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Long questionId
    );

    @Select("""
            select id, option_label as label, content, sort_order as sortOrder
            from exam_attempt_options
            where attempt_question_id = #{attemptQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findAttemptOptions(@Param("attemptQuestionId") Long attemptQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_attempt_attachments
            where attempt_question_id = #{attemptQuestionId}
            order by sort_order, id
            """)
    List<Map<String, Object>> findAttemptAttachments(@Param("attemptQuestionId") Long attemptQuestionId);

    @Select("""
            select option_label
            from exam_attempt_options
            where attempt_question_id = #{attemptQuestionId} and is_correct = true
            order by option_label
            """)
    List<String> findAttemptCorrectLabels(@Param("attemptQuestionId") Long attemptQuestionId);

    @Insert("""
            insert into exam_answers (attempt_id, attempt_question_id, selected_labels, is_correct, score)
            values (#{attemptId}, #{attemptQuestionId}, #{selectedLabels}, #{correct}, #{score})
            on duplicate key update
              selected_labels = values(selected_labels),
              is_correct = values(is_correct),
              score = values(score),
              updated_at = current_timestamp
            """)
    void upsertAnswer(
            @Param("attemptId") Long attemptId,
            @Param("attemptQuestionId") Long attemptQuestionId,
            @Param("selectedLabels") String selectedLabels,
            @Param("correct") boolean correct,
            @Param("score") BigDecimal score
    );

    @Select("""
            select selected_labels
            from exam_answers
            where attempt_question_id = #{attemptQuestionId}
            """)
    String findSelectedLabels(@Param("attemptQuestionId") Long attemptQuestionId);

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

    @Insert("""
            insert into exam_results (attempt_id, exam_id, user_id, total_score, obtained_score, correct_count, question_count, submitted_at)
            values (#{attemptId}, #{examId}, #{userId}, #{totalScore}, #{obtainedScore}, #{correctCount}, #{questionCount}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertResult(Map<String, Object> result);

    @Select("""
            select r.*, e.title as examTitle,
                   u.username as username,
                   u.display_name as userName,
                   d.name as departmentName,
                   case when r.obtained_score >= e.qualify_score then true else false end as passed
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
                   case when r.obtained_score >= e.qualify_score then true else false end as passed
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
                   case when r.obtained_score >= e.qualify_score then true else false end as passed
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
                   ea.is_correct as correct
            from exam_attempt_questions aq
            join exam_answers ea on ea.attempt_question_id = aq.id
            where aq.attempt_id = #{attemptId}
            order by aq.display_order, aq.id
            """)
    List<Map<String, Object>> findResultQuestions(@Param("attemptId") Long attemptId);
}
