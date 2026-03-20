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
import com.example.artinus.external.csrng.CsrngApiService;
import com.example.artinus.external.llm.LLMService;
import com.example.artinus.mapper.SubscriptionHistoryMapper;
import com.example.artinus.repository.ChannelRepository;
import com.example.artinus.repository.MemberRepository;
import com.example.artinus.repository.SubscriptionHistoryQueryRepository;
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
    private final SubscriptionHistoryQueryRepository historyQueryRepository;
    private final SubscriptionHistoryMapper historyMapper;
    private final CsrngApiService csrngApiService;
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

        SubscriptionStatus previousStatus = null;

        // step 3: 회원 구독 상태 업데이트
        if (member == null) {
            // 존재하지 않았던 회원이라면 생성
            member = Member.create(
                    request.getName(),
                    request.getPhoneNumber(),
                    request.getTargetStatus()
            );
        } else {
            // 기존 회원인 경우 이름 검증 (보안상 동일한 에러 메시지)
            if (!member.getName().equals(request.getName())) {
                throw new CustomException(ExceptionType.MEMBER_NOT_FOUND);
            }

            previousStatus = member.getSubscriptionStatus();
            if (!member.isCanSubscribeTo(request.getTargetStatus())) {
                throw new CustomException(ExceptionType.INVALID_SUBSCRIPTION_STATUS);
            }
            member.changeSubscriptionStatus(request.getTargetStatus());
        }

        // step 4: 외부 API 처리
        csrngApiService.verifyExternalApi();

        // step 5: 회원 DB 저장
        memberRepository.save(member);

        // step 6: 이력 저장
        saveHistory(member, channel, ActionType.SUBSCRIBE, previousStatus, request.getTargetStatus());

        return SubscriptionResponseDto.builder()
                .phoneNumber(request.getPhoneNumber())
                .previousStatus(previousStatus)
                .newStatus(request.getTargetStatus())
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

        // step 2: 회원 조회 (이름 + 휴대폰번호로 검증)
        Member member = memberRepository.findByNameAndPhoneNumber(request.getName(), request.getPhoneNumber())
                .orElseThrow(() -> new CustomException(ExceptionType.MEMBER_NOT_FOUND));

        SubscriptionStatus previousStatus = member.getSubscriptionStatus();

        if (!member.isCanUnsubscribeTo(request.getTargetStatus())) {
            throw new CustomException(ExceptionType.INVALID_UNSUBSCRIPTION_STATUS);
        }

        // step 3: 외부 API 검증
        csrngApiService.verifyExternalApi();

        // step 4: 회원 DB 저장
        member.changeSubscriptionStatus(request.getTargetStatus());
        memberRepository.save(member);

        // step 5: 이력 저장
        saveHistory(member, channel, ActionType.UNSUBSCRIBE, previousStatus, request.getTargetStatus());

        return SubscriptionResponseDto.builder()
                .phoneNumber(request.getPhoneNumber())
                .previousStatus(previousStatus)
                .newStatus(request.getTargetStatus())
                .build();
    }

    private void saveHistory(Member member, Channel channel, ActionType actionType,
                             SubscriptionStatus previousStatus, SubscriptionStatus newStatus) {
        SubscriptionHistory history = SubscriptionHistory.create(member, channel, actionType, previousStatus, newStatus);
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public SubscriptionHistoryResponseDto getHistory(String name, String phoneNumber) {
        // 이름 + 휴대폰번호로 검증
        Member member = memberRepository.findByNameAndPhoneNumber(name, phoneNumber)
                .orElseThrow(() -> new CustomException(ExceptionType.MEMBER_NOT_FOUND));

        List<SubscriptionHistory> histories = historyQueryRepository.findByMemberWithChannel(member);

        List<SubscriptionHistoryResponseDto.HistoryItemDto> historyItems = historyMapper.toHistoryItemDtoList(histories);

        String summary = generateSummary(member, histories);

        return SubscriptionHistoryResponseDto.builder()
                .memberName(member.getName())
                .currentStatus(member.getSubscriptionStatus())
                .memberCreatedAt(member.getCreatedAt())
                .history(historyItems)
                .summary(summary)
                .build();
    }

    private String generateSummary(Member member, List<SubscriptionHistory> histories) {
        if (histories.isEmpty()) {
            return "구독 이력이 없습니다.";
        }

        StringBuilder historyText = new StringBuilder();

        // prompt를 위해 구독 이력 문자열로 변환
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
                다음은 %s 회원의 구독 이력입니다. 이 이력을 자연스러운 한국어 문장으로 요약해주세요.
                간결하게 2-3문장으로 작성해주세요.
                
                구독 이력:
                %s
                """, member.getName(), historyText);
        log.info(prompt);
        return llmService.generatePrompt(prompt);
    }
}
