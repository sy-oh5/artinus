package com.example.artinus.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "구독 상태", enumAsRef = true)
public enum SubscriptionStatus {
    @Schema(description = "구독 안함")
    NONE("구독 안함"),

    @Schema(description = "일반 구독")
    STANDARD("일반 구독"),

    @Schema(description = "프리미엄 구독")
    PREMIUM("프리미엄 구독");

    private final String description;
}
