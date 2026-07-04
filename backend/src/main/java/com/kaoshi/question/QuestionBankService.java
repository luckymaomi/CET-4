package com.kaoshi.question;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.question.domain.QuestionBank;
import com.kaoshi.question.domain.QuestionCategory;
import com.kaoshi.question.dto.QuestionBankResponse;
import com.kaoshi.question.dto.QuestionBankSaveRequest;
import com.kaoshi.question.dto.QuestionCategorySaveRequest;
import com.kaoshi.question.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionBankService {
    private final QuestionBankMapper bankMapper;

    public QuestionBankService(QuestionBankMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    public PageResponse<QuestionBankResponse> page(PageRequest request) {
        long total = bankMapper.countBanks(request.keywordLike());
        List<QuestionBankResponse> records = bankMapper.findBanks(request.keywordLike(), request.size(), request.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, total, request.page(), request.size());
    }

    public List<QuestionCategory> categories() {
        return bankMapper.findCategories();
    }

    @Transactional
    public QuestionCategory createCategory(QuestionCategorySaveRequest request) {
        validateCategorySave(null, request);
        QuestionCategory category = new QuestionCategory();
        fillCategory(category, request);
        bankMapper.insertCategory(category);
        return findCategory(category.getId());
    }

    @Transactional
    public QuestionCategory updateCategory(Long id, QuestionCategorySaveRequest request) {
        QuestionCategory category = findCategory(id);
        validateCategorySave(id, request);
        fillCategory(category, request);
        bankMapper.updateCategory(category);
        return findCategory(id);
    }

    @Transactional
    public void deleteCategory(Long id) {
        findCategory(id);
        if (bankMapper.countBanksByCategory(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类下存在题库，不能删除");
        }
        bankMapper.deleteCategory(id);
    }

    public QuestionBankResponse detail(Long id) {
        return toResponse(findBank(id));
    }

    @Transactional
    public QuestionBankResponse create(QuestionBankSaveRequest request) {
        ensureCategoryExists(request.categoryId());
        QuestionBank bank = new QuestionBank();
        fillBank(bank, request);
        bankMapper.insertBank(bank);
        return detail(bank.getId());
    }

    @Transactional
    public QuestionBankResponse update(Long id, QuestionBankSaveRequest request) {
        ensureCategoryExists(request.categoryId());
        QuestionBank bank = findBank(id);
        fillBank(bank, request);
        bankMapper.updateBank(bank);
        return detail(id);
    }

    private QuestionBank findBank(Long id) {
        QuestionBank bank = bankMapper.findBankById(id);
        if (bank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题库不存在");
        }
        return bank;
    }

    private QuestionCategory findCategory(Long id) {
        QuestionCategory category = bankMapper.findCategoryById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "试题分类不存在");
        }
        return category;
    }

    private void validateCategorySave(Long id, QuestionCategorySaveRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "分类名称不能为空");
        }
        int duplicate = id == null
                ? bankMapper.countCategoryByName(request.name())
                : bankMapper.countCategoryByNameExceptId(request.name(), id);
        if (duplicate > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类名称已存在");
        }
    }

    private void fillCategory(QuestionCategory category, QuestionCategorySaveRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void fillBank(QuestionBank bank, QuestionBankSaveRequest request) {
        if (!List.of("ACTIVE", "DISABLED").contains(request.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "题库状态不合法");
        }
        bank.setCategoryId(request.categoryId());
        bank.setName(request.name());
        bank.setDescription(request.description());
        bank.setStatus(request.status());
    }

    private void ensureCategoryExists(Long categoryId) {
        if (bankMapper.countCategoryById(categoryId) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "试题分类不存在");
        }
    }

    private QuestionBankResponse toResponse(QuestionBank bank) {
        return new QuestionBankResponse(
                bank.getId(),
                bank.getCategoryId(),
                bankMapper.findCategoryName(bank.getCategoryId()),
                bank.getName(),
                bank.getDescription(),
                bank.getStatus(),
                bankMapper.countQuestionsByBank(bank.getId()),
                bankMapper.countQuestionsByBankAndType(bank.getId(), "SINGLE_CHOICE"),
                bankMapper.countQuestionsByBankAndType(bank.getId(), "MULTIPLE_CHOICE"),
                bankMapper.countQuestionsByBankAndType(bank.getId(), "WRITING")
        );
    }
}

