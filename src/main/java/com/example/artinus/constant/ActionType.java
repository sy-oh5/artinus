package com.example.artinus.constant;

public enum ActionType {
    SUBSCRIBE("구독"),
    UNSUBSCRIBE("해지");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
