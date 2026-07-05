package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.dto.ExamPaperQuestionRequest;
import com.kaoshi.exam.dto.ExamRuleRequest;
import com.kaoshi.exam.dto.ExamSaveRequest;
import com.kaoshi.exam.dto.ExamMaterialRequest;
import com.kaoshi.exam.dto.ExamAnswerCardItemRequest;
import com.kaoshi.exam.mapper.ExamMapper;
import com.kaoshi.question.QuestionType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kaoshi.exam.ExamRowValues.decimalValue;
import static com.kaoshi.exam.ExamRowValues.intValue;
import static com.kaoshi.exam.ExamRowValues.longValue;
import static com.kaoshi.exam.ExamRowValues.normalizedLabels;
import static com.kaoshi.exam.ExamRowValues.splitLabels;
import static com.kaoshi.exam.ExamRowValues.stringValue;
import static com.kaoshi.exam.ExamRowValues.value;

final class ExamPaperWorkflow {
    private final ExamMapper examMapper;
    private final ExamResponseAssembler assembler;

    ExamPaperWorkflow(ExamMapper examMapper, ExamResponseAssembler assembler) {
        this.examMapper = examMapper;
        this.assembler = assembler;
    }

    void ensureExamEditableAsDraft(Exam exam) {
        if ("CLOSED".equals(exam.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已关闭，不能编辑草稿");
        }
        if (!"DRAFT".equals(exam.getStatus())
                && (examMapper.countAttemptsByExam(exam.getId()) > 0 || examMapper.countResultsByExam(exam.getId()) > 0)) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已有作答或成绩记录，不能改回草稿");
        }
    }

    void ensureExamPublishable(Exam exam) {
        if ("CLOSED".equals(exam.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已关闭，不能发布");
        }
        if ("PUBLISHED".equals(exam.getStatus())
                && (examMapper.countAttemptsByExam(exam.getId()) > 0 || examMapper.countResultsByExam(exam.getId()) > 0)) {
            throw new BusinessException(ErrorCode.CONFLICT, "考试已有作答或成绩记录，不能重新发布快照");
        }
    }

    void clearPublishedSnapshotIfPresent(Long examId) {
        if (examMapper.countPublishedQuestions(examId) == 0) {
            return;
        }
        examMapper.deletePublishedAttachments(examId);
        examMapper.deletePublishedOptions(examId);
        examMapper.deletePublishedAnswerLabels(examId);
        examMapper.deletePublishedQuestions(examId);
    }

    void validateDraft(ExamSaveRequest request) {
        if (!List.of("PAGED", "ALL").contains(request.displayMode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目展示方式不合法");
        }
        if (!List.of("FIXED", "RANDOM").contains(request.questionOrderMode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目顺序规则不合法");
        }
        if (!List.of("PUBLIC", "DEPARTMENT").contains(request.openType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "开放范围不合法");
        }
        if (!List.of("STRUCTURED", "ANSWER_SHEET").contains(request.examMode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试模式不合法");
        }
        if (Boolean.TRUE.equals(request.timeLimit()) && !request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试结束时间必须晚于开始时间");
        }
        validateDepartments(request.departmentIds());
        if ("ANSWER_SHEET".equals(request.examMode())) {
            validateAnswerSheet(request);
        } else {
            validateRules(request.rules());
        }
    }

    void validatePublish(Exam exam, List<Map<String, Object>> rules) {
        if ("DEPARTMENT".equals(exam.getOpenType()) && examMapper.findExamDepartmentIds(exam.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门开放必须选择部门");
        }
        if (Boolean.TRUE.equals(exam.getTimeLimit()) && !exam.getEndTime().isAfter(exam.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "考试结束时间必须晚于开始时间");
        }
        if ("ANSWER_SHEET".equals(exam.getExamMode())) {
            List<Map<String, Object>> items = examMapper.findExamAnswerCardItems(exam.getId());
            if (items.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答题卡试卷必须配置答题卡");
            }
            BigDecimal totalScore = items.stream()
                    .map(item -> decimalValue(value(item, "score")))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试至少需要有效分值");
            }
            if (exam.getQualifyScore().compareTo(totalScore) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "及格分不能超过试卷总分");
            }
            return;
        }
        for (Map<String, Object> rule : rules) {
            for (QuestionType type : QuestionType.ruleOrder()) {
                validateRuleAvailability(rule, type);
            }
        }
        List<Map<String, Object>> draftQuestions = examMapper.findDraftQuestions(exam.getId());
        if (draftQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试必须生成题目明细");
        }
        BigDecimal totalScore = assembler.calculatePaperScore(draftQuestions);
        int activeCount = examMapper.findDraftQuestionsForPublish(exam.getId()).size();
        if (activeCount != draftQuestions.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题目明细包含已停用试题");
        }
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "发布考试至少需要有效分值");
        }
        if (exam.getQualifyScore().compareTo(totalScore) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "及格分不能超过试卷总分");
        }
    }

