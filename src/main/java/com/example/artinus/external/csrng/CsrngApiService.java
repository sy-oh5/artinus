package com.example.artinus.external.csrng;

import com.example.artinus.exception.CustomException;
import com.example.artinus.exception.ExceptionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CsrngApiService {

    private final CsrngApiClient csrngApiClient;

    /**
     * 외부 API를 호출하여 랜덤 값을 검증합니다.
     * 실패 시 최대 3번 재시도합니다.
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void verifyExternalApi() {
        RetryContext context = RetrySynchronizationManager.getContext();
        int attempt = (context != null) ? context.getRetryCount() + 1 : 1;
        log.info("외부 API 호출 시도 ({}/3)", attempt);

        List<CsrngApiClient.CsrngResponse> responses = csrngApiClient.getRandomNumber(0, 1);

        if (responses == null || responses.isEmpty()) {
            log.warn("외부 API 검증 실패 - 응답 없음 ({}/3)", attempt);
            throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
        }

        CsrngApiClient.CsrngResponse response = responses.getFirst();
        if (response.random() == 0) {
            log.warn("외부 API 검증 실패 - random 값이 0 ({}/3)", attempt);
            throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
        }

        log.info("외부 API 검증 성공 ({}/3)", attempt);
    }

    @Recover
    public void recover(Exception e) {
        log.error("외부 API 호출 3회 재시도 후 최종 실패: {}", e.getMessage());
        throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
    }
}
