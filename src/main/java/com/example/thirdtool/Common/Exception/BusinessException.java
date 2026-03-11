package com.example.thirdtool.Common.Exception;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import lombok.Getter;


public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    //디테일한 메시지 추가가 필요한
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " — " + detail);
        this.errorCode = errorCode;
    }

    public static BusinessException withDetail(ErrorCode errorCode, String detail) {
        return new BusinessException(errorCode, detail);
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
