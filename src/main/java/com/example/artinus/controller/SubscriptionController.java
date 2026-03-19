package com.example.artinus.controller;

import com.example.artinus.dto.request.SubscribeRequestDto;
import com.example.artinus.dto.request.UnsubscribeRequestDto;
import com.example.artinus.dto.response.SubscriptionHistoryResponseDto;
import com.example.artinus.dto.response.SubscriptionResponseDto;
import com.example.artinus.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionResponseDto> subscribe(@Valid @RequestBody SubscribeRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.subscribe(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<SubscriptionResponseDto> unsubscribe(@Valid @RequestBody UnsubscribeRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.unsubscribe(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<SubscriptionHistoryResponseDto> getHistory(@RequestParam String phoneNumber) {
        SubscriptionHistoryResponseDto response = subscriptionService.getHistory(phoneNumber);
        return ResponseEntity.ok(response);
    }
}
