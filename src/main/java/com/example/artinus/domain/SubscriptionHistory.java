package com.example.artinus.domain;

import com.example.artinus.constant.ActionType;
import com.example.artinus.constant.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_history")
@Comment("구독 이력")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("이력 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Comment("회원 ID")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    @Comment("채널 ID")
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("액션 타입 (SUBSCRIBE, UNSUBSCRIBE)")
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("이전 구독 상태")
    private SubscriptionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("변경된 구독 상태")
    private SubscriptionStatus newStatus;

    @Column(nullable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Builder
    public SubscriptionHistory(Member member, Channel channel, ActionType actionType,
                               SubscriptionStatus previousStatus, SubscriptionStatus newStatus) {
        this.member = member;
        this.channel = channel;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.createdAt = LocalDateTime.now();
    }
}
