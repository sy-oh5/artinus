package com.example.artinus.repository;

import com.example.artinus.domain.Member;
import com.example.artinus.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    @Query("SELECT h FROM SubscriptionHistory h " +
           "JOIN FETCH h.channel " +
           "WHERE h.member = :member " +
           "ORDER BY h.createdAt ASC")
    List<SubscriptionHistory> findByMemberWithChannel(@Param("member") Member member);

    List<SubscriptionHistory> findByMemberOrderByCreatedAtAsc(Member member);
}
