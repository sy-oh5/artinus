package com.example.artinus.dto.request;

import com.example.artinus.constant.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "구독 해지 요청 DTO")
public class UnsubscribeRequestDto {

    @NotBlank(message = "휴대폰번호는 필수입니다")
    @Schema(description = "Member.phoneNumber - 회원 휴대폰번호", example = "01012345678")
    private String phoneNumber;

    @NotNull(message = "채널 ID는 필수입니다")
    @Schema(description = "Channel.id - 해지 채널 ID", example = "1")
    private Long channelId;

    @NotNull(message = "변경할 구독 상태는 필수입니다")
    @Schema(description = "Member.subscriptionStatus - 변경할 구독 상태", example = "NONE")
    private SubscriptionStatus targetStatus;
}
