package com.example.artinus.service;

import com.example.artinus.constant.ChannelType;
import com.example.artinus.constant.SubscriptionStatus;
import com.example.artinus.domain.Channel;
import com.example.artinus.domain.Member;
import com.example.artinus.dto.request.SubscribeRequestDto;
import com.example.artinus.dto.request.UnsubscribeRequestDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    ChannelRepository channelRepository;

    @Mock
    SubscriptionHistoryRepository historyRepository;

    @Mock
    SubscriptionHistoryQueryRepository historyQueryRepository;

    @Mock
    SubscriptionHistoryMapper historyMapper;

    @Mock
    CsrngApiService csrngApiService;

    @Mock
    LLMService llmService;

    @InjectMocks
    SubscriptionService subscriptionService;

    private Channel channel;

    @BeforeEach
    void setUp() {
        channel = Channel.builder()
                .name("홈페이지")
                .type(ChannelType.BOTH)
                .build();
        ReflectionTestUtils.setField(channel, "id", 1L);
    }

    @Nested
    @DisplayName("subscribe 메서드")
    class Subscribe {

        @Test
        @DisplayName("신규 회원 구독 성공")
        void 신규_회원_구독_성공() {
            // given
            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.STANDARD);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
            doNothing().when(csrngApiService).verifyExternalApi();
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SubscriptionResponseDto response = subscriptionService.subscribe(request);

            // then
            assertThat(response.getPhoneNumber()).isEqualTo("01012345678");
            assertThat(response.getPreviousStatus()).isNull();
            assertThat(response.getNewStatus()).isEqualTo(SubscriptionStatus.STANDARD);

            verify(memberRepository).save(any(Member.class));
            verify(historyRepository).save(any());
        }

        @Test
        @DisplayName("기존 회원 구독 업그레이드 성공")
        void 기존_회원_구독_업그레이드_성공() {
            // given
            Member existingMember = Member.create("홍길동", "01012345678", SubscriptionStatus.STANDARD);

            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.PREMIUM);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(existingMember));
            doNothing().when(csrngApiService).verifyExternalApi();

            // when
            SubscriptionResponseDto response = subscriptionService.subscribe(request);

            // then
            assertThat(response.getPreviousStatus()).isEqualTo(SubscriptionStatus.STANDARD);
            assertThat(response.getNewStatus()).isEqualTo(SubscriptionStatus.PREMIUM);
        }

        @Test
        @DisplayName("채널이 존재하지 않으면 예외 발생")
        void 채널_없음_예외() {
            // given
            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setChannelId(999L);

            when(channelRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.CHANNEL_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("구독 불가 채널에서 구독 시도 시 예외 발생")
        void 구독_불가_채널_예외() {
            // given
            Channel unsubscribeOnlyChannel = Channel.builder()
                    .name("해지전용채널")
                    .type(ChannelType.UNSUBSCRIBE_ONLY)
                    .build();
            ReflectionTestUtils.setField(unsubscribeOnlyChannel, "id", 2L);

            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setChannelId(2L);

            when(channelRepository.findById(2L)).thenReturn(Optional.of(unsubscribeOnlyChannel));

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.CHANNEL_SUBSCRIBE_NOT_ALLOWED);
                    });
        }

        @Test
        @DisplayName("기존 회원인데 이름이 불일치하면 예외 발생")
        void 기존_회원_이름_불일치_예외() {
            // given
            Member existingMember = Member.create("홍길동", "01012345678", SubscriptionStatus.STANDARD);

            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setName("김철수");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.PREMIUM);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(existingMember));

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.MEMBER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("구독 상태 변경 불가 시 예외 발생")
        void 구독_상태_변경_불가_예외() {
            // given
            Member premiumMember = Member.create("홍길동", "01012345678", SubscriptionStatus.PREMIUM);

            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.STANDARD);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(premiumMember));

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.INVALID_SUBSCRIPTION_STATUS);
                    });
        }

        @Test
        @DisplayName("외부 API 실패 시 예외 발생")
        void 외부_API_실패_예외() {
            // given
            SubscribeRequestDto request = new SubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.STANDARD);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
            doThrow(new CustomException(ExceptionType.EXTERNAL_API_FAILURE))
                    .when(csrngApiService).verifyExternalApi();

            // when & then
            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.EXTERNAL_API_FAILURE);
                    });

            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unsubscribe 메서드")
    class Unsubscribe {

        @Test
        @DisplayName("구독 해지 성공")
        void 구독_해지_성공() {
            // given
            Member member = Member.create("홍길동", "01012345678", SubscriptionStatus.STANDARD);

            UnsubscribeRequestDto request = new UnsubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.NONE);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByNameAndPhoneNumber("홍길동", "01012345678")).thenReturn(Optional.of(member));
            doNothing().when(csrngApiService).verifyExternalApi();

            // when
            SubscriptionResponseDto response = subscriptionService.unsubscribe(request);

            // then
            assertThat(response.getPreviousStatus()).isEqualTo(SubscriptionStatus.STANDARD);
            assertThat(response.getNewStatus()).isEqualTo(SubscriptionStatus.NONE);

            verify(memberRepository).save(any(Member.class));
            verify(historyRepository).save(any());
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외 발생")
        void 회원_없음_예외() {
            // given
            UnsubscribeRequestDto request = new UnsubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01099999999");
            request.setChannelId(1L);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByNameAndPhoneNumber("홍길동", "01099999999")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> subscriptionService.unsubscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.MEMBER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("해지 불가 채널에서 해지 시도 시 예외 발생")
        void 해지_불가_채널_예외() {
            // given
            Channel subscribeOnlyChannel = Channel.builder()
                    .name("구독전용채널")
                    .type(ChannelType.SUBSCRIBE_ONLY)
                    .build();
            ReflectionTestUtils.setField(subscribeOnlyChannel, "id", 3L);

            UnsubscribeRequestDto request = new UnsubscribeRequestDto();
            request.setChannelId(3L);

            when(channelRepository.findById(3L)).thenReturn(Optional.of(subscribeOnlyChannel));

            // when & then
            assertThatThrownBy(() -> subscriptionService.unsubscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.CHANNEL_UNSUBSCRIBE_NOT_ALLOWED);
                    });
        }

        @Test
        @DisplayName("해지 상태 변경 불가 시 예외 발생")
        void 해지_상태_변경_불가_예외() {
            // given
            Member noneMember = Member.create("홍길동", "01012345678", SubscriptionStatus.NONE);

            UnsubscribeRequestDto request = new UnsubscribeRequestDto();
            request.setName("홍길동");
            request.setPhoneNumber("01012345678");
            request.setChannelId(1L);
            request.setTargetStatus(SubscriptionStatus.STANDARD);

            when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
            when(memberRepository.findByNameAndPhoneNumber("홍길동", "01012345678")).thenReturn(Optional.of(noneMember));

            // when & then
            assertThatThrownBy(() -> subscriptionService.unsubscribe(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getExceptionType()).isEqualTo(ExceptionType.INVALID_UNSUBSCRIPTION_STATUS);
                    });
        }
    }
}
