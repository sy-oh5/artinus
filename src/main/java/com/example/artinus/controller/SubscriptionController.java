package com.example.artinus.controller;

import com.example.artinus.dto.request.SubscribeRequestDto;
import com.example.artinus.dto.request.UnsubscribeRequestDto;
import com.example.artinus.dto.response.SubscriptionHistoryResponseDto;
import com.example.artinus.dto.response.SubscriptionResponseDto;
import com.example.artinus.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "구독 관리 API")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    @Operation(summary = "구독하기")
    public ResponseEntity<SubscriptionResponseDto> subscribe(@Valid @RequestBody SubscribeRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.subscribe(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "구독 해지")
    public ResponseEntity<SubscriptionResponseDto> unsubscribe(@Valid @RequestBody UnsubscribeRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.unsubscribe(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "구독 이력 조회")
    public ResponseEntity<SubscriptionHistoryResponseDto> getHistory(
            @RequestParam String name,
            @RequestParam String phoneNumber) {
        SubscriptionHistoryResponseDto response = subscriptionService.getHistory(name, phoneNumber);
        return ResponseEntity.ok(response);
    }
}
