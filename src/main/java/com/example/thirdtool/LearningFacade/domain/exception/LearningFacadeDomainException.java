
package com.example.thirdtool.LearningFacade.domain.exception;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

public class LearningFacadeDomainException extends BusinessException {

    private LearningFacadeDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    private LearningFacadeDomainException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static LearningFacadeDomainException of(ErrorCode errorCode) {
        return new LearningFacadeDomainException(errorCode);
    }

    public static LearningFacadeDomainException of(ErrorCode errorCode, String detail) {
        return new LearningFacadeDomainException(errorCode, detail);
    }
}