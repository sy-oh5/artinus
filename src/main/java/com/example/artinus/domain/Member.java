package com.example.artinus.domain;

import com.example.artinus.config.PhoneNumberEncryptor;
import com.example.artinus.constant.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PhoneNumberEncryptor.class)
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus;

    @Builder
    public Member(String phoneNumber, SubscriptionStatus subscriptionStatus) {
        this.phoneNumber = phoneNumber;
        this.subscriptionStatus = subscriptionStatus;
    }

    public void changeSubscriptionStatus(SubscriptionStatus newStatus) {
        this.subscriptionStatus = newStatus;
    }

    public boolean canSubscribeTo(SubscriptionStatus targetStatus) {
        return switch (this.subscriptionStatus) {
            case NONE -> targetStatus == SubscriptionStatus.STANDARD || targetStatus == SubscriptionStatus.PREMIUM;
            case STANDARD -> targetStatus == SubscriptionStatus.PREMIUM;
            case PREMIUM -> false;
        };
    }

    public boolean canUnsubscribeTo(SubscriptionStatus targetStatus) {
        return switch (this.subscriptionStatus) {
            case PREMIUM -> targetStatus == SubscriptionStatus.STANDARD || targetStatus == SubscriptionStatus.NONE;
            case STANDARD -> targetStatus == SubscriptionStatus.NONE;
            case NONE -> false;
        };
    }
}
