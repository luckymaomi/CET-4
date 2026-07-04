package com.kaoshi.question;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;

import java.util.Arrays;
import java.util.List;

public enum QuestionType {
    SINGLE_CHOICE("SINGLE_CHOICE", "单选题", true, true, false),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "多选题", true, true, false),
    WRITING("WRITING", "写作题", false, false, true);

    private final String code;
    private final String label;
    private final boolean optionBased;
    private final boolean autoGradable;
    private final boolean manualReview;

    QuestionType(String code, String label, boolean optionBased, boolean autoGradable, boolean manualReview) {
        this.code = code;
        this.label = label;
        this.optionBased = optionBased;
        this.autoGradable = autoGradable;
        this.manualReview = manualReview;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean optionBased() {
        return optionBased;
    }

    public boolean autoGradable() {
        return autoGradable;
    }

    public boolean manualReview() {
        return manualReview;
    }

    public static QuestionType require(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "试题类型不支持"));
    }

    public static List<QuestionType> ruleOrder() {
        return List.of(SINGLE_CHOICE, MULTIPLE_CHOICE, WRITING);
    }
}
