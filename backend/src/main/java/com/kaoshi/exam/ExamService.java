package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.dto.AnswerSubmitItem;
import com.kaoshi.exam.dto.ExamAnswerSaveRequest;
import com.kaoshi.exam.dto.ExamPaperQuestionRequest;
import com.kaoshi.exam.dto.ExamPaperQuestionResponse;
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
import org.springframework.http.ResponseEntity;
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
    private static final Duration SUBMIT_GRACE = Duration.ofSeconds(30);

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
        replaceDraftQuestions(exam.getId(), request);
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
        replaceDraftQuestions(id, request);
        return detail(id);
    }

    @Transactional
    public ExamResponse publish(Long id) {
        Exam exam = findExam(id);
        List<Map<String, Object>> rules = examMapper.findExamRules(id);
        validatePublish(exam, rules);
        rebuildPublishedSnapshot(id);
        examMapper.updateExamStatus(id, "PUBLISHED");
        return detail(id);
    }

    @Transactional
    public ExamResponse close(Long id) {
        findExam(id);
        examMapper.updateExamStatus(id, "CLOSED");
        return detail(id);
    }

    @Transactional
    public ExamResponse copy(Long id) {
        Exam source = findExam(id);
        Exam target = new Exam();
        target.setTitle(source.getTitle() + " 副本");
        target.setDescription(source.getDescription());
        target.setQualifyScore(source.getQualifyScore());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setTimeLimit(source.getTimeLimit());
        target.setAttemptLimit(source.getAttemptLimit());
        target.setDisplayMode(source.getDisplayMode());
        target.setQuestionOrderMode(source.getQuestionOrderMode());
        target.setOpenType(source.getOpenType());
        target.setStatus("DRAFT");
        examMapper.insertExam(target);
        replaceExamDepartments(target.getId(), examMapper.findExamDepartmentIds(id));
        copyRules(id, target.getId());
        copyPaperQuestions(id, target.getId());
        return detail(target.getId());
    }

    public ResponseEntity<byte[]> download(Long id) {
        Exam exam = findExam(id);
        List<Map<String, Object>> rows = !examMapper.findPublishedQuestions(id).isEmpty()
                ? examMapper.findPublishedQuestions(id)
                : examMapper.findDraftQuestions(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试卷没有题目明细，无法下载");
        }
        return ExcelWorkbooks.template(
                exam.getTitle() + ".xlsx",
                "试卷",
                List.of("顺序", "题库", "题型", "分值", "题干", "正确答案", "解析", "选项"),
                rows.stream().map(this::paperExportRow).toList()
        );
    }

    @Transactional
    public ExamResponse revoke(Long id) {
        Exam exam = findExam(id);
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已发布考试可以撤销发布");
        }
        if (examMapper.countAttemptsByExam(id) > 0 || examMapper.countResultsByExam(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已有作答记录，不能撤销发布");
        }
        examMapper.deletePublishedAttachments(id);
        examMapper.deletePublishedOptions(id);
        examMapper.deletePublishedQuestions(id);
        examMapper.updateExamStatus(id, "DRAFT");
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
        ensureAttemptCanContinue(exam, attempt);
        return sessionResponse(exam, attempt);
    }

    @Transactional
    public ExamSessionResponse saveAnswer(Long examId, Long userId, ExamAnswerSaveRequest request) {
        Exam exam = findExam(examId);
        ensureExamAvailable(exam);
        ensureExamOpenToUser(exam, userId);
        Map<String, Object> attempt = findRunningAttempt(examId, userId);
        ensureAttemptCanContinue(exam, attempt);
        saveAnswerForAttempt(longValue(value(attempt, "id")), request.questionId(), request.selectedLabels(), false, BigDecimal.ZERO);
        return sessionResponse(exam, attempt);
    }

    @Transactional
    public ExamResultResponse submit(Long examId, Long userId, ExamSubmitRequest request) {
        Exam exam = findExam(examId);
        ensureExamAvailable(exam);
        ensureExamOpenToUser(exam, userId);
        for (AnswerSubmitItem answer : request.answers()) {
            normalizedLabels(answer.selectedLabels());
        }
        Map<String, Object> attempt = findRunningAttempt(examId, userId);
        if (isAttemptPastDeadline(exam, attempt, SUBMIT_GRACE)) {
            return gradeAndLockAttempt(exam, attempt);
        }
        Long attemptId = longValue(value(attempt, "id"));
        for (AnswerSubmitItem answer : request.answers()) {
            saveAnswerForAttempt(attemptId, answer.questionId(), answer.selectedLabels(), false, BigDecimal.ZERO);
        }
        return gradeAndLockAttempt(exam, attempt);
    }

    private ExamResultResponse gradeAndLockAttempt(Exam exam, Map<String, Object> attempt) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal obtainedScore = BigDecimal.ZERO;
        int correctCount = 0;
        Long attemptId = longValue(value(attempt, "id"));
        List<Map<String, Object>> questions = examMapper.findAttemptQuestions(attemptId);
        for (Map<String, Object> question : questions) {
            Long attemptQuestionId = longValue(value(question, "id"));
            BigDecimal questionScore = decimalValue(value(question, "score"));
            totalScore = totalScore.add(questionScore);
            List<String> selected = splitLabels(examMapper.findSelectedLabels(attemptQuestionId));
            List<String> correct = normalizedLabels(examMapper.findAttemptCorrectLabels(attemptQuestionId));
            boolean right = selected.equals(correct);
            BigDecimal score = right ? questionScore : BigDecimal.ZERO;
            if (right) {
                correctCount++;
                obtainedScore = obtainedScore.add(score);
            }
            examMapper.upsertAnswer(attemptId, attemptQuestionId, String.join(",", selected), right, score);
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
        result.put("correctCount", correctCount);
        result.put("questionCount", questions.size());
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
                correctCount,
                questions.size(),
                obtainedScore.compareTo(exam.getQualifyScore()) >= 0,
                submittedAt
        );
    }

    public List<ExamResultResponse> adminResults(Long examId) {
        return examMapper.findResults(examId).stream().map(this::toResultResponse).toList();
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
        if ("DEPARTMENT".equals(exam.getOpenType()) && examMapper.findExamDepartmentIds(exam.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门开放必须选择部门");
        }
        if (Boolean.TRUE.equals(exam.getTimeLimit()) && !exam.getEndTime().isAfter(exam.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试结束时间必须晚于开始时间");
        }
        for (Map<String, Object> rule : rules) {
            validateRuleAvailability(rule, SINGLE_CHOICE);
            validateRuleAvailability(rule, MULTIPLE_CHOICE);
        }
        List<Map<String, Object>> draftQuestions = examMapper.findDraftQuestions(exam.getId());
        if (draftQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试必须生成题目明细");
        }
        BigDecimal totalScore = calculatePaperScore(draftQuestions);
        int totalCount = draftQuestions.size();
        int activeCount = examMapper.findDraftQuestionsForPublish(exam.getId()).size();
        if (activeCount != totalCount) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目明细包含已停用试题");
        }
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试至少需要有效分值");
        }
        if (exam.getQualifyScore().compareTo(totalScore) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "及格分不能超过试卷总分");
        }
    }

    private void copyRules(Long sourceExamId, Long targetExamId) {
        for (Map<String, Object> source : examMapper.findExamRules(sourceExamId)) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("examId", targetExamId);
            rule.put("bankId", longValue(value(source, "bankId")));
            rule.put("singleCount", intValue(value(source, "singleCount")));
            rule.put("singleScore", decimalValue(value(source, "singleScore")));
            rule.put("multipleCount", intValue(value(source, "multipleCount")));
            rule.put("multipleScore", decimalValue(value(source, "multipleScore")));
            rule.put("sortOrder", intValue(value(source, "sortOrder")));
            examMapper.insertExamRule(rule);
        }
    }

    private void copyPaperQuestions(Long sourceExamId, Long targetExamId) {
        List<Map<String, Object>> sourceQuestions = examMapper.findDraftQuestions(sourceExamId);
        if (sourceQuestions.isEmpty()) {
            sourceQuestions = examMapper.findPublishedQuestions(sourceExamId);
        }
        for (Map<String, Object> source : sourceQuestions) {
            Long sourceQuestionId = value(source, "questionId") == null
                    ? longValue(value(source, "sourceQuestionId"))
                    : longValue(value(source, "questionId"));
            insertDraftQuestionFromSource(targetExamId, sourceQuestionId, decimalValue(value(source, "score")), intValue(value(source, "sortOrder")));
        }
    }

    private List<String> paperExportRow(Map<String, Object> row) {
        boolean publishedRow = row.containsKey("sourceQuestionId") || row.containsKey("source_question_id") || row.containsKey("SOURCE_QUESTION_ID");
        List<Map<String, Object>> options = publishedRow
                ? examMapper.findPublishedOptions(longValue(value(row, "id")))
                : examMapper.findDraftOptions(longValue(value(row, "id")));
        return List.of(
                String.valueOf(intValue(value(row, "sortOrder"))),
                safeString(value(row, "bankName")),
                questionTypeName(stringValue(value(row, "type"))),
                decimalValue(value(row, "score")).stripTrailingZeros().toPlainString(),
                safeString(value(row, "stem")),
                correctLabels(options),
                safeString(value(row, "analysis")),
                optionText(options)
        );
    }

    private String correctLabels(List<Map<String, Object>> options) {
        return options.stream()
                .filter(option -> booleanValue(value(option, "correct")))
                .map(option -> stringValue(value(option, "label")))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String optionText(List<Map<String, Object>> options) {
        return options.stream()
                .map(option -> stringValue(value(option, "label")) + ". " + stringValue(value(option, "content")))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String questionTypeName(String type) {
        return MULTIPLE_CHOICE.equals(type) ? "多选题" : "单选题";
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private BigDecimal calculatePaperScore(List<Map<String, Object>> paperQuestions) {
        return paperQuestions.stream()
                .map(question -> decimalValue(value(question, "score")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calculatePaperCount(List<Map<String, Object>> paperQuestions) {
        return paperQuestions.size();
    }

    private BigDecimal calculateRulePublishScore(List<Map<String, Object>> rules) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (Map<String, Object> rule : rules) {
            totalScore = totalScore.add(validateRuleAvailability(rule, SINGLE_CHOICE));
            totalScore = totalScore.add(validateRuleAvailability(rule, MULTIPLE_CHOICE));
        }
        return totalScore;
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

    private void replaceDraftQuestions(Long examId, ExamSaveRequest request) {
        examMapper.deleteDraftAttachments(examId);
        examMapper.deleteDraftOptions(examId);
        examMapper.deleteDraftQuestions(examId);
        List<ExamPaperQuestionRequest> paperQuestions = request.paperQuestions();
        if (paperQuestions != null && !paperQuestions.isEmpty()) {
            replaceDraftQuestionsFromRequest(examId, paperQuestions);
            return;
        }
        replaceDraftQuestionsFromRules(examId, request.rules());
    }

    private void replaceDraftQuestionsFromRequest(Long examId, List<ExamPaperQuestionRequest> paperQuestions) {
        Set<Long> questionIds = new HashSet<>();
        int sort = 10;
        for (ExamPaperQuestionRequest request : paperQuestions.stream()
                .sorted((left, right) -> left.sortOrder().compareTo(right.sortOrder()))
                .toList()) {
            if (!questionIds.add(request.questionId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目明细不能重复选择试题");
            }
            Map<String, Object> source = examMapper.findSourceQuestion(request.questionId());
            if (source == null || !"ACTIVE".equals(stringValue(value(source, "status")))) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目不存在或未启用");
            }
            insertDraftQuestionFromSource(examId, request.questionId(), request.score(), sort);
            sort += 10;
        }
    }

    private void replaceDraftQuestionsFromRules(Long examId, List<ExamRuleRequest> rules) {
        if (rules == null) {
            return;
        }
        int sort = 10;
        for (ExamRuleRequest rule : rules) {
            sort = draftQuestions(examId, rule.bankId(), SINGLE_CHOICE, rule.singleCount(), rule.singleScore(), sort);
            sort = draftQuestions(examId, rule.bankId(), MULTIPLE_CHOICE, rule.multipleCount(), rule.multipleScore(), sort);
        }
    }

    private int draftQuestions(Long examId, Long bankId, String type, int count, BigDecimal score, int sort) {
        if (count <= 0) {
            return sort;
        }
        for (Map<String, Object> source : examMapper.findQuestionsForPublish(bankId, type, count)) {
            insertDraftQuestionFromSource(examId, longValue(value(source, "questionId")), score, sort);
            sort += 10;
        }
        return sort;
    }

    private void insertDraftQuestionFromSource(Long examId, Long sourceQuestionId, BigDecimal score, int sortOrder) {
        Map<String, Object> source = examMapper.findSourceQuestion(sourceQuestionId);
        if (source == null || !"ACTIVE".equals(stringValue(value(source, "status")))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目不存在或未启用");
        }
        Map<String, Object> question = new HashMap<>();
        question.put("examId", examId);
        question.put("sourceQuestionId", sourceQuestionId);
        question.put("bankId", longValue(value(source, "bankId")));
        question.put("bankName", stringValue(value(source, "bankName")));
        question.put("type", stringValue(value(source, "type")));
        question.put("stem", stringValue(value(source, "stem")));
        question.put("analysis", stringValue(value(source, "analysis")));
        question.put("score", score);
        question.put("sortOrder", sortOrder);
        examMapper.insertDraftQuestion(question);
        Long draftQuestionId = longValue(value(question, "id"));
        examMapper.copyDraftOptionsFromSource(draftQuestionId, sourceQuestionId);
        examMapper.copyDraftAttachmentsFromSource(draftQuestionId, sourceQuestionId);
    }

    private void rebuildPublishedSnapshot(Long examId) {
        examMapper.deletePublishedAttachments(examId);
        examMapper.deletePublishedOptions(examId);
        examMapper.deletePublishedQuestions(examId);
        for (Map<String, Object> source : examMapper.findDraftQuestionsForPublish(examId)) {
            Map<String, Object> question = new HashMap<>();
            question.put("examId", examId);
            question.put("sourceQuestionId", longValue(value(source, "questionId")));
            question.put("type", stringValue(value(source, "type")));
            question.put("stem", stringValue(value(source, "stem")));
            question.put("analysis", stringValue(value(source, "analysis")));
            question.put("score", decimalValue(value(source, "score")));
            question.put("sortOrder", intValue(value(source, "sortOrder")));
            examMapper.insertPublishedQuestion(question);
            Long publishedQuestionId = longValue(value(question, "id"));
            examMapper.copyPublishedOptions(publishedQuestionId, longValue(value(source, "draftQuestionId")));
            examMapper.copyPublishedAttachments(publishedQuestionId, longValue(value(source, "draftQuestionId")));
        }
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

    private Map<String, Object> findRunningAttempt(Long examId, Long userId) {
        Map<String, Object> attempt = examMapper.findInProgressAttempt(examId, userId);
        if (attempt == null) {
            if (examMapper.countSubmittedAttempts(examId, userId) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "考试已提交，不能重复提交");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开始");
        }
        return attempt;
    }

    private void ensureAttemptCanContinue(Exam exam, Map<String, Object> attempt) {
        if (isAttemptPastDeadline(exam, attempt, Duration.ZERO)) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试时间已到，不能继续作答");
        }
    }

    private boolean isAttemptPastDeadline(Exam exam, Map<String, Object> attempt, Duration grace) {
        LocalDateTime startedAt = dateTimeValue(value(attempt, "startedAt"));
        LocalDateTime deadline = startedAt.plusMinutes(exam.getDurationMinutes()).plus(grace);
        return LocalDateTime.now().isAfter(deadline);
    }

    private void saveAnswerForAttempt(Long attemptId, Long questionId, List<String> selectedLabels, boolean correct, BigDecimal score) {
        Map<String, Object> attemptQuestion = examMapper.findAttemptQuestionBySource(attemptId, questionId);
        if (attemptQuestion == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目不属于本次作答");
        }
        List<String> selected = normalizedLabels(selectedLabels);
        examMapper.upsertAnswer(
                attemptId,
                longValue(value(attemptQuestion, "id")),
                String.join(",", selected),
                correct,
                score
        );
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
                splitLabels(stringValue(value(row, "selectedLabels"))),
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
        List<Map<String, Object>> paperQuestions = examMapper.findDraftQuestions(exam.getId());
        BigDecimal totalScore = calculateRuleScore(rules);
        int questionCount = calculateRuleCount(rules);
        if (!paperQuestions.isEmpty()) {
            totalScore = calculatePaperScore(paperQuestions);
            questionCount = calculatePaperCount(paperQuestions);
        }
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
                paperQuestions.stream().map(this::toPaperQuestionResponse).toList(),
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

    private ExamPaperQuestionResponse toPaperQuestionResponse(Map<String, Object> row) {
        return new ExamPaperQuestionResponse(
                longValue(value(row, "questionId")),
                longValue(value(row, "bankId")),
                stringValue(value(row, "bankName")),
                stringValue(value(row, "type")),
                stringValue(value(row, "stem")),
                decimalValue(value(row, "score")),
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
                stringValue(value(row, "username")),
                stringValue(value(row, "userName")),
                stringValue(value(row, "departmentName")),
                decimalValue(value(row, "totalScore")),
                decimalValue(value(row, "obtainedScore")),
                intValue(value(row, "correctCount")),
                intValue(value(row, "questionCount")),
                booleanValue(value(row, "passed")),
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
                stringValue(value(row, "username")),
                stringValue(value(row, "userName")),
                stringValue(value(row, "departmentName")),
                decimalValue(value(row, "totalScore")),
                decimalValue(value(row, "obtainedScore")),
                intValue(value(row, "correctCount")),
                intValue(value(row, "questionCount")),
                booleanValue(value(row, "passed")),
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
