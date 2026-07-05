package com.kaoshi.exam;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.exam.domain.Exam;
import com.kaoshi.exam.mapper.ExamMapper;
import com.kaoshi.question.QuestionType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static com.kaoshi.exam.ExamRowValues.booleanValue;
import static com.kaoshi.exam.ExamRowValues.decimalValue;
import static com.kaoshi.exam.ExamRowValues.intValue;
import static com.kaoshi.exam.ExamRowValues.longValue;
import static com.kaoshi.exam.ExamRowValues.stringValue;
import static com.kaoshi.exam.ExamRowValues.value;

final class ExamPaperExportService {
    private final ExamMapper examMapper;

    ExamPaperExportService(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    ResponseEntity<byte[]> download(Exam exam) {
        List<Map<String, Object>> rows = !examMapper.findPublishedQuestions(exam.getId()).isEmpty()
                ? examMapper.findPublishedQuestions(exam.getId())
                : examMapper.findDraftQuestions(exam.getId());
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

    private List<String> paperExportRow(Map<String, Object> row) {
        boolean publishedRow = value(row, "sourceQuestionId") != null;
        List<Map<String, Object>> options = publishedRow
                ? examMapper.findPublishedOptions(longValue(value(row, "id")))
                : examMapper.findDraftOptions(longValue(value(row, "id")));
        return List.of(
                String.valueOf(intValue(value(row, "sortOrder"))),
                safeString(value(row, "bankName")),
                QuestionType.require(stringValue(value(row, "type"))).label(),
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

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }
}
