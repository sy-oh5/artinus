package com.example.artinus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistoryResponseDto {

    private List<HistoryItemDto> history;
    private String summary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryItemDto {
        private String channelName;
        private String actionType;
        private String previousStatus;
        private String newStatus;
        private String actionDate;
    }
}
