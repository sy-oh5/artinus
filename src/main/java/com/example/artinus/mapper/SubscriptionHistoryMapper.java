package com.example.artinus.mapper;

import com.example.artinus.domain.SubscriptionHistory;
import com.example.artinus.dto.response.SubscriptionHistoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionHistoryMapper {

    @Mapping(source = "channel.name", target = "channelName")
    @Mapping(source = "createdAt", target = "actionDate")
    SubscriptionHistoryResponseDto.HistoryItemDto toHistoryItemDto(SubscriptionHistory history);

    List<SubscriptionHistoryResponseDto.HistoryItemDto> toHistoryItemDtoList(List<SubscriptionHistory> histories);
}
