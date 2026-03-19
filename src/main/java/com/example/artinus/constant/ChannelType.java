package com.example.artinus.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "채널 타입", enumAsRef = true)
public enum ChannelType {
    @Schema(description = "구독/해지 모두 가능")
    BOTH("구독/해지 모두 가능", true, true),

    @Schema(description = "구독만 가능")
    SUBSCRIBE_ONLY("구독만 가능", true, false),

    @Schema(description = "해지만 가능")
    UNSUBSCRIBE_ONLY("해지만 가능", false, true);

    private final String description;
    private final boolean canSubscribe;
    private final boolean canUnsubscribe;
}
