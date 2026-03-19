package com.example.artinus.constant;

public enum SubscriptionStatus {
    NONE("구독 안함"),
    STANDARD("일반 구독"),
    PREMIUM("프리미엄 구독");

    private final String description;

    SubscriptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
