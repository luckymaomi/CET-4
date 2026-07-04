package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.dto.ExamResultResponse;
import com.kaoshi.exam.dto.WritingReviewRequest;
import com.kaoshi.exam.mapper.ExamMapper;
import com.kaoshi.question.QuestionType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaoshi.exam.ExamRowValues.dateTimeValue;
import static com.kaoshi.exam.ExamRowValues.decimalValue;
import static com.kaoshi.exam.ExamRowValues.longValue;
import static com.kaoshi.exam.ExamRowValues.normalizedLabels;
import static com.kaoshi.exam.ExamRowValues.splitLabels;
import static com.kaoshi.exam.ExamRowValues.stringValue;
import static com.kaoshi.exam.ExamRowValues.value;

final class ExamGradingWorkflow {
    static final String GRADING_PENDING_REVIEW = "PENDING_REVIEW";
    static final String GRADING_FINAL = "FINAL";

    private final ExamMapper examMapper;

    ExamGradingWorkflow(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    ExamResultResponse gradeAndLockAttempt(Exam exam, Map<String, Object> attempt) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal obtainedScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;
        int correctCount = 0;
        boolean hasWriting = false;
        Long attemptId = longValue(value(attempt, "id"));
        List<Map<String, Object>> questions = examMapper.findAttemptQuestions(attemptId);
        for (Map<String, Object> question : questions) {
            Long attemptQuestionId = longValue(value(question, "id"));
            BigDecimal questionScore = decimalValue(value(question, "score"));
            totalScore = totalScore.add(questionScore);
            if (QuestionType.require(stringValue(value(question, "type"))).manualReview()) {
                hasWriting = true;
                String answerText = examMapper.findAnswerText(attemptQuestionId);
                examMapper.upsertWritingAnswer(attemptId, attemptQuestionId, answerText == null ? "" : answerText);
                continue;
            }
            List<String> selected = splitLabels(examMapper.findSelectedLabels(attemptQuestionId));
            List<String> correct = normalizedLabels(examMapper.findAttemptCorrectLabels(attemptQuestionId));
            boolean right = selected.equals(correct);
            BigDecimal score = right ? questionScore : BigDecimal.ZERO;
            if (right) {
                correctCount++;
                obtainedScore = obtainedScore.add(score);
                objectiveScore = objectiveScore.add(score);
            }
            examMapper.upsertAnswer(attemptId, attemptQuestionId, String.join(",", selected), null, right, score);
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        int durationSeconds = Math.max(0, (int) Duration.between(dateTimeValue(value(attempt, "startedAt")), submittedAt).toSeconds());
        int updated = examMapper.submitAttempt(attemptId, submittedAt, totalScore, obtainedScore, durationSeconds);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已提交，不能重复提交");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("examId", exam.getId());
        result.put("userId", longValue(value(attempt, "userId")));
        result.put("totalScore", totalScore);
        result.put("obtainedScore", obtainedScore);
        result.put("objectiveScore", objectiveScore);
        result.put("subjectiveScore", BigDecimal.ZERO);
        result.put("correctCount", correctCount);
        result.put("questionCount", questions.size());
        result.put("gradingStatus", hasWriting ? GRADING_PENDING_REVIEW : GRADING_FINAL);
        result.put("submittedAt", submittedAt);
        examMapper.insertResult(result);
        return new ExamResultResponse(
                longValue(value(result, "id")),
                attemptId,
                exam.getId(),
                exam.getTitle(),
                longValue(value(attempt, "userId")),
                null,
                null,
                null,
                totalScore,
                obtainedScore,
                objectiveScore,
                BigDecimal.ZERO,
                correctCount,
                questions.size(),
                hasWriting ? GRADING_PENDING_REVIEW : GRADING_FINAL,
                !hasWriting && obtainedScore.compareTo(exam.getQualifyScore()) >= 0,
                submittedAt
        );
    }

    void reviewWriting(Long resultId, Long questionId, Long reviewerId, WritingReviewRequest request, Map<String, Object> result) {
        if (GRADING_FINAL.equals(stringValue(value(result, "gradingStatus")))) {
            throw new BusinessException(ErrorCode.CONFLICT, "成绩已完成阅卷，不能再次保存评分");
        }
        Map<String, Object> question = examMapper.findResultQuestionForReview(resultId, questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成绩题目不存在");
        }
        if (!QuestionType.require(stringValue(value(question, "type"))).manualReview()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "只有写作题需要人工阅卷");
        }
        BigDecimal maxScore = decimalValue(value(question, "score"));
        if (request.score().compareTo(maxScore) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "写作题得分不能超过题目分值");
        }
        LocalDateTime reviewedAt = LocalDateTime.now();
        int updated = examMapper.updateWritingReview(
                longValue(value(question, "id")),
                request.score(),
                request.comment(),
                reviewerId,
                reviewedAt
        );
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "写作题阅卷保存失败");
        }
        recalculateResult(resultId, GRADING_PENDING_REVIEW, null);
    }

    void completeReview(Long resultId, Map<String, Object> result) {
        if (GRADING_FINAL.equals(stringValue(value(result, "gradingStatus")))) {
            throw new BusinessException(ErrorCode.CONFLICT, "成绩已完成阅卷，不能重复完成阅卷");
        }
        if (examMapper.countWritingQuestionsByResult(resultId) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "该成绩没有需要人工阅卷的写作题");
        }
        if (examMapper.countPendingWritingReviews(resultId) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "仍有写作题未完成评分");
        }
        recalculateResult(resultId, GRADING_FINAL, LocalDateTime.now());
    }

    private void recalculateResult(Long resultId, String gradingStatus, LocalDateTime reviewedAt) {
        BigDecimal objectiveScore = examMapper.sumObjectiveScore(resultId);
        BigDecimal subjectiveScore = examMapper.sumSubjectiveScore(resultId);
        BigDecimal obtainedScore = objectiveScore.add(subjectiveScore);
        int correctCount = examMapper.countCorrectObjectiveAnswers(resultId);
        examMapper.updateResultAfterReview(
                resultId,
                obtainedScore,
                objectiveScore,
                subjectiveScore,
                correctCount,
                gradingStatus,
                reviewedAt
        );
    }
}
