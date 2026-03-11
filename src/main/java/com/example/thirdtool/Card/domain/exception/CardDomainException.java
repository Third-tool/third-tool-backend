package com.example.thirdtool.Card.domain.exception;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

public class CardDomainException extends BusinessException {

    private CardDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    private CardDomainException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }


    public static CardDomainException of(ErrorCode errorCode) {
        return new CardDomainException(errorCode);
    }

    /**
     * 부가 상세 메시지를 포함한 CardDomainException을 생성한다.
     *
     * <p>응답 body의 message: "{errorCode.message} — {detail}"
     */
    public static CardDomainException of(ErrorCode errorCode, String detail) {
        return new CardDomainException(errorCode, detail);
    }
}
