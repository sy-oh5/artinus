package com.example.artinus.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "구독/해지 응답 DTO")
public class SubscriptionResponseDto {

    @Schema(description = "Member.phoneNumber - 회원 휴대폰번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "Member.subscriptionStatus - 이전 구독 상태", example = "구독 안함")
    private String previousStatus;

    @Schema(description = "Member.subscriptionStatus - 변경된 구독 상태", example = "일반 구독")
    private String newStatus;

    @Schema(description = "처리 결과 메시지", example = "구독이 완료되었습니다.")
    private String message;
}
