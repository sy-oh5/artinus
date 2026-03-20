package com.example.artinus.repository;

import com.example.artinus.domain.Member;
import com.example.artinus.domain.SubscriptionHistory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.artinus.domain.QChannel.channel;
import static com.example.artinus.domain.QSubscriptionHistory.subscriptionHistory;

@Repository
@RequiredArgsConstructor
public class SubscriptionHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<SubscriptionHistory> findByMemberWithChannel(Member member) {
        return queryFactory
                .selectFrom(subscriptionHistory)
                .join(subscriptionHistory.channel, channel).fetchJoin()
                .where(subscriptionHistory.member.eq(member))
                .orderBy(subscriptionHistory.createdAt.asc())
                .fetch();
    }
}
