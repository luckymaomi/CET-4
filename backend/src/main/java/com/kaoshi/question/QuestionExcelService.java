package com.kaoshi.question;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.question.domain.Question;
import com.kaoshi.question.domain.QuestionOption;
import com.kaoshi.question.dto.QuestionOptionRequest;
import com.kaoshi.question.dto.QuestionSaveRequest;
import com.kaoshi.question.mapper.QuestionMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class QuestionExcelService {
    private final QuestionMapper questionMapper;

    QuestionExcelService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    ResponseEntity<byte[]> template() {
        return ExcelWorkbooks.template(
                "试题导入模板.xlsx",
                List.of(importSheet(List.of(
                        List.of("2015年12月英语四级真题第一卷", "单选", "简单", "What does the author think of time displayed everywhere?", "It makes everybody time-conscious.", "It is a convenience for work and life.", "It may have a negative effect on creative work.", "It clearly indicates the fast pace of modern life.", "C", "CET4 阅读理解样例题。"),
                        List.of("2015年12月英语四级真题第一卷", "多选", "简单", "Which sections are included in CET4 set 1?", "Writing", "Listening", "Reading", "Physics experiment", "ABC", "示例多选题使用连续字母，不使用逗号。"),
                        List.of("2015年12月英语四级真题第一卷", "写作", "困难", "Write an essay commenting on the saying Listening is more important than talking.", "", "", "", "", "", "写作题不填写选项和正确答案，可在解析中填写写作要求或参考思路。")
                )), dictionarySheet())
        );
    }

    com.kaoshi.common.excel.ExcelImportResult importExcel(MultipartFile file, Consumer<QuestionSaveRequest> importer) {
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
                    importer.accept(rowToRequest(row));
                    success++;
                } catch (RuntimeException exception) {
                    errors.add("第 " + (rowIndex + 1) + " 行：" + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "读取 Excel 失败");
        }
        return new com.kaoshi.common.excel.ExcelImportResult(success, errors.size(), errors);
    }

    ResponseEntity<byte[]> exportExcel() {
        List<List<String>> rows = questionMapper.findQuestions(null, null, 10000, 0)
                .stream()
                .map(this::exportRow)
                .toList();
        return ExcelWorkbooks.template("试题导出.xlsx", List.of(importSheet(rows), dictionarySheet()));
    }

    private ExcelWorkbooks.SheetData importSheet(List<List<String>> rows) {
        return new ExcelWorkbooks.SheetData(
                "试题导入",
                List.of("题库名称", "题型", "难度", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"),
                rows
        );
    }

    private ExcelWorkbooks.SheetData dictionarySheet() {
        return new ExcelWorkbooks.SheetData(
                "字典清单",
                List.of("字段", "可用值"),
                List.of(
                        List.of("题库名称", String.join("、", questionMapper.findActiveBankNames())),
                        List.of("题型", "单选、 多选、 写作"),
                        List.of("难度", "简单、 困难"),
                        List.of("正确答案", "单选填写 A/B/C/D；多选填写 AC/BCD，不使用逗号；写作题留空")
                )
        );
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
        return new QuestionSaveRequest(bankId, type, stem, ExcelWorkbooks.text(row, 9).trim(), difficulty, "ACTIVE", options, List.of());
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

    private List<String> exportRow(Question question) {
        List<QuestionOption> options = questionMapper.findOptions(question.getId());
        String answer = options.stream()
                .filter(QuestionOption::getCorrect)
                .map(QuestionOption::getOptionLabel)
                .collect(Collectors.joining());
        return List.of(
                questionMapper.findBankName(question.getBankId()),
                QuestionType.require(question.getType()).label().replace("题", ""),
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
}
