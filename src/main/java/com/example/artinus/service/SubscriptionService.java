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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SubscriptionService {

    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final CsrngFeignClient csrngFeignClient;
    private final LLMService llmService;

    @Transactional
    public SubscriptionResponseDto subscribe(SubscribeRequestDto request) {
        // step 1: 채널 조회
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new CustomException(ExceptionType.CHANNEL_NOT_FOUND));

        // 해당 채널에서 구독이 허용되지 않는다면 에러
        if (!channel.getType().isCanSubscribe()) {
            throw new CustomException(ExceptionType.CHANNEL_SUBSCRIBE_NOT_ALLOWED);
        }

        // step 2: 회원 조회
        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);

        SubscriptionStatus previousStatus;

        // step 3: 회원 구독 상태 업데이트
        if (member == null) {
            previousStatus = null; // 신규 회원은 이전 상태 없음
            // 존재하지 않았던 회원이라면 생성
            member = Member.builder()
                    .name(request.getName())
                    .phoneNumber(request.getPhoneNumber())
                    .subscriptionStatus(request.getTargetStatus())
                    .build();
        } else {
            previousStatus = member.getSubscriptionStatus();
            if (!member.isCanSubscribeTo(request.getTargetStatus())) {
                throw new CustomException(ExceptionType.INVALID_SUBSCRIPTION_STATUS);
            }
            member.changeSubscriptionStatus(request.getTargetStatus());
        }

        // step 4: 외부 API 처리
        verifyExternalApi();

        // step 5: 회원 DB 저장
        memberRepository.save(member);

        // step 6: 이력 저장
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
                .previousStatus(previousStatus != null ? previousStatus.getDescription() : null)
                .newStatus(request.getTargetStatus().getDescription())
                .message("구독이 완료되었습니다.")
                .build();
    }

    @Transactional
    public SubscriptionResponseDto unsubscribe(UnsubscribeRequestDto request) {
        // step 1: 채널 조회
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new CustomException(ExceptionType.CHANNEL_NOT_FOUND));

        if (!channel.getType().isCanUnsubscribe()) {
            throw new CustomException(ExceptionType.CHANNEL_UNSUBSCRIBE_NOT_ALLOWED);
        }

        // step 2: 회원 조회
        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new CustomException(ExceptionType.MEMBER_NOT_FOUND));

        SubscriptionStatus previousStatus = member.getSubscriptionStatus();

        if (!member.isCanUnsubscribeTo(request.getTargetStatus())) {
            throw new CustomException(ExceptionType.INVALID_UNSUBSCRIPTION_STATUS);
        }

        // step 3: 외부 API 검증
        verifyExternalApi();

        // step 4: 회원 DB 저장
        member.changeSubscriptionStatus(request.getTargetStatus());
        memberRepository.save(member);

        // step 5: 이력 저장
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
                        .previousStatus(h.getPreviousStatus() != null ? h.getPreviousStatus().getDescription() : null)
                        .newStatus(h.getNewStatus().getDescription())
                        .actionDate(h.getCreatedAt())
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

        CsrngFeignClient.CsrngResponse response = responses.getFirst();
        if (response.random() == 0) {
            throw new CustomException(ExceptionType.EXTERNAL_API_FAILURE);
        }
    }

    private String generateSummary(List<SubscriptionHistory> histories) {
        if (histories.isEmpty()) {
            return "구독 이력이 없습니다.";
        }

        StringBuilder historyText = new StringBuilder();
        for (SubscriptionHistory h : histories) {
            String previousStatusDesc = h.getPreviousStatus() != null
                    ? h.getPreviousStatus().getDescription()
                    : "없음";
            historyText.append(String.format("- %s: %s에서 %s → %s (%s)\n",
                    h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")),
                    h.getChannel().getName(),
                    previousStatusDesc,
                    h.getNewStatus().getDescription(),
                    h.getActionType().getDescription()
            ));
        }

        String prompt = String.format("""
                다음은 회원의 구독 이력입니다. 이 이력을 자연스러운 한국어 문장으로 요약해주세요.
                간결하게 2-3문장으로 작성해주세요.
                
                구독 이력:
                %s
                """, historyText);
        log.info(prompt);
        return llmService.generateSummary(prompt);
    }
}
