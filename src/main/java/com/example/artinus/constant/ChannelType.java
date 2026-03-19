package com.example.artinus.constant;

public enum ChannelType {
    BOTH("구독/해지 모두 가능", true, true),
    SUBSCRIBE_ONLY("구독만 가능", true, false),
    UNSUBSCRIBE_ONLY("해지만 가능", false, true);

    private final String description;
    private final boolean canSubscribe;
    private final boolean canUnsubscribe;

    ChannelType(String description, boolean canSubscribe, boolean canUnsubscribe) {
        this.description = description;
        this.canSubscribe = canSubscribe;
        this.canUnsubscribe = canUnsubscribe;
    }

    public String getDescription() {
        return description;
    }

    public boolean canSubscribe() {
        return canSubscribe;
    }

    public boolean canUnsubscribe() {
        return canUnsubscribe;
    }
}
