package com.example.artinus.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ExceptionType exceptionType;

    public CustomException(ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
    }
}
