-- 샘플 회원 데이터 (phone_number는 AES 암호화됨, key: 1234567890123456)
-- 01012341234 -> HF5JS+lI8zjl1C8qQzOqPw==
-- 01012345678 -> nk1G6636AwE8zj8g+Px0jw==
-- 01056781234 -> 6cszmsa8sfnEXTj9U8SjXg==
INSERT INTO members (name, phone_number, subscription_status, created_at) VALUES
('홍길동', 'HF5JS+lI8zjl1C8qQzOqPw==', 'PREMIUM', '2026-01-01 10:00:00'),
('김철수', 'nk1G6636AwE8zj8g+Px0jw==', 'STANDARD', '2026-01-15 14:30:00'),
('이영희', '6cszmsa8sfnEXTj9U8SjXg==', 'NONE', '2026-02-01 09:00:00');

-- 샘플 구독 이력 데이터
INSERT INTO subscription_history (member_id, channel_id, action_type, previous_status, new_status, created_at) VALUES
-- 홍길동: 홈페이지에서 일반구독 (신규) -> 모바일앱에서 프리미엄 업그레이드
(1, 1, 'SUBSCRIBE', NULL, 'STANDARD', '2026-01-01 10:00:00'),
(1, 2, 'SUBSCRIBE', 'STANDARD', 'PREMIUM', '2026-02-01 11:00:00'),
-- 김철수: 네이버에서 일반구독 (신규)
(2, 3, 'SUBSCRIBE', NULL, 'STANDARD', '2026-01-15 14:30:00'),
-- 이영희: SKT에서 프리미엄 구독 (신규) -> 콜센터에서 해지
(3, 4, 'SUBSCRIBE', NULL, 'PREMIUM', '2026-02-01 09:00:00'),
(3, 5, 'UNSUBSCRIBE', 'PREMIUM', 'NONE', '2026-03-01 16:00:00');
