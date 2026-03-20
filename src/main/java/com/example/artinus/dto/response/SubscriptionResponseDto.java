package com.example.artinus.dto.response;

import com.example.artinus.constant.SubscriptionStatus;
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

    @Schema(description = "Member.subscriptionStatus - 이전 구독 상태", example = "NONE")
    private SubscriptionStatus previousStatus;

    @Schema(description = "Member.subscriptionStatus - 변경된 구독 상태", example = "STANDARD")
    private SubscriptionStatus newStatus;
}
