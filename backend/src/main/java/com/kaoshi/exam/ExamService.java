package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.dto.AnswerSubmitItem;
import com.kaoshi.exam.dto.ExamQuestionOptionResponse;
import com.kaoshi.exam.dto.ExamQuestionResponse;
import com.kaoshi.exam.dto.ExamResultDetailResponse;
import com.kaoshi.exam.dto.ExamResultQuestionResponse;
import com.kaoshi.exam.dto.ExamResponse;
import com.kaoshi.exam.dto.ExamResultResponse;
import com.kaoshi.exam.dto.ExamRuleRequest;
import com.kaoshi.exam.dto.ExamRuleResponse;
import com.kaoshi.exam.dto.ExamSaveRequest;
import com.kaoshi.exam.dto.ExamSessionResponse;
import com.kaoshi.exam.dto.ExamSubmitRequest;
import com.kaoshi.exam.mapper.ExamMapper;
import com.kaoshi.question.dto.QuestionAttachmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExamService {
    private static final String SINGLE_CHOICE = "SINGLE_CHOICE";
    private static final String MULTIPLE_CHOICE = "MULTIPLE_CHOICE";

    private final ExamMapper examMapper;

    public ExamService(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    public PageResponse<ExamResponse> page(PageRequest request) {
        long total = examMapper.countExams(request.keywordLike());
        List<ExamResponse> records = examMapper.findExams(request.keywordLike(), request.size(), request.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, total, request.page(), request.size());
    }

    public ExamResponse detail(Long id) {
        return toResponse(findExam(id));
    }

    @Transactional
    public ExamResponse create(ExamSaveRequest request) {
        validateDraft(request);
        Exam exam = new Exam();
        fillExam(exam, request, "DRAFT");
        examMapper.insertExam(exam);
        replaceExamDepartments(exam.getId(), request.departmentIds());
        replaceRules(exam.getId(), request.rules());
        return detail(exam.getId());
    }

    @Transactional
    public ExamResponse update(Long id, ExamSaveRequest request) {
        validateDraft(request);
        Exam exam = findExam(id);
        fillExam(exam, request, "DRAFT");
        examMapper.updateExam(exam);
        replaceExamDepartments(id, request.departmentIds());
        replaceRules(id, request.rules());
        return detail(id);
    }

    @Transactional
    public ExamResponse publish(Long id) {
        Exam exam = findExam(id);
        List<Map<String, Object>> rules = examMapper.findExamRules(id);
        validatePublish(exam, rules);
        rebuildPublishedSnapshot(id, rules);
        examMapper.updateExamStatus(id, "PUBLISHED");
        return detail(id);
    }

    @Transactional
    public ExamResponse close(Long id) {
        findExam(id);
        examMapper.updateExamStatus(id, "CLOSED");
        return detail(id);
    }

    public List<ExamResponse> publishedExams() {
        return examMapper.findPublishedExams().stream().map(this::toResponse).toList();
    }

    public List<ExamResponse> publishedExams(Long userId) {
        Long departmentId = examMapper.findUserDepartmentId(userId);
        if (departmentId == null) {
            return examMapper.findPublishedExams().stream()
                    .filter(exam -> "PUBLIC".equals(exam.getOpenType()))
                    .map(this::toResponse)
                    .toList();
        }
        return examMapper.findPublishedExamsByDepartment(departmentId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExamSessionResponse startExam(Long examId, Long userId) {
        Exam exam = findExam(examId);
        ensureExamAvailable(exam);
        ensureExamOpenToUser(exam, userId);
        Map<String, Object> attempt = examMapper.findInProgressAttempt(examId, userId);
        if (attempt == null) {
            ensureAttemptLimit(exam, userId);
            attempt = new HashMap<>();
            attempt.put("examId", examId);
            attempt.put("userId", userId);
            examMapper.insertAttempt(attempt);
            attempt = examMapper.findInProgressAttempt(examId, userId);
            createAttemptSnapshot(exam, longValue(value(attempt, "id")));
        } else if (examMapper.countAttemptQuestions(longValue(value(attempt, "id"))) == 0) {
            createAttemptSnapshot(exam, longValue(value(attempt, "id")));
        }
        return sessionResponse(exam, attempt);
    }

    @Transactional
    public ExamResultResponse submit(Long examId, Long userId, ExamSubmitRequest request) {
        Exam exam = findExam(examId);
        ensureExamAvailable(exam);
        ensureExamOpenToUser(exam, userId);
        Map<String, Object> attempt = examMapper.findInProgressAttempt(examId, userId);
        if (attempt == null) {
            if (examMapper.countSubmittedAttempts(examId, userId) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "考试已提交，不能重复提交");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开始");
        }

        Map<Long, AnswerSubmitItem> submitted = new HashMap<>();
        for (AnswerSubmitItem answer : request.answers()) {
            submitted.put(answer.questionId(), answer);
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal obtainedScore = BigDecimal.ZERO;
        int correctCount = 0;
        Long attemptId = longValue(value(attempt, "id"));
        List<Map<String, Object>> questions = examMapper.findAttemptQuestions(attemptId);
        for (Map<String, Object> question : questions) {
            Long attemptQuestionId = longValue(value(question, "id"));
            Long sourceQuestionId = longValue(value(question, "sourceQuestionId"));
            BigDecimal questionScore = decimalValue(value(question, "score"));
            totalScore = totalScore.add(questionScore);
            AnswerSubmitItem answer = submitted.get(sourceQuestionId);
            List<String> selected = answer == null ? List.of() : normalizedLabels(answer.selectedLabels());
            List<String> correct = normalizedLabels(examMapper.findAttemptCorrectLabels(attemptQuestionId));
            boolean right = selected.equals(correct);
            BigDecimal score = right ? questionScore : BigDecimal.ZERO;
            if (right) {
                correctCount++;
                obtainedScore = obtainedScore.add(score);
            }
            examMapper.insertAnswer(attemptId, attemptQuestionId, String.join(",", selected), right, score);
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        int durationSeconds = Math.max(0, (int) Duration.between(dateTimeValue(value(attempt, "startedAt")), submittedAt).toSeconds());
        int updated = examMapper.submitAttempt(attemptId, submittedAt, totalScore, obtainedScore, durationSeconds);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已提交，不能重复提交");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("examId", examId);
        result.put("userId", userId);
        result.put("totalScore", totalScore);
        result.put("obtainedScore", obtainedScore);
        result.put("correctCount", correctCount);
        result.put("questionCount", questions.size());
        result.put("submittedAt", submittedAt);
        examMapper.insertResult(result);
        return new ExamResultResponse(
                longValue(value(result, "id")),
                attemptId,
                examId,
                exam.getTitle(),
                userId,
                totalScore,
                obtainedScore,
                correctCount,
                questions.size(),
                submittedAt
        );
    }

    public List<ExamResultResponse> adminResults() {
        return examMapper.findResults().stream().map(this::toResultResponse).toList();
    }

    public List<ExamResultResponse> userResults(Long userId) {
        return examMapper.findResultsByUser(userId).stream().map(this::toResultResponse).toList();
    }

    public ExamResultDetailResponse adminResultDetail(Long resultId) {
        return toResultDetailResponse(findResult(resultId));
    }

    public ExamResultDetailResponse userResultDetail(Long resultId, Long userId) {
        Map<String, Object> result = findResult(resultId);
        if (!userId.equals(longValue(value(result, "userId")))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成绩不存在");
        }
        return toResultDetailResponse(result);
    }

    private Exam findExam(Long id) {
        Exam exam = examMapper.findExamById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private Map<String, Object> findResult(Long resultId) {
        Map<String, Object> result = examMapper.findResultById(resultId);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成绩不存在");
        }
        return result;
    }

    private void validateDraft(ExamSaveRequest request) {
        if (!List.of("PAGED", "ALL").contains(request.displayMode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目展示方式不合法");
        }
        if (!List.of("FIXED", "RANDOM").contains(request.questionOrderMode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目顺序规则不合法");
        }
        if (!List.of("PUBLIC", "DEPARTMENT").contains(request.openType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "开放范围不合法");
        }
        if (Boolean.TRUE.equals(request.timeLimit()) && !request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试结束时间必须晚于开始时间");
        }
        validateDepartments(request.departmentIds());
        validateRules(request.rules());
    }

    private void validatePublish(Exam exam, List<Map<String, Object>> rules) {
        if (rules.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试必须配置组卷规则");
        }
        if ("DEPARTMENT".equals(exam.getOpenType()) && examMapper.findExamDepartmentIds(exam.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门开放必须选择部门");
        }
        if (Boolean.TRUE.equals(exam.getTimeLimit()) && !exam.getEndTime().isAfter(exam.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试结束时间必须晚于开始时间");
        }
        BigDecimal totalScore = BigDecimal.ZERO;
        int totalCount = 0;
        for (Map<String, Object> rule : rules) {
            totalScore = totalScore.add(validateRuleAvailability(rule, SINGLE_CHOICE));
            totalScore = totalScore.add(validateRuleAvailability(rule, MULTIPLE_CHOICE));
            totalCount += intValue(value(rule, "singleCount")) + intValue(value(rule, "multipleCount"));
        }
        if (totalCount == 0 || totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试至少需要一道有效试题和有效分值");
        }
        if (exam.getQualifyScore().compareTo(totalScore) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "及格分不能超过试卷总分");
        }
    }

    private BigDecimal validateRuleAvailability(Map<String, Object> rule, String type) {
        Long bankId = longValue(value(rule, "bankId"));
        int count = SINGLE_CHOICE.equals(type) ? intValue(value(rule, "singleCount")) : intValue(value(rule, "multipleCount"));
        BigDecimal score = SINGLE_CHOICE.equals(type) ? decimalValue(value(rule, "singleScore")) : decimalValue(value(rule, "multipleScore"));
        if (count == 0 && score.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (count <= 0 || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题量和分值必须同时大于 0");
        }
        int available = examMapper.countActiveQuestions(bankId, type);
        if (count > available) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库题量不足：" + examMapper.findBankName(bankId));
        }
        return score.multiply(BigDecimal.valueOf(count));
    }

    private void validateDepartments(List<Long> departmentIds) {
        if (departmentIds == null) {
            return;
        }
        for (Long departmentId : departmentIds.stream().distinct().toList()) {
            if (examMapper.countActiveDepartmentById(departmentId) == 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门不存在或未启用");
            }
        }
    }

    private void validateRules(List<ExamRuleRequest> rules) {
        if (rules == null) {
            return;
        }
        Set<Long> bankIds = new HashSet<>();
        for (ExamRuleRequest rule : rules) {
            if (!bankIds.add(rule.bankId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不能重复选择题库");
            }
            if (examMapper.countActiveBankById(rule.bankId()) == 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库不存在或未启用");
            }
        }
    }

    private void fillExam(Exam exam, ExamSaveRequest request, String status) {
        exam.setTitle(request.title());
        exam.setDescription(request.description());
        exam.setQualifyScore(request.qualifyScore());
        exam.setStartTime(request.startTime());
        exam.setEndTime(request.endTime());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTimeLimit(request.timeLimit());
        exam.setAttemptLimit(request.attemptLimit());
        exam.setDisplayMode(request.displayMode());
        exam.setQuestionOrderMode(request.questionOrderMode());
        exam.setOpenType(request.openType());
        exam.setStatus(status);
    }

    private void replaceExamDepartments(Long examId, List<Long> departmentIds) {
        examMapper.deleteExamDepartments(examId);
        if (departmentIds == null) {
            return;
        }
        departmentIds.stream()
                .distinct()
                .forEach(departmentId -> examMapper.insertExamDepartment(examId, departmentId));
    }

    private void replaceRules(Long examId, List<ExamRuleRequest> rules) {
        examMapper.deleteExamRules(examId);
        if (rules == null) {
            return;
        }
        int sort = 10;
        for (ExamRuleRequest request : rules) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("examId", examId);
            rule.put("bankId", request.bankId());
            rule.put("singleCount", request.singleCount());
            rule.put("singleScore", request.singleScore());
            rule.put("multipleCount", request.multipleCount());
            rule.put("multipleScore", request.multipleScore());
            rule.put("sortOrder", sort);
            examMapper.insertExamRule(rule);
            sort += 10;
        }
    }

    private void rebuildPublishedSnapshot(Long examId, List<Map<String, Object>> rules) {
        examMapper.deletePublishedAttachments(examId);
        examMapper.deletePublishedOptions(examId);
        examMapper.deletePublishedQuestions(examId);
        int sort = 10;
        for (Map<String, Object> rule : rules) {
            sort = publishQuestions(examId, rule, SINGLE_CHOICE, sort);
            sort = publishQuestions(examId, rule, MULTIPLE_CHOICE, sort);
        }
    }

    private int publishQuestions(Long examId, Map<String, Object> rule, String type, int sort) {
        Long bankId = longValue(value(rule, "bankId"));
        int count = SINGLE_CHOICE.equals(type) ? intValue(value(rule, "singleCount")) : intValue(value(rule, "multipleCount"));
        BigDecimal score = SINGLE_CHOICE.equals(type) ? decimalValue(value(rule, "singleScore")) : decimalValue(value(rule, "multipleScore"));
        if (count <= 0) {
            return sort;
        }
        for (Map<String, Object> source : examMapper.findQuestionsForPublish(bankId, type, count)) {
            Map<String, Object> question = new HashMap<>();
            question.put("examId", examId);
            question.put("sourceQuestionId", longValue(value(source, "questionId")));
            question.put("type", stringValue(value(source, "type")));
            question.put("stem", stringValue(value(source, "stem")));
            question.put("analysis", stringValue(value(source, "analysis")));
            question.put("score", score);
            question.put("sortOrder", sort);
            examMapper.insertPublishedQuestion(question);
            Long publishedQuestionId = longValue(value(question, "id"));
            examMapper.copyPublishedOptions(publishedQuestionId, longValue(value(source, "questionId")));
            examMapper.copyPublishedAttachments(publishedQuestionId, longValue(value(source, "questionId")));
            sort += 10;
        }
        return sort;
    }

    private void ensureExamAvailable(Exam exam) {
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

    private void ensureExamOpenToUser(Exam exam, Long userId) {
        if ("PUBLIC".equals(exam.getOpenType())) {
            return;
        }
        Long departmentId = examMapper.findUserDepartmentId(userId);
        if (departmentId == null || !examMapper.findExamDepartmentIds(exam.getId()).contains(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "没有该考试权限");
        }
    }

    private void ensureAttemptLimit(Exam exam, Long userId) {
        if (exam.getAttemptLimit() == null) {
            return;
        }
        int submittedCount = examMapper.countSubmittedAttempts(exam.getId(), userId);
        if (submittedCount >= exam.getAttemptLimit()) {
            throw new BusinessException(ErrorCode.CONFLICT, "已达到本场考试可考次数");
        }
    }

    private void createAttemptSnapshot(Exam exam, Long attemptId) {
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
            examMapper.copyAttemptAttachments(attemptQuestionId, publishedQuestionId);
            displayOrder += 10;
        }
    }

    private ExamSessionResponse sessionResponse(Exam exam, Map<String, Object> attempt) {
        Long attemptId = longValue(value(attempt, "id"));
        return new ExamSessionResponse(
                exam.getId(),
                attemptId,
                exam.getTitle(),
                exam.getDurationMinutes(),
                exam.getDisplayMode(),
                dateTimeValue(value(attempt, "startedAt")),
                stringValue(value(attempt, "status")),
                examMapper.findAttemptQuestions(attemptId).stream()
                        .map(this::toExamQuestionResponse)
                        .toList()
        );
    }

    private ExamQuestionResponse toExamQuestionResponse(Map<String, Object> row) {
        Long attemptQuestionId = longValue(value(row, "id"));
        return new ExamQuestionResponse(
                longValue(value(row, "sourceQuestionId")),
                stringValue(value(row, "type")),
                stringValue(value(row, "stem")),
                decimalValue(value(row, "score")),
                intValue(value(row, "displayOrder")),
                examMapper.findAttemptAttachments(attemptQuestionId).stream().map(this::toAttachmentResponse).toList(),
                examMapper.findAttemptOptions(attemptQuestionId).stream().map(this::toOptionResponse).toList()
        );
    }

    private QuestionAttachmentResponse toAttachmentResponse(Map<String, Object> row) {
        return new QuestionAttachmentResponse(
                longValue(value(row, "id")),
                stringValue(value(row, "fileName")),
                stringValue(value(row, "fileUrl")),
                stringValue(value(row, "mediaType")),
                intValue(value(row, "sortOrder"))
        );
    }

    private ExamQuestionOptionResponse toOptionResponse(Map<String, Object> row) {
        return new ExamQuestionOptionResponse(
                longValue(value(row, "id")),
                stringValue(value(row, "label")),
                stringValue(value(row, "content")),
                intValue(value(row, "sortOrder"))
        );
    }

    private ExamResponse toResponse(Exam exam) {
        List<Map<String, Object>> rules = examMapper.findExamRules(exam.getId());
        BigDecimal totalScore = calculateRuleScore(rules);
        int questionCount = calculateRuleCount(rules);
        BigDecimal publishedScore = examMapper.findPublishedTotalScore(exam.getId());
        int publishedCount = examMapper.countPublishedQuestions(exam.getId());
        if (publishedCount > 0) {
            totalScore = publishedScore;
            questionCount = publishedCount;
        }
        return new ExamResponse(
                exam.getId(),
                totalScore,
                questionCount,
                exam.getTitle(),
                exam.getDescription(),
                exam.getQualifyScore(),
                exam.getStartTime(),
                exam.getEndTime(),
                exam.getDurationMinutes(),
                exam.getTimeLimit(),
                exam.getAttemptLimit(),
                exam.getDisplayMode(),
                exam.getQuestionOrderMode(),
                exam.getOpenType(),
                examMapper.findExamDepartmentIds(exam.getId()),
                rules.stream().map(this::toRuleResponse).toList(),
                exam.getStatus()
        );
    }

    private ExamRuleResponse toRuleResponse(Map<String, Object> row) {
        Long bankId = longValue(value(row, "bankId"));
        return new ExamRuleResponse(
                longValue(value(row, "id")),
                bankId,
                examMapper.findBankName(bankId),
                intValue(value(row, "singleCount")),
                decimalValue(value(row, "singleScore")),
                intValue(value(row, "multipleCount")),
                decimalValue(value(row, "multipleScore")),
                intValue(value(row, "sortOrder"))
        );
    }

    private BigDecimal calculateRuleScore(List<Map<String, Object>> rules) {
        return rules.stream()
                .map(rule -> decimalValue(value(rule, "singleScore")).multiply(BigDecimal.valueOf(intValue(value(rule, "singleCount"))))
                        .add(decimalValue(value(rule, "multipleScore")).multiply(BigDecimal.valueOf(intValue(value(rule, "multipleCount"))))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calculateRuleCount(List<Map<String, Object>> rules) {
        return rules.stream()
                .mapToInt(rule -> intValue(value(rule, "singleCount")) + intValue(value(rule, "multipleCount")))
                .sum();
    }

    private ExamResultResponse toResultResponse(Map<String, Object> row) {
        return new ExamResultResponse(
                longValue(value(row, "id")),
                longValue(value(row, "attemptId")),
                longValue(value(row, "examId")),
                stringValue(value(row, "examTitle")),
                longValue(value(row, "userId")),
                decimalValue(value(row, "totalScore")),
                decimalValue(value(row, "obtainedScore")),
                intValue(value(row, "correctCount")),
                intValue(value(row, "questionCount")),
                dateTimeValue(value(row, "submittedAt"))
        );
    }

    private ExamResultDetailResponse toResultDetailResponse(Map<String, Object> row) {
        Long attemptId = longValue(value(row, "attemptId"));
        return new ExamResultDetailResponse(
                longValue(value(row, "id")),
                attemptId,
                longValue(value(row, "examId")),
                stringValue(value(row, "examTitle")),
                longValue(value(row, "userId")),
                decimalValue(value(row, "totalScore")),
                decimalValue(value(row, "obtainedScore")),
                intValue(value(row, "correctCount")),
                intValue(value(row, "questionCount")),
                dateTimeValue(value(row, "submittedAt")),
                examMapper.findResultQuestions(attemptId).stream()
                        .map(this::toResultQuestionResponse)
                        .toList()
        );
    }

    private ExamResultQuestionResponse toResultQuestionResponse(Map<String, Object> row) {
        Long attemptQuestionId = longValue(value(row, "attemptQuestionId"));
        return new ExamResultQuestionResponse(
                longValue(value(row, "questionId")),
                stringValue(value(row, "type")),
                stringValue(value(row, "stem")),
                stringValue(value(row, "analysis")),
                decimalValue(value(row, "score")),
                decimalValue(value(row, "obtainedScore")),
                intValue(value(row, "sortOrder")),
                splitLabels(stringValue(value(row, "selectedLabels"))),
                normalizedLabels(examMapper.findAttemptCorrectLabels(attemptQuestionId)),
                booleanValue(value(row, "correct")),
                examMapper.findAttemptAttachments(attemptQuestionId).stream().map(this::toAttachmentResponse).toList(),
                examMapper.findAttemptOptions(attemptQuestionId).stream().map(this::toOptionResponse).toList()
        );
    }

    private List<String> normalizedLabels(List<String> labels) {
        return labels.stream()
                .map(String::trim)
                .filter(label -> !label.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> splitLabels(String labels) {
        if (labels == null || labels.isBlank()) {
            return List.of();
        }
        return normalizedLabels(List.of(labels.split(",")));
    }

    private Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String upperSnake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        if (row.containsKey(upperSnake)) {
            return row.get(upperSnake);
        }
        String lowerSnake = upperSnake.toLowerCase();
        if (row.containsKey(lowerSnake)) {
            return row.get(lowerSnake);
        }
        String upper = key.toUpperCase();
        if (row.containsKey(upper)) {
            return row.get(upper);
        }
        return row.get(key.toLowerCase());
    }

    private Integer intValue(Object value) {
        return ((Number) value).intValue();
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDateTime dateTimeValue(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }
}
