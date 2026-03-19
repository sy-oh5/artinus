package com.example.artinus.service;

import com.example.artinus.constant.ActionType;
import com.example.artinus.constant.SubscriptionStatus;
import com.example.artinus.domain.Channel;
import com.example.artinus.domain.Member;
import com.example.artinus.domain.SubscriptionHistory;
import com.example.artinus.dto.request.SubscribeRequestDto;
import com.example.artinus.dto.request.UnsubscribeRequestDto;
import com.example.artinus.dto.response.SubscriptionHistoryResponseDto;
import com.example.artinus.dto.response.SubscriptionResponseDto;
import com.example.artinus.exception.CustomException;
import com.example.artinus.exception.ExceptionType;
import com.example.artinus.external.CsrngFeignClient;
import com.example.artinus.repository.ChannelRepository;
import com.example.artinus.repository.MemberRepository;
import com.example.artinus.repository.SubscriptionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final CsrngFeignClient csrngFeignClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public SubscriptionResponseDto subscribe(SubscribeRequestDto request) {
        Channel channel = channelRepository.findById(request.getChannelId())
            .orElseThrow(() -> new CustomException(ExceptionType.CHANNEL_NOT_FOUND));

        if (!channel.canSubscribe()) {
            throw new CustomException(ExceptionType.CHANNEL_SUBSCRIBE_NOT_ALLOWED);
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
            .orElse(null);

        SubscriptionStatus previousStatus;

        if (member == null) {
            if (request.getTargetStatus() == SubscriptionStatus.NONE) {
                throw new CustomException(ExceptionType.CANNOT_SUBSCRIBE_TO_NONE);
            }
            previousStatus = SubscriptionStatus.NONE;
            member = Member.builder()
                .phoneNumber(request.getPhoneNumber())
                .subscriptionStatus(request.getTargetStatus())
                .build();
        } else {
            previousStatus = member.getSubscriptionStatus();
            if (!member.canSubscribeTo(request.getTargetStatus())) {
                throw new CustomException(ExceptionType.INVALID_SUBSCRIPTION_STATUS);
            }
            member.changeSubscriptionStatus(request.getTargetStatus());
        }

        verifyExternalApi();

        memberRepository.save(member);

        SubscriptionHistory history = SubscriptionHistory.builder()
            .member(member)
            .channel(channel)
            .actionType(ActionType.SUBSCRIBE)
            .previousStatus(previousStatus)
            .newStatus(request.getTargetStatus())
            .build();
        historyRepository.save(history);

        return SubscriptionResponseDto.builder()
            .phoneNumber(request.getPhoneNumber())
            .previousStatus(previousStatus.getDescription())
            .newStatus(request.getTargetStatus().getDescription())
            .message("구독이 완료되었습니다.")
            .build();
    }

    @Transactional
    public SubscriptionResponseDto unsubscribe(UnsubscribeRequestDto request) {
        Channel channel = channelRepository.findById(request.getChannelId())
            .orElseThrow(() -> new CustomException(ExceptionType.CHANNEL_NOT_FOUND));

        if (!channel.canUnsubscribe()) {
            throw new CustomException(ExceptionType.CHANNEL_UNSUBSCRIBE_NOT_ALLOWED);
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
            .orElseThrow(() -> new CustomException(ExceptionType.MEMBER_NOT_FOUND));

        SubscriptionStatus previousStatus = member.getSubscriptionStatus();

        if (!member.canUnsubscribeTo(request.getTargetStatus())) {
            throw new CustomException(ExceptionType.INVALID_UNSUBSCRIPTION_STATUS);
        }

        verifyExternalApi();

        member.changeSubscriptionStatus(request.getTargetStatus());
        memberRepository.save(member);

        SubscriptionHistory history = SubscriptionHistory.builder()
            .member(member)
            .channel(channel)
            .actionType(ActionType.UNSUBSCRIBE)
            .previousStatus(previousStatus)
            .newStatus(request.getTargetStatus())
            .build();
        historyRepository.save(history);

        return SubscriptionResponseDto.builder()
            .phoneNumber(request.getPhoneNumber())
            .previousStatus(previousStatus.getDescription())
            .newStatus(request.getTargetStatus().getDescription())
            .message("해지가 완료되었습니다.")
            .build();
    }

    @Transactional(readOnly = true)
    public SubscriptionHistoryResponseDto getHistory(String phoneNumber) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new CustomException(ExceptionType.MEMBER_NOT_FOUND));

        List<SubscriptionHistory> histories = historyRepository.findByMemberWithChannel(member);

        List<SubscriptionHistoryResponseDto.HistoryItemDto> historyItems = histories.stream()
            .map(h -> SubscriptionHistoryResponseDto.HistoryItemDto.builder()
                .channelName(h.getChannel().getName())
                .actionType(h.getActionType().getDescription())
                .previousStatus(h.getPreviousStatus().getDescription())
                .newStatus(h.getNewStatus().getDescription())
                .actionDate(h.getCreatedAt().format(DATE_FORMATTER))
                .build())
            .toList();

        // TODO: LLM API 연동하여 요약 생성
        String summary = generateSummary(histories);

        return SubscriptionHistoryResponseDto.builder()
            .history(historyItems)
            .summary(summary)
            .build();
    }

    private void verifyExternalApi() {
        List<CsrngFeignClient.CsrngResponse> responses = csrngFeignClient.getRandomNumber(0, 1);

        if (responses == null || responses.isEmpty()) {
            throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
        }

        CsrngFeignClient.CsrngResponse response = responses.get(0);
        if (!"success".equals(response.status()) || response.random() != 1) {
            throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
        }
    }

    private String generateSummary(List<SubscriptionHistory> histories) {
        // TODO: LLM API 연동 구현
        if (histories.isEmpty()) {
            return "구독 이력이 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        for (SubscriptionHistory h : histories) {
            sb.append(String.format("%s %s을(를) 통해 %s에서 %s(으)로 %s하였습니다. ",
                h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")),
                h.getChannel().getName(),
                h.getPreviousStatus().getDescription(),
                h.getNewStatus().getDescription(),
                h.getActionType().getDescription()
            ));
        }
        return sb.toString().trim();
    }
}
