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
import com.example.artinus.exception.SubscriptionException;
import com.example.artinus.external.CsrngClient;
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
    private final CsrngClient csrngClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public SubscriptionResponseDto subscribe(SubscribeRequestDto request) {
        Channel channel = channelRepository.findById(request.getChannelId())
            .orElseThrow(() -> new SubscriptionException("존재하지 않는 채널입니다."));

        if (!channel.canSubscribe()) {
            throw new SubscriptionException("해당 채널에서는 구독할 수 없습니다.");
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
            .orElse(null);

        SubscriptionStatus previousStatus;

        if (member == null) {
            // 최초 회원은 어떤 상태로든 가입 가능
            if (request.getTargetStatus() == SubscriptionStatus.NONE) {
                throw new SubscriptionException("구독 안함 상태로는 구독할 수 없습니다.");
            }
            previousStatus = SubscriptionStatus.NONE;
            member = Member.builder()
                .phoneNumber(request.getPhoneNumber())
                .subscriptionStatus(request.getTargetStatus())
                .build();
        } else {
            previousStatus = member.getSubscriptionStatus();
            if (!member.canSubscribeTo(request.getTargetStatus())) {
                throw new SubscriptionException(
                    String.format("%s 상태에서 %s 상태로 변경할 수 없습니다.",
                        previousStatus.getDescription(),
                        request.getTargetStatus().getDescription())
                );
            }
            member.changeSubscriptionStatus(request.getTargetStatus());
        }

        // 외부 API 호출
        boolean isSuccess = csrngClient.verifyTransaction();
        if (!isSuccess) {
            throw new SubscriptionException("외부 시스템 검증에 실패했습니다. 트랜잭션이 롤백됩니다.");
        }

        memberRepository.save(member);

        // 이력 저장
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
            .orElseThrow(() -> new SubscriptionException("존재하지 않는 채널입니다."));

        if (!channel.canUnsubscribe()) {
            throw new SubscriptionException("해당 채널에서는 해지할 수 없습니다.");
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
            .orElseThrow(() -> new SubscriptionException("존재하지 않는 회원입니다."));

        SubscriptionStatus previousStatus = member.getSubscriptionStatus();

        if (!member.canUnsubscribeTo(request.getTargetStatus())) {
            throw new SubscriptionException(
                String.format("%s 상태에서 %s 상태로 해지할 수 없습니다.",
                    previousStatus.getDescription(),
                    request.getTargetStatus().getDescription())
            );
        }

        // 외부 API 호출
        boolean isSuccess = csrngClient.verifyTransaction();
        if (!isSuccess) {
            throw new SubscriptionException("외부 시스템 검증에 실패했습니다. 트랜잭션이 롤백됩니다.");
        }

        member.changeSubscriptionStatus(request.getTargetStatus());
        memberRepository.save(member);

        // 이력 저장
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
            .orElseThrow(() -> new SubscriptionException("존재하지 않는 회원입니다."));

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
