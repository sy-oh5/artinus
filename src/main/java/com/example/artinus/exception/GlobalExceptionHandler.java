package com.example.artinus.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * Custom Exception
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ExceptionResponse> handleCustomException(final CustomException e) {
        log.error("[GlobalExceptionHandler] handleCustomException: {}", e.getExceptionType());

        return ResponseEntity
                .status(e.getExceptionType().getStatus().value())
                .body(new ExceptionResponse(e.getExceptionType()));
    }
}
