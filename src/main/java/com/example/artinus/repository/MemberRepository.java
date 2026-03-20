package com.example.artinus.repository;

import com.example.artinus.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);
}
