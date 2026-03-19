package com.example.artinus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "구독 이력 조회 응답 DTO")
public class SubscriptionHistoryResponseDto {

    @Schema(description = "SubscriptionHistory - 구독/해지 이력 목록")
    private List<HistoryItemDto> history;

    @Schema(description = "LLM 생성 요약", example = "2026년 1월 1일 홈페이지를 통해 일반 구독으로 가입하였습니다.")
    private String summary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "구독 이력 항목")
    public static class HistoryItemDto {

        @Schema(description = "Channel.name - 채널명", example = "홈페이지")
        private String channelName;

        @Schema(description = "SubscriptionHistory.actionType - 액션 타입", example = "구독")
        private String actionType;

        @Schema(description = "SubscriptionHistory.previousStatus - 이전 구독 상태", example = "구독 안함")
        private String previousStatus;

        @Schema(description = "SubscriptionHistory.newStatus - 변경된 구독 상태", example = "일반 구독")
        private String newStatus;

        @Schema(description = "SubscriptionHistory.createdAt - 액션 일시", example = "2026-01-01 10:00:00")
        private String actionDate;
    }
}
