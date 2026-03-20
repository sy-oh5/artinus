package com.example.artinus.repository;

import com.example.artinus.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {
}
