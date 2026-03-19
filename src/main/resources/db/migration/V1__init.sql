CREATE TABLE channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL
);

CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    subscription_status VARCHAR(20) NOT NULL
);

CREATE TABLE subscription_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (channel_id) REFERENCES channels(id)
);

-- 초기 채널 데이터
INSERT INTO channels (name, type) VALUES ('홈페이지', 'BOTH');
INSERT INTO channels (name, type) VALUES ('모바일앱', 'BOTH');
INSERT INTO channels (name, type) VALUES ('네이버', 'SUBSCRIBE_ONLY');
INSERT INTO channels (name, type) VALUES ('SKT', 'SUBSCRIBE_ONLY');
INSERT INTO channels (name, type) VALUES ('콜센터', 'UNSUBSCRIBE_ONLY');
INSERT INTO channels (name, type) VALUES ('이메일', 'UNSUBSCRIBE_ONLY');
