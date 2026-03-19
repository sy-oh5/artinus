package com.example.artinus.domain;

import com.example.artinus.constant.ChannelType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Builder
    public Channel(String name, ChannelType type) {
        this.name = name;
        this.type = type;
    }

    public boolean canSubscribe() {
        return type.canSubscribe();
    }

    public boolean canUnsubscribe() {
        return type.canUnsubscribe();
    }
}
