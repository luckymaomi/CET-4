package com.kaoshi.question;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelImportResult;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class QuestionService {
    private final QuestionMapper questionMapper;
    private final QuestionExcelService excelService;

    public QuestionService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
        this.excelService = new QuestionExcelService(questionMapper);
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
        return excelService.template();
    }

    @Transactional
    public ExcelImportResult importExcel(MultipartFile file) {
        return excelService.importExcel(file, this::create);
    }

    public ResponseEntity<byte[]> exportExcel() {
        return excelService.exportExcel();
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
}
