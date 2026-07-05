package com.kaoshi.question.mapper;

import com.kaoshi.question.domain.Question;
import com.kaoshi.question.domain.QuestionAttachment;
import com.kaoshi.question.domain.QuestionOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QuestionMapper {
    String QUESTION_FIELDS = """
            q.id,
            q.bank_id,
            q.type,
            q.stem,
            q.analysis,
            q.difficulty,
            q.status
            """;

    @Select("""
            select count(*)
            from questions q
            join question_banks b on b.id = q.bank_id
            where (#{bankId} is null or q.bank_id = #{bankId})
              and (#{keyword} is null or q.stem like #{keyword} or b.name like #{keyword})
            """)
    long countQuestions(@Param("bankId") Long bankId, @Param("keyword") String keyword);

    @Select("""
            select
            """ + QUESTION_FIELDS + """
            from questions q
            join question_banks b on b.id = q.bank_id
            where (#{bankId} is null or q.bank_id = #{bankId})
              and (#{keyword} is null or q.stem like #{keyword} or b.name like #{keyword})
            order by q.id desc
            limit #{size} offset #{offset}
            """)
    List<Question> findQuestions(
            @Param("bankId") Long bankId,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Select("""
            select
            """ + QUESTION_FIELDS + """
            from questions q
            where q.id = #{id}
            """)
    Question findQuestionById(@Param("id") Long id);

    @Select("select name from question_banks where id = #{id}")
    String findBankName(@Param("id") Long id);

    @Select("select name from question_banks where status = 'ACTIVE' order by id")
    List<String> findActiveBankNames();

    @Select("select id from question_banks where name = #{name}")
    Long findBankIdByName(@Param("name") String name);

    @Select("select count(*) from question_banks where id = #{id}")
    long countBankById(@Param("id") Long id);

    @Insert("""
            insert into questions (
              bank_id, type, stem, analysis, difficulty, status
            )
            values (
              #{bankId}, #{type}, #{stem}, #{analysis}, #{difficulty}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertQuestion(Question question);

    @Update("""
            update questions
            set bank_id = #{bankId},
                type = #{type},
                stem = #{stem},
                analysis = #{analysis},
                difficulty = #{difficulty},
                status = #{status},
                updated_at = current_timestamp
            where id = #{id}
            """)
    int updateQuestion(Question question);

    @Select("""
            select id, question_id, option_label, content, is_correct as correct, sort_order
            from question_options
            where question_id = #{questionId}
            order by sort_order, id
            """)
    List<QuestionOption> findOptions(@Param("questionId") Long questionId);

    @Insert("""
            insert into question_options (question_id, option_label, content, is_correct, sort_order)
            values (#{questionId}, #{optionLabel}, #{content}, #{correct}, #{sortOrder})
            """)
    void insertOption(QuestionOption option);

    @Delete("delete from question_options where question_id = #{questionId}")
    void deleteOptions(@Param("questionId") Long questionId);

    @Delete("delete from question_answer_labels where question_id = #{questionId}")
    void deleteAnswerLabels(@Param("questionId") Long questionId);

    @Insert("""
            insert into question_answer_labels (question_id, answer_label, sort_order)
            values (#{questionId}, #{answerLabel}, #{sortOrder})
            """)
    void insertAnswerLabel(
            @Param("questionId") Long questionId,
            @Param("answerLabel") String answerLabel,
            @Param("sortOrder") int sortOrder
    );

    @Select("""
            select answer_label
            from question_answer_labels
            where question_id = #{questionId}
            order by sort_order, id
            """)
    List<String> findAnswerLabels(@Param("questionId") Long questionId);

    @Select("select * from question_attachments where question_id = #{questionId} order by sort_order, id")
    List<QuestionAttachment> findAttachments(@Param("questionId") Long questionId);

    @Insert("""
            insert into question_attachments (question_id, file_name, file_url, media_type, sort_order)
            values (#{questionId}, #{fileName}, #{fileUrl}, #{mediaType}, #{sortOrder})
            """)
    void insertAttachment(
            @Param("questionId") Long questionId,
            @Param("fileName") String fileName,
            @Param("fileUrl") String fileUrl,
            @Param("mediaType") String mediaType,
            @Param("sortOrder") int sortOrder
    );

    @Delete("delete from question_attachments where question_id = #{questionId}")
    void deleteAttachments(@Param("questionId") Long questionId);
}
