package com.example.thirdtool.Review.domain.exception;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

public class ReviewSessionException extends BusinessException {

    private ReviewSessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ReviewSessionException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static ReviewSessionException of(ErrorCode errorCode) {
        return new ReviewSessionException(errorCode);
    }

    public static ReviewSessionException of(ErrorCode errorCode, String detail) {
        return new ReviewSessionException(errorCode, detail);
    }
}

