package com.example.artinus.external;

import com.example.artinus.exception.ExternalApiException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CsrngClient {

    private static final Logger log = LoggerFactory.getLogger(CsrngClient.class);
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final CsrngFeignClient csrngFeignClient;

    public CsrngClient(CsrngFeignClient csrngFeignClient) {
        this.csrngFeignClient = csrngFeignClient;
    }

    public boolean verifyTransaction() {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                List<CsrngFeignClient.CsrngResponse> responses = csrngFeignClient.getRandomNumber(0, 1);

                if (responses != null && !responses.isEmpty()) {
                    CsrngFeignClient.CsrngResponse response = responses.get(0);
                    if ("success".equals(response.status())) {
                        return response.random() == 1;
                    }
                }

                log.warn("CSRNG API 응답이 올바르지 않습니다. 재시도 중... ({})", retryCount + 1);

            } catch (FeignException e) {
                log.warn("CSRNG API 호출 실패. 재시도 중... ({}) - {}", retryCount + 1, e.getMessage());
            }

            retryCount++;
            if (retryCount < MAX_RETRY_COUNT) {
                sleep(RETRY_DELAY_MS);
            }
        }

        throw new ExternalApiException("외부 API(csrng) 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
