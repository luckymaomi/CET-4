package com.kaoshi.question;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelImportResult;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.question.domain.Question;
import com.kaoshi.question.domain.QuestionAttachment;
import com.kaoshi.question.domain.QuestionOption;
import com.kaoshi.question.dto.QuestionAttachmentRequest;
import com.kaoshi.question.dto.QuestionAttachmentResponse;
import com.kaoshi.question.dto.QuestionOptionRequest;
import com.kaoshi.question.dto.QuestionOptionResponse;
import com.kaoshi.question.dto.QuestionResponse;
import com.kaoshi.question.dto.QuestionSaveRequest;
import com.kaoshi.question.mapper.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    public PageResponse<QuestionResponse> page(Long bankId, PageRequest request) {
        long total = questionMapper.countQuestions(bankId, request.keywordLike());
        List<QuestionResponse> records = questionMapper
                .findQuestions(bankId, request.keywordLike(), request.size(), request.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, total, request.page(), request.size());
    }

    public QuestionResponse detail(Long id) {
        return toResponse(findQuestion(id));
    }

    public ResponseEntity<byte[]> template() {
        return ExcelWorkbooks.template(
                "试题导入模板.xlsx",
                List.of(
                        new ExcelWorkbooks.SheetData(
                                "试题导入",
                                List.of("题库名称", "题型", "难度", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"),
                                List.of(
                                        List.of("英语基础题库", "单选", "简单", "选择正确的主谓一致句子。", "He go to school.", "He goes to school.", "He going to school.", "He gone to school.", "B", "第三人称单数主语后动词使用 goes。"),
                                        List.of("英语基础题库", "多选", "简单", "下列哪些单词是名词？", "book", "quickly", "teacher", "beautiful", "AC", "book 和 teacher 是名词。"),
                                        List.of("英语基础题库", "单选", "困难", "“提高”的英文最贴近哪一项？", "practice", "improve", "listen", "repeat", "B", "improve 表示提高、改善。"),
                                        List.of("英语基础题库", "多选", "困难", "哪些行为有助于英语听力训练？", "每天听英文材料", "只背中文释义", "跟读音频", "完全不复习", "AC", "持续听音频和跟读能训练听力输入与语音识别。"),
                                        List.of("英语基础题库", "写作", "困难", "Write an essay about the importance of lifelong learning.", "", "", "", "", "", "写作题不填写选项和正确答案，可在解析中填写写作要求或参考思路。")
                                )
                        ),
                        new ExcelWorkbooks.SheetData(
                                "字典清单",
                                List.of("字段", "可用值"),
                                List.of(
                                        List.of("题库名称", String.join("、", questionMapper.findActiveBankNames())),
                                        List.of("题型", "单选、 多选、 写作"),
                                        List.of("难度", "简单、 困难"),
                                        List.of("正确答案", "单选填写 A/B/C/D；多选填写 AC/BCD，不使用逗号；写作题留空")
                                )
                        )
                )
        );
    }

    @Transactional
    public ExcelImportResult importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || ExcelWorkbooks.text(row, 3).isBlank()) {
                    continue;
                }
                try {
                    create(rowToRequest(row));
                    success++;
                } catch (RuntimeException exception) {
                    errors.add("第 " + (rowIndex + 1) + " 行：" + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "读取 Excel 失败");
        }
        return new ExcelImportResult(success, errors.size(), errors);
    }

    @Transactional
    public QuestionResponse create(QuestionSaveRequest request) {
        validateQuestion(request);
        Question question = new Question();
        fillQuestion(question, request);
        questionMapper.insertQuestion(question);
        replaceOptions(question.getId(), request.options());
        replaceAttachments(question.getId(), request.attachments());
        return detail(question.getId());
    }

    private QuestionSaveRequest rowToRequest(Row row) {
        String bankName = ExcelWorkbooks.text(row, 0).trim();
        if (bankName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库名称不能为空");
        }
        Long bankId = questionMapper.findBankIdByName(bankName);
        if (bankId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库不存在：" + bankName);
        }
        String type = normalizeType(ExcelWorkbooks.text(row, 1).trim());
        String difficulty = normalizeDifficulty(ExcelWorkbooks.text(row, 2).trim());
        String stem = ExcelWorkbooks.text(row, 3).trim();
        if (stem.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题干不能为空");
        }
        List<QuestionOptionRequest> options = new ArrayList<>();
        if (QuestionType.require(type).optionBased()) {
            for (int index = 0; index < 4; index++) {
                String label = String.valueOf((char) ('A' + index));
                String content = ExcelWorkbooks.text(row, 4 + index).trim();
                if (!content.isBlank()) {
                    options.add(new QuestionOptionRequest(label, content, false));
                }
            }
            Set<String> correctLabels = parseCorrectLabels(ExcelWorkbooks.text(row, 8));
            if (ExcelWorkbooks.text(row, 4).trim().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "选项A不能为空");
            }
            for (String label : correctLabels) {
                int index = label.charAt(0) - 'A';
                if (index < 0 || index > 3 || ExcelWorkbooks.text(row, 4 + index).trim().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "正确答案引用的选项不能为空：" + label);
                }
            }
            options = options.stream()
                    .map(option -> new QuestionOptionRequest(option.label(), option.content(), correctLabels.contains(option.label())))
                    .toList();
        }
        return new QuestionSaveRequest(
                bankId,
                type,
                stem,
                ExcelWorkbooks.text(row, 9).trim(),
                difficulty,
                "ACTIVE",
                options,
                List.of()
        );
    }

    private String normalizeType(String value) {
        if ("单选".equals(value) || "单选题".equals(value) || QuestionType.SINGLE_CHOICE.code().equals(value)) {
            return QuestionType.SINGLE_CHOICE.code();
        }
        if ("多选".equals(value) || "多选题".equals(value) || QuestionType.MULTIPLE_CHOICE.code().equals(value)) {
            return QuestionType.MULTIPLE_CHOICE.code();
        }
        if ("写作".equals(value) || "写作题".equals(value) || QuestionType.WRITING.code().equals(value)) {
            return QuestionType.WRITING.code();
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题类型不支持：" + value);
    }

    private String normalizeDifficulty(String value) {
        if (value.isBlank() || "简单".equals(value)) {
            return "EASY";
        }
        if ("困难".equals(value)) {
            return "HARD";
        }
        if (List.of("EASY", "HARD").contains(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题难度不合法：" + value);
    }

    private Set<String> parseCorrectLabels(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (normalized.contains(",") || normalized.contains("，")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "正确答案请使用 AC，不要使用 A,C");
        }
        Set<String> labels = normalized.chars()
                .mapToObj(character -> String.valueOf((char) character))
                .filter(label -> !label.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (labels.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "正确答案不能为空");
        }
        for (String label : labels) {
            if (!List.of("A", "B", "C", "D").contains(label)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "正确答案只能使用 A、B、C、D");
            }
        }
        return labels;
    }

    public ResponseEntity<byte[]> exportExcel() {
        List<List<String>> rows = questionMapper.findQuestions(null, null, 10000, 0)
                .stream()
                .map(this::exportRow)
                .toList();
        return ExcelWorkbooks.template(
                "试题导出.xlsx",
                List.of(
                        new ExcelWorkbooks.SheetData(
                                "试题导出",
                                List.of("题库名称", "题型", "难度", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"),
                                rows
                        ),
                        new ExcelWorkbooks.SheetData(
                                "字典清单",
                                List.of("字段", "可用值"),
                                List.of(
                                        List.of("题库名称", String.join("、", questionMapper.findActiveBankNames())),
                                        List.of("题型", "单选、 多选、 写作"),
                                        List.of("难度", "简单、 困难"),
                                        List.of("正确答案", "单选填写 A/B/C/D；多选填写 AC/BCD，不使用逗号；写作题留空")
                                )
                        )
                )
        );
    }

    private List<String> exportRow(Question question) {
        List<QuestionOption> options = questionMapper.findOptions(question.getId());
        String answer = options.stream()
                .filter(QuestionOption::getCorrect)
                .map(QuestionOption::getOptionLabel)
                .collect(Collectors.joining());
        return List.of(
                questionMapper.findBankName(question.getBankId()),
                questionTypeName(question.getType()),
                "HARD".equals(question.getDifficulty()) ? "困难" : "简单",
                question.getStem(),
                optionContent(options, "A"),
                optionContent(options, "B"),
                optionContent(options, "C"),
                optionContent(options, "D"),
                answer,
                question.getAnalysis() == null ? "" : question.getAnalysis()
        );
    }

    private String optionContent(List<QuestionOption> options, String label) {
        return options.stream()
                .filter(option -> label.equals(option.getOptionLabel()))
                .map(QuestionOption::getContent)
                .findFirst()
                .orElse("");
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionSaveRequest request) {
        validateQuestion(request);
        Question question = findQuestion(id);
        fillQuestion(question, request);
        questionMapper.updateQuestion(question);
        replaceOptions(id, request.options());
        replaceAttachments(id, request.attachments());
        return detail(id);
    }

    private Question findQuestion(Long id) {
        Question question = questionMapper.findQuestionById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        return question;
    }

    private void validateQuestion(QuestionSaveRequest request) {
        if (questionMapper.countBankById(request.bankId()) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库不存在");
        }
        QuestionType type = QuestionType.require(request.type());
        if (!List.of("EASY", "HARD").contains(request.difficulty())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题难度不合法");
        }
        if (!List.of("ACTIVE", "DISABLED").contains(request.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题状态不合法");
        }
        List<QuestionOptionRequest> options = request.options() == null ? List.of() : request.options();
        if (!type.optionBased()) {
            if (!options.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "写作题不需要选项");
            }
            return;
        }
        if (options.size() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题至少需要两个选项");
        }
        Set<String> labels = new HashSet<>();
        for (QuestionOptionRequest option : options) {
            if (!labels.add(option.label())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "选项标签不能重复");
            }
        }
        long correctCount = options.stream().filter(QuestionOptionRequest::correct).count();
        if (QuestionType.SINGLE_CHOICE.code().equals(request.type()) && correctCount != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "单选题必须且只能有一个正确答案");
        }
        if (QuestionType.MULTIPLE_CHOICE.code().equals(request.type()) && correctCount < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "多选题至少需要两个正确答案");
        }
    }

    private void fillQuestion(Question question, QuestionSaveRequest request) {
        question.setBankId(request.bankId());
        question.setType(request.type());
        question.setStem(request.stem());
        question.setAnalysis(request.analysis());
        question.setDifficulty(request.difficulty());
        question.setStatus(request.status());
    }

    private void replaceOptions(Long questionId, List<QuestionOptionRequest> options) {
        questionMapper.deleteOptions(questionId);
        int sort = 10;
        for (QuestionOptionRequest request : options == null ? List.<QuestionOptionRequest>of() : options) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionLabel(request.label());
            option.setContent(request.content());
            option.setCorrect(request.correct());
            option.setSortOrder(sort);
            questionMapper.insertOption(option);
            sort += 10;
        }
    }

    private void replaceAttachments(Long questionId, List<QuestionAttachmentRequest> attachments) {
        questionMapper.deleteAttachments(questionId);
        if (attachments == null) {
            return;
        }
        int sort = 10;
        for (QuestionAttachmentRequest attachment : attachments) {
            questionMapper.insertAttachment(questionId, attachment.fileName(), attachment.fileUrl(), attachment.mediaType(), sort);
            sort += 10;
        }
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getBankId(),
                questionMapper.findBankName(question.getBankId()),
                question.getType(),
                question.getStem(),
                question.getAnalysis(),
                question.getDifficulty(),
                question.getStatus(),
                questionMapper.findOptions(question.getId()).stream()
                        .map(this::toOptionResponse)
                        .toList(),
                questionMapper.findAttachments(question.getId()).stream()
                        .map(this::toAttachmentResponse)
                        .toList()
        );
    }

    private QuestionOptionResponse toOptionResponse(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionLabel(),
                option.getContent(),
                option.getCorrect(),
                option.getSortOrder()
        );
    }

    private QuestionAttachmentResponse toAttachmentResponse(QuestionAttachment attachment) {
        return new QuestionAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getMediaType(),
                attachment.getSortOrder()
        );
    }

    private String questionTypeName(String type) {
        return QuestionType.require(type).label().replace("题", "");
    }
}

