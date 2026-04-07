package com.example.thirdtool.UserSchedule.domain.exception;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

public class UserScheduleDomainException extends BusinessException {

    private UserScheduleDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    private UserScheduleDomainException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static UserScheduleDomainException of(ErrorCode errorCode) {
        return new UserScheduleDomainException(errorCode);
    }

    public static UserScheduleDomainException of(ErrorCode errorCode, String detail) {
        return new UserScheduleDomainException(errorCode, detail);
    }
}