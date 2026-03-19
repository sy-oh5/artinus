package com.example.artinus.exception;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class ExceptionResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;

    public ExceptionResponse(ExceptionType exceptionType) {
        this.timestamp = LocalDateTime.now();
        this.status = exceptionType.getStatus().value();
        this.error = exceptionType.getStatus().name();
        this.code = exceptionType.name();
        this.message = exceptionType.getMessage();
    }

}

