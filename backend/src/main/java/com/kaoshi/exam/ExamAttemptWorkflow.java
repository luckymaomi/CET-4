package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.dto.AnswerSubmitItem;
import com.kaoshi.exam.mapper.ExamMapper;
import com.kaoshi.question.QuestionType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaoshi.exam.ExamRowValues.dateTimeValue;
import static com.kaoshi.exam.ExamRowValues.decimalValue;
import static com.kaoshi.exam.ExamRowValues.intValue;
import static com.kaoshi.exam.ExamRowValues.longValue;
import static com.kaoshi.exam.ExamRowValues.normalizedLabels;
import static com.kaoshi.exam.ExamRowValues.stringValue;
import static com.kaoshi.exam.ExamRowValues.value;

final class ExamAttemptWorkflow {
    private final ExamMapper examMapper;

    ExamAttemptWorkflow(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    void ensureExamAvailable(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试未发布");
        }
        if (examMapper.countPublishedQuestions(exam.getId()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试尚未生成发布快照");
        }
        if (Boolean.TRUE.equals(exam.getTimeLimit()) && (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime()))) {
            throw new BusinessException(ErrorCode.CONFLICT, "不在考试时间内");
        }
    }

    void ensureExamOpenToUser(Exam exam, Long userId) {
        if ("PUBLIC".equals(exam.getOpenType())) {
            return;
        }
        Long departmentId = examMapper.findUserDepartmentId(userId);
        if (departmentId == null || !examMapper.findExamDepartmentIds(exam.getId()).contains(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "没有该考试权限");
        }
    }

    void ensureAttemptLimit(Exam exam, Long userId) {
        if (exam.getAttemptLimit() == null) {
            return;
        }
        int submittedCount = examMapper.countSubmittedAttempts(exam.getId(), userId);
        if (submittedCount >= exam.getAttemptLimit()) {
            throw new BusinessException(ErrorCode.CONFLICT, "已达到本场考试可考次数");
        }
    }

    Map<String, Object> findRunningAttempt(Long examId, Long userId) {
        Map<String, Object> attempt = examMapper.findInProgressAttempt(examId, userId);
        if (attempt == null) {
            if (examMapper.countSubmittedAttempts(examId, userId) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "考试已提交，不能重复提交");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开始");
        }
        return attempt;
    }

    void ensureAttemptCanContinue(Exam exam, Map<String, Object> attempt) {
        if (isAttemptPastDeadline(exam, attempt, Duration.ZERO)) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试时间已到，不能继续作答");
        }
    }

    boolean isAttemptPastDeadline(Exam exam, Map<String, Object> attempt, Duration grace) {
        LocalDateTime startedAt = dateTimeValue(value(attempt, "startedAt"));
        LocalDateTime deadline = startedAt.plusMinutes(exam.getDurationMinutes()).plus(grace);
        return LocalDateTime.now().isAfter(deadline);
    }

    void saveAnswerSnapshotForAttempt(Long attemptId, List<AnswerSubmitItem> answers) {
        List<Map<String, Object>> attemptQuestions = examMapper.findAttemptQuestions(attemptId);
        if (answers.size() != attemptQuestions.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答案快照必须包含本次作答的全部题目");
        }
        Map<Long, AnswerSubmitItem> answersByQuestionId = new HashMap<>();
        for (AnswerSubmitItem answer : answers) {
            AnswerSubmitItem previous = answersByQuestionId.put(answer.questionId(), answer);
            if (previous != null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答案快照存在重复题目");
            }
        }
        for (Map<String, Object> attemptQuestion : attemptQuestions) {
            Long questionId = longValue(value(attemptQuestion, "sourceQuestionId"));
            AnswerSubmitItem answer = answersByQuestionId.remove(questionId);
            if (answer == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答案快照缺少本次作答题目");
            }
            saveAnswerForAttemptQuestion(attemptId, attemptQuestion, answer.selectedLabels(), answer.answerText(), false, BigDecimal.ZERO);
        }
        if (!answersByQuestionId.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答案快照包含不属于本次作答的题目");
        }
    }

    void createAttemptSnapshot(Exam exam, Long attemptId) {
        List<Map<String, Object>> questions = new ArrayList<>(examMapper.findPublishedQuestions(exam.getId()));
        if ("RANDOM".equals(exam.getQuestionOrderMode())) {
            Collections.shuffle(questions);
        }
        int displayOrder = 10;
        for (Map<String, Object> source : questions) {
            Map<String, Object> question = new HashMap<>();
            question.put("attemptId", attemptId);
            question.put("publishedQuestionId", longValue(value(source, "id")));
            question.put("sourceQuestionId", longValue(value(source, "sourceQuestionId")));
            question.put("type", stringValue(value(source, "type")));
            question.put("stem", stringValue(value(source, "stem")));
            question.put("analysis", stringValue(value(source, "analysis")));
            question.put("score", decimalValue(value(source, "score")));
            question.put("sortOrder", intValue(value(source, "sortOrder")));
            question.put("displayOrder", displayOrder);
            examMapper.insertAttemptQuestion(question);
            Long attemptQuestionId = longValue(value(question, "id"));
            Long publishedQuestionId = longValue(value(source, "id"));
            examMapper.copyAttemptOptions(attemptQuestionId, publishedQuestionId);
            examMapper.copyAttemptAnswerLabels(attemptQuestionId, publishedQuestionId);
            examMapper.copyAttemptAttachments(attemptQuestionId, publishedQuestionId);
            displayOrder += 10;
        }
    }

    private void saveAnswerForAttemptQuestion(Long attemptId, Map<String, Object> attemptQuestion, List<String> selectedLabels, String answerText, boolean correct, BigDecimal score) {
        if (QuestionType.require(stringValue(value(attemptQuestion, "type"))).manualReview()) {
            examMapper.upsertWritingAnswer(
                    attemptId,
                    longValue(value(attemptQuestion, "id")),
                    answerText == null ? "" : answerText
            );
            return;
        }
        List<String> selected = normalizedLabels(selectedLabels);
        examMapper.upsertAnswer(
                attemptId,
                longValue(value(attemptQuestion, "id")),
                String.join(",", selected),
                null,
                correct,
                score
        );
    }
}
