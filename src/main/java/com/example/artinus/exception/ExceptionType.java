package com.example.artinus.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionType {

    /*
     * 400 BAD_REQUEST: 잘못된 요청
     */
    INVALID_SUBSCRIPTION_STATUS(HttpStatus.BAD_REQUEST, "해당 구독 상태로 변경할 수 없습니다."),
    INVALID_UNSUBSCRIPTION_STATUS(HttpStatus.BAD_REQUEST, "해당 구독 상태로 해지할 수 없습니다."),

    /*
     * 403 FORBIDDEN: 권한 없음
     */
    CHANNEL_SUBSCRIBE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "해당 채널에서는 구독할 수 없습니다."),
    CHANNEL_UNSUBSCRIBE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "해당 채널에서는 해지할 수 없습니다."),

    /*
     * 404 NOT_FOUND: 리소스 없음
     */
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채널입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    /*
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),

    /*
     * 503 SERVICE_UNAVAILABLE: 서비스 이용 불가
     */
    EXTERNAL_API_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "외부 시스템 검증에 실패했습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String message;
}
