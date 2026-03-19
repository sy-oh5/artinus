package com.example.artinus.domain;

import com.example.artinus.constant.SubscriptionStatus;
import com.example.artinus.converter.AesConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "members")
@Comment("회원")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회원 ID")
    private Long id;

    @Convert(converter = AesConverter.class)
    @Column(nullable = false, unique = true)
    @Comment("휴대폰번호 (암호화)")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("구독 상태 (NONE, STANDARD, PREMIUM)")
    private SubscriptionStatus subscriptionStatus;

    @Builder
    public Member(String phoneNumber, SubscriptionStatus subscriptionStatus) {
        this.phoneNumber = phoneNumber;
        this.subscriptionStatus = subscriptionStatus;
    }

    public void changeSubscriptionStatus(SubscriptionStatus newStatus) {
        this.subscriptionStatus = newStatus;
    }

    public boolean isCanSubscribeTo(SubscriptionStatus targetStatus) {
        return switch (this.subscriptionStatus) {
            // 구독 안함 -> 일반 구독, 프리미엄 구독 변경 가능
            case NONE -> targetStatus == SubscriptionStatus.STANDARD || targetStatus == SubscriptionStatus.PREMIUM;
            // 일반 구독 -> 프리미엄 구독 변경 가능
            case STANDARD -> targetStatus == SubscriptionStatus.PREMIUM;
            // 프리미엄 구독은 구독 상태 변경 불가
            case PREMIUM -> false;
        };
    }

    public boolean isCanUnsubscribeTo(SubscriptionStatus targetStatus) {
        return switch (this.subscriptionStatus) {
            // 프리미엄 구독 -> 일반 구독, 구독 안함으로 변경 가능
            case PREMIUM -> targetStatus == SubscriptionStatus.STANDARD || targetStatus == SubscriptionStatus.NONE;
            // 일반 구독 -> 구독 안함으로 변경 가능
            case STANDARD -> targetStatus == SubscriptionStatus.NONE;
            // 구독 안함은 구독 상태 변경 불가
            case NONE -> false;
        };
    }
}