    void fillExam(Exam exam, ExamSaveRequest request, String status) {
        exam.setTitle(request.title());
        exam.setDescription(request.description());
        exam.setQualifyScore(request.qualifyScore());
        exam.setStartTime(request.startTime());
        exam.setEndTime(request.endTime());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setTimeLimit(request.timeLimit());
        exam.setAttemptLimit(request.attemptLimit());
        exam.setExamMode(request.examMode());
        exam.setDisplayMode(request.displayMode());
        exam.setQuestionOrderMode(request.questionOrderMode());
        exam.setOpenType(request.openType());
        exam.setStatus(status);
    }

    void replaceExamDepartments(Long examId, List<Long> departmentIds) {
        examMapper.deleteExamDepartments(examId);
        if (departmentIds == null) {
            return;
        }
        departmentIds.stream()
                .distinct()
                .forEach(departmentId -> examMapper.insertExamDepartment(examId, departmentId));
    }

    void replaceRules(Long examId, List<ExamRuleRequest> rules) {
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
            rule.put("writingCount", request.writingCount() == null ? 0 : request.writingCount());
            rule.put("writingScore", request.writingScore() == null ? BigDecimal.ZERO : request.writingScore());
            rule.put("sortOrder", sort);
            examMapper.insertExamRule(rule);
            sort += 10;
        }
    }

    void replaceAnswerSheet(Long examId, List<ExamMaterialRequest> materials, List<ExamAnswerCardItemRequest> items) {
        examMapper.deleteExamMaterials(examId);
        examMapper.deleteExamAnswerCardItems(examId);
        int materialSort = 10;
        for (ExamMaterialRequest material : materials == null ? List.<ExamMaterialRequest>of() : materials) {
            Map<String, Object> row = new HashMap<>();
            row.put("examId", examId);
            row.put("title", material.title());
            row.put("description", material.description());
            row.put("fileName", material.fileName());
            row.put("fileUrl", material.fileUrl());
            row.put("mediaType", material.mediaType());
            row.put("sortOrder", material.sortOrder() == null ? materialSort : material.sortOrder());
            examMapper.insertExamMaterial(row);
            materialSort += 10;
        }
        int itemSort = 10;
        for (ExamAnswerCardItemRequest item : items == null ? List.<ExamAnswerCardItemRequest>of() : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("examId", examId);
            row.put("questionNo", item.questionNo());
            row.put("answerType", item.answerType());
            row.put("optionLabels", String.join(",", normalizedLabels(item.optionLabels())));
            row.put("correctLabels", String.join(",", normalizedLabels(item.correctLabels())));
            row.put("score", item.score());
            row.put("sortOrder", item.sortOrder() == null ? itemSort : item.sortOrder());
            examMapper.insertExamAnswerCardItem(row);
            itemSort += 10;
        }
    }

    void replaceDraftQuestions(Long examId, ExamSaveRequest request) {
        examMapper.deleteDraftAttachments(examId);
        examMapper.deleteDraftOptions(examId);
        examMapper.deleteDraftAnswerLabels(examId);
        examMapper.deleteDraftQuestions(examId);
        List<ExamPaperQuestionRequest> paperQuestions = request.paperQuestions();
        if ("ANSWER_SHEET".equals(request.examMode())) {
            return;
        }
        if (paperQuestions != null && !paperQuestions.isEmpty()) {
            replaceDraftQuestionsFromRequest(examId, paperQuestions);
            return;
        }
        replaceDraftQuestionsFromRules(examId, request.rules());
    }

    void copyRules(Long sourceExamId, Long targetExamId) {
        for (Map<String, Object> source : examMapper.findExamRules(sourceExamId)) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("examId", targetExamId);
            rule.put("bankId", longValue(value(source, "bankId")));
            rule.put("singleCount", intValue(value(source, "singleCount")));
            rule.put("singleScore", decimalValue(value(source, "singleScore")));
            rule.put("multipleCount", intValue(value(source, "multipleCount")));
            rule.put("multipleScore", decimalValue(value(source, "multipleScore")));
            rule.put("writingCount", intValue(value(source, "writingCount")));
            rule.put("writingScore", decimalValue(value(source, "writingScore")));
            rule.put("sortOrder", intValue(value(source, "sortOrder")));
            examMapper.insertExamRule(rule);
        }
    }

    void copyPaperQuestions(Long sourceExamId, Long targetExamId) {
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

    void rebuildPublishedSnapshot(Long examId) {
        examMapper.deletePublishedAttachments(examId);
        examMapper.deletePublishedOptions(examId);
        examMapper.deletePublishedAnswerLabels(examId);
        examMapper.deletePublishedQuestions(examId);
        for (Map<String, Object> source : examMapper.findDraftQuestionsForPublish(examId)) {
            Map<String, Object> question = new HashMap<>();
            question.put("examId", examId);
            question.put("sourceQuestionId", longValue(value(source, "questionId")));
            question.put("bankId", longValue(value(source, "bankId")));
            question.put("bankName", stringValue(value(source, "bankName")));
            question.put("type", stringValue(value(source, "type")));
            question.put("stem", stringValue(value(source, "stem")));
            question.put("analysis", stringValue(value(source, "analysis")));
            question.put("score", decimalValue(value(source, "score")));
            question.put("sortOrder", intValue(value(source, "sortOrder")));
            examMapper.insertPublishedQuestion(question);
            Long publishedQuestionId = longValue(value(question, "id"));
            examMapper.copyPublishedOptions(publishedQuestionId, longValue(value(source, "draftQuestionId")));
            examMapper.copyPublishedAnswerLabels(publishedQuestionId, longValue(value(source, "draftQuestionId")));
            examMapper.copyPublishedAttachments(publishedQuestionId, longValue(value(source, "draftQuestionId")));
        }
    }

    void rebuildAnswerSheetPublishedSnapshot(Long examId) {
        examMapper.deletePublishedAttachments(examId);
        examMapper.deletePublishedOptions(examId);
        examMapper.deletePublishedAnswerLabels(examId);
        examMapper.deletePublishedQuestions(examId);
        for (Map<String, Object> item : examMapper.findExamAnswerCardItems(examId)) {
            Map<String, Object> question = new HashMap<>();
            question.put("examId", examId);
            question.put("sourceQuestionId", 0L - longValue(value(item, "questionNo")));
            question.put("bankId", 0L);
            question.put("bankName", "答题卡");
            question.put("type", stringValue(value(item, "answerType")));
            question.put("stem", "第 " + intValue(value(item, "questionNo")) + " 题");
            question.put("analysis", null);
            question.put("score", decimalValue(value(item, "score")));
            question.put("sortOrder", intValue(value(item, "sortOrder")));
            examMapper.insertPublishedQuestion(question);
            Long publishedQuestionId = longValue(value(question, "id"));
            List<String> labels = splitLabels(stringValue(value(item, "optionLabels")));
            List<String> correctLabels = splitLabels(stringValue(value(item, "correctLabels")));
            int sort = 10;
            for (String label : labels) {
                Map<String, Object> option = new HashMap<>();
                option.put("publishedQuestionId", publishedQuestionId);
                option.put("optionLabel", label);
                option.put("content", label);
                option.put("correct", correctLabels.contains(label));
                option.put("sortOrder", sort);
                examMapper.insertPublishedOption(option);
                sort += 10;
            }
            sort = 10;
            for (String label : correctLabels) {
                Map<String, Object> answer = new HashMap<>();
                answer.put("publishedQuestionId", publishedQuestionId);
                answer.put("answerLabel", label);
                answer.put("sortOrder", sort);
                examMapper.insertPublishedAnswerLabel(answer);
                sort += 10;
            }
        }
    }

    private BigDecimal validateRuleAvailability(Map<String, Object> rule, QuestionType type) {
        Long bankId = longValue(value(rule, "bankId"));
        int count = ruleCount(rule, type);
        BigDecimal score = ruleScore(rule, type);
        if (count == 0 && score.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (count <= 0 || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题量和分值必须同时大于 0");
        }
        int available = examMapper.countActiveQuestions(bankId, type.code());
        if (count > available) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库题量不足：" + examMapper.findBankName(bankId));
        }
        return score.multiply(BigDecimal.valueOf(count));
    }

    private int ruleCount(Map<String, Object> rule, QuestionType type) {
        if (type == QuestionType.SINGLE_CHOICE) {
            return intValue(value(rule, "singleCount"));
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            return intValue(value(rule, "multipleCount"));
        }
        return intValue(value(rule, "writingCount"));
    }

    private BigDecimal ruleScore(Map<String, Object> rule, QuestionType type) {
        if (type == QuestionType.SINGLE_CHOICE) {
            return decimalValue(value(rule, "singleScore"));
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            return decimalValue(value(rule, "multipleScore"));
        }
        return decimalValue(value(rule, "writingScore"));
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

    private void validateAnswerSheet(ExamSaveRequest request) {
        if (request.materials() == null || request.materials().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答题卡试卷必须上传或引用试卷材料");
        }
        if (request.answerCardItems() == null || request.answerCardItems().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答题卡试卷必须配置答题卡");
        }
        Set<Integer> questionNos = new HashSet<>();
        for (ExamAnswerCardItemRequest item : request.answerCardItems()) {
            if (!questionNos.add(item.questionNo())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "答题卡题号不能重复");
            }
            QuestionType type = QuestionType.require(item.answerType());
            if (type.optionBased()) {
                if (item.optionLabels() == null || item.optionLabels().size() < 2) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "选择题至少需要两个选项");
                }
                validateAnswerCardLabels(type, normalizedLabels(item.optionLabels()), normalizedLabels(item.correctLabels()));
            }
        }
    }

    private void validateAnswerCardLabels(QuestionType type, List<String> optionLabels, List<String> correctLabels) {
        if (type.singleAnswer() && correctLabels.size() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "单选题必须且只能有一个正确答案");
        }
        if (QuestionType.MULTIPLE_CHOICE == type && correctLabels.size() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "多选题至少需要两个正确答案");
        }
        for (String label : correctLabels) {
            if (!optionLabels.contains(label)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "正确答案引用了不存在的选项：" + label);
            }
        }
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
            sort = draftQuestions(examId, rule.bankId(), QuestionType.SINGLE_CHOICE.code(), rule.singleCount(), rule.singleScore(), sort);
            sort = draftQuestions(examId, rule.bankId(), QuestionType.MULTIPLE_CHOICE.code(), rule.multipleCount(), rule.multipleScore(), sort);
            sort = draftQuestions(
                    examId,
                    rule.bankId(),
                    QuestionType.WRITING.code(),
                    rule.writingCount() == null ? 0 : rule.writingCount(),
                    rule.writingScore() == null ? BigDecimal.ZERO : rule.writingScore(),
                    sort
            );
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
        examMapper.copyDraftAnswerLabelsFromSource(draftQuestionId, sourceQuestionId);
        examMapper.copyDraftAttachmentsFromSource(draftQuestionId, sourceQuestionId);
    }

}
