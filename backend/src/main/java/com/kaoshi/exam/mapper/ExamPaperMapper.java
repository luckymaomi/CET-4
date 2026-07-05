package com.kaoshi.exam.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

interface ExamPaperMapper {
    @Select("""
            select q.id as questionId,
                   q.bank_id as bankId,
                   b.name as bankName,
                   q.type,
                   q.stem,
                   q.analysis,
                   q.status
            from questions q
            join question_banks b on b.id = q.bank_id
            where q.bank_id = #{bankId}
              and q.type = #{type}
              and q.status = 'ACTIVE'
            order by q.id
            limit #{limit}
            """)
    List<Map<String, Object>> findQuestionsForPublish(
            @Param("bankId") Long bankId,
            @Param("type") String type,
            @Param("limit") int limit
    );

    @Delete("delete from exam_draft_attachments where draft_question_id in (select id from exam_draft_questions where exam_id = #{examId})")
    void deleteDraftAttachments(@Param("examId") Long examId);

    @Delete("delete from exam_draft_options where draft_question_id in (select id from exam_draft_questions where exam_id = #{examId})")
    void deleteDraftOptions(@Param("examId") Long examId);

    @Delete("delete from exam_draft_answer_labels where draft_question_id in (select id from exam_draft_questions where exam_id = #{examId})")
    void deleteDraftAnswerLabels(@Param("examId") Long examId);

    @Delete("delete from exam_draft_questions where exam_id = #{examId}")
    void deleteDraftQuestions(@Param("examId") Long examId);

    @Insert("""
            insert into exam_draft_questions (
              exam_id, source_question_id, bank_id, bank_name, type, stem, analysis, score, sort_order
            )
            values (
              #{examId}, #{sourceQuestionId}, #{bankId}, #{bankName}, #{type}, #{stem}, #{analysis}, #{score}, #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertDraftQuestion(Map<String, Object> question);

    @Insert("""
            insert into exam_draft_options (draft_question_id, option_label, content, is_correct, sort_order)
            select #{draftQuestionId}, option_label, content, is_correct, sort_order
            from question_options
            where question_id = #{sourceQuestionId}
            order by sort_order, id
            """)
    void copyDraftOptionsFromSource(@Param("draftQuestionId") Long draftQuestionId, @Param("sourceQuestionId") Long sourceQuestionId);

    @Insert("""
            insert into exam_draft_answer_labels (draft_question_id, answer_label, sort_order)
            select #{draftQuestionId}, answer_label, sort_order
            from question_answer_labels
            where question_id = #{sourceQuestionId}
            order by sort_order, id
            """)
    void copyDraftAnswerLabelsFromSource(@Param("draftQuestionId") Long draftQuestionId, @Param("sourceQuestionId") Long sourceQuestionId);

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
            order by sortOrder, id
            """)
    List<Map<String, Object>> findSourceOptions(@Param("questionId") Long questionId);

    @Select("""
            select id, option_label as label, content, is_correct as correct, sort_order as sortOrder
            from exam_draft_options
            where draft_question_id = #{draftQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findDraftOptions(@Param("draftQuestionId") Long draftQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_draft_attachments
            where draft_question_id = #{draftQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findDraftAttachments(@Param("draftQuestionId") Long draftQuestionId);

    @Delete("delete from exam_published_attachments where published_question_id in (select id from exam_published_questions where exam_id = #{examId})")
    void deletePublishedAttachments(@Param("examId") Long examId);

    @Delete("delete from exam_published_options where published_question_id in (select id from exam_published_questions where exam_id = #{examId})")
    void deletePublishedOptions(@Param("examId") Long examId);

    @Delete("delete from exam_published_answer_labels where published_question_id in (select id from exam_published_questions where exam_id = #{examId})")
    void deletePublishedAnswerLabels(@Param("examId") Long examId);

    @Delete("delete from exam_published_questions where exam_id = #{examId}")
    void deletePublishedQuestions(@Param("examId") Long examId);

    @Delete("delete from exam_departments where exam_id = #{examId}")
    void deleteAllExamDepartments(@Param("examId") Long examId);

    @Insert("""
            insert into exam_published_questions (
              exam_id, source_question_id, bank_id, bank_name, type, stem, analysis, score, sort_order
            )
            values (
              #{examId}, #{sourceQuestionId}, #{bankId}, #{bankName}, #{type}, #{stem}, #{analysis}, #{score}, #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPublishedQuestion(Map<String, Object> question);

    @Insert("""
            insert into exam_published_options (published_question_id, option_label, content, is_correct, sort_order)
            values (#{publishedQuestionId}, #{optionLabel}, #{content}, #{correct}, #{sortOrder})
            """)
    void insertPublishedOption(Map<String, Object> option);

    @Insert("""
            insert into exam_published_answer_labels (published_question_id, answer_label, sort_order)
            values (#{publishedQuestionId}, #{answerLabel}, #{sortOrder})
            """)
    void insertPublishedAnswerLabel(Map<String, Object> label);

    @Select("""
            select edq.id as draftQuestionId,
                   edq.source_question_id as questionId,
                   edq.bank_id as bankId,
                   edq.bank_name as bankName,
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
            insert into exam_published_answer_labels (published_question_id, answer_label, sort_order)
            select #{publishedQuestionId}, answer_label, sort_order
            from exam_draft_answer_labels
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    void copyPublishedAnswerLabels(@Param("publishedQuestionId") Long publishedQuestionId, @Param("draftQuestionId") Long draftQuestionId);

    @Insert("""
            insert into exam_published_attachments (published_question_id, file_name, file_url, media_type, sort_order)
            select #{publishedQuestionId}, file_name, file_url, media_type, sort_order
            from exam_draft_attachments
            where draft_question_id = #{draftQuestionId}
            order by sort_order, id
            """)
    void copyPublishedAttachments(@Param("publishedQuestionId") Long publishedQuestionId, @Param("draftQuestionId") Long draftQuestionId);

    @Select("select coalesce(sum(score), 0) from exam_published_questions where exam_id = #{examId}")
    BigDecimal findPublishedTotalScore(@Param("examId") Long examId);

    @Select("select count(*) from exam_published_questions where exam_id = #{examId}")
    int countPublishedQuestions(@Param("examId") Long examId);

    @Select("""
            select epq.id,
                   epq.exam_id as examId,
                   epq.source_question_id as sourceQuestionId,
                   epq.bank_id as bankId,
                   epq.bank_name as bankName,
                   epq.type,
                   epq.stem,
                   epq.analysis,
                   epq.score,
                   epq.sort_order as sortOrder
            from exam_published_questions epq
            where epq.exam_id = #{examId}
            order by epq.sort_order, epq.id
            """)
    List<Map<String, Object>> findPublishedQuestions(@Param("examId") Long examId);

    @Select("""
            select id, option_label as label, content, is_correct as correct, sort_order as sortOrder
            from exam_published_options
            where published_question_id = #{publishedQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findPublishedOptions(@Param("publishedQuestionId") Long publishedQuestionId);

    @Select("""
            select id, file_name as fileName, file_url as fileUrl, media_type as mediaType, sort_order as sortOrder
            from exam_published_attachments
            where published_question_id = #{publishedQuestionId}
            order by sortOrder, id
            """)
    List<Map<String, Object>> findPublishedAttachments(@Param("publishedQuestionId") Long publishedQuestionId);
}
