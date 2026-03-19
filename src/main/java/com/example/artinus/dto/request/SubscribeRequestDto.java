package com.example.artinus.dto.request;

import com.example.artinus.constant.SubscriptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscribeRequestDto {

    @NotBlank(message = "휴대폰번호는 필수입니다")
    private String phoneNumber;

    @NotNull(message = "채널 ID는 필수입니다")
    private Long channelId;

    @NotNull(message = "변경할 구독 상태는 필수입니다")
    private SubscriptionStatus targetStatus;
}
