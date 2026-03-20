package com.example.artinus.dto.response;

import com.example.artinus.constant.ActionType;
import com.example.artinus.constant.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "구독 이력 조회 응답 DTO")
public class SubscriptionHistoryResponseDto {

    @Schema(description = "Member.name - 회원명", example = "홍길동")
    private String memberName;

    @Schema(description = "Member.subscriptionStatus - 현재 구독 상태", example = "STANDARD")
    private SubscriptionStatus currentStatus;

    @Schema(description = "Member.createdAt - 회원 가입일")
    private LocalDateTime memberCreatedAt;

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

        @Schema(description = "SubscriptionHistory.actionType - 액션 타입", example = "SUBSCRIBE")
        private ActionType actionType;

        @Schema(description = "SubscriptionHistory.previousStatus - 이전 구독 상태", example = "NONE")
        private SubscriptionStatus previousStatus;

        @Schema(description = "SubscriptionHistory.newStatus - 변경된 구독 상태", example = "STANDARD")
        private SubscriptionStatus newStatus;

        @Schema(description = "SubscriptionHistory.createdAt - 액션 일시", example = "2026-01-01 10:00:00")
        private LocalDateTime actionDate;
    }
}
