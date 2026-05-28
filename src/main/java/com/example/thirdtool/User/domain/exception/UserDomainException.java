package com.example.thirdtool.User.domain.exception;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

public class UserDomainException extends BusinessException {

    private UserDomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    private UserDomainException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static UserDomainException of(ErrorCode errorCode) {
        return new UserDomainException(errorCode);
    }

    /**
     * 부가 상세 메시지를 포함한 UserDomainException을 생성한다.
     *
     * <p>응답 body의 message: "{errorCode.message} — {detail}"
     *
     * <p>⚠ detail에 **사용자 입력값을 그대로 echo하지 말 것** — input reflection 패턴은
     * 보안상 응답에 노출하지 않고 서버 로그에만 기록한다. 도메인 내부 컨텍스트
     * (예: 자기 자신의 ID·DB식별자 등)에만 사용한다.
     */
    public static UserDomainException of(ErrorCode errorCode, String detail) {
        return new UserDomainException(errorCode, detail);
    }
}
