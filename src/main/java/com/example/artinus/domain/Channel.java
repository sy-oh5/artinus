package com.example.artinus.domain;

import com.example.artinus.constant.ChannelType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "channels")
@Comment("채널")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채널 ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("채널명")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("채널 타입 (BOTH, SUBSCRIBE_ONLY, UNSUBSCRIBE_ONLY)")
    private ChannelType type;

    @Builder
    public Channel(String name, ChannelType type) {
        this.name = name;
        this.type = type;
    }

    public boolean canSubscribe() {
        return type.isCanSubscribe();
    }

    public boolean canUnsubscribe() {
        return type.isCanUnsubscribe();
    }
}
