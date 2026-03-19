package com.example.artinus.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "액션 타입", enumAsRef = true)
public enum ActionType {
    @Schema(description = "구독")
    SUBSCRIBE("구독"),

    @Schema(description = "해지")
    UNSUBSCRIBE("해지");

    private final String description;
}
