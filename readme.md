# Artinus 구독 서비스 API

구독 서비스 백엔드 API 서버입니다.

---

## 기술 스택

| 분류        | 기술              | 선택 이유                   |
|-----------|-----------------|-------------------------|
| Language  | Java 21         | 풍부한 생태계와 안정성, 최신 LTS 버전 |
| Framework | Spring Boot 3.4 | 엔터프라이즈 표준, 레퍼런스 풍부      |
| Database  | MySQL 8.0       | RDBMS 표준, 트랜잭션 보장       |
| ORM       | JPA (Hibernate) | Java 표준 ORM             |
| LLM       | Google Gemini   | 무료 티어 제공                |

### 주요 라이브러리

| 라이브러리        | 용도         | 선택 이유                           |
|--------------|------------|---------------------------------|
| QueryDSL     | 동적 쿼리      | 규모 확장 시 동적 쿼리 생성 용이, 타입 안전     |
| OpenFeign    | 외부 API 호출  | 선언적 HTTP 클라이언트, Spring Cloud 통합 |
| Spring Retry | 재시도 처리     | 외부 API 장애 대응, 백오프 지원            |
| MapStruct    | 객체 매핑      | 객체간 변환 용이                       |
| Flyway       | DB 마이그레이션  | 버전 관리                           |
| SpringDoc    | API 문서화    | Swagger UI 자동 생성                |
| Lombok       | 보일러플레이트 제거 | Getter/Setter/Builder 자동 생성     |

---

## 프로젝트 구조

**계층형 아키텍처(Layered Architecture)** 를 적용하여 관심사를 분리하고 유지보수성을 높였습니다.

```
Controller → Service → Repository
```

```
src/main/java/com/example/artinus/
├── config/          # 설정 파일
├── constant/        # Enum, 상수
├── controller/      # REST API 컨트롤러
├── domain/          # JPA 엔티티 (Member, Channel, SubscriptionHistory)
├── dto/             # 요청/응답 DTO
├── exception/       # 예외 관련
├── external/        # 외부 API 연동
│   ├── csrng/       # CSRNG API (Feign + Retry)
│   └── llm/         # LLM API (Gemini)
├── mapper/          # MapStruct 매퍼
├── repository/      # JPA Repository + QueryDSL
├── service/         # 비즈니스 로직
└── util/            # 유틸리티 (StringUtil, AesUtil)
```

---

## 실행 방법

### 로컬 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t artinus .

# 컨테이너 실행
docker run -p 8080:8080 \
  -e DB_HOST="${host}" \
  -e DB_USER_NAME={user} \
  -e DB_PASSWORD={password} \
  -e GEMINI_API_KEY={gemini_api_key} \
  -e ENCRYPTION_KEY={encryption_key} \
  artinus
```

### API 문서

서버 실행 후 Swagger UI 접속:

```
http://localhost:8080/swagger-ui.html
```

---

## API 명세

### 1. 구독하기 API

```
POST /api/subscriptions/subscribe
```

**요청 예시**

```json
{
  "name": "김이박",
  "phoneNumber": "01012345678",
  "channelId": 1,
  "targetStatus": "STANDARD"
}
```

**응답 예시**

```json
{
  "phoneNumber": "01012345678",
  "previousStatus": null,
  "newStatus": "STANDARD"
}
```

**구현 코드**

```java

@Transactional
public SubscriptionResponseDto subscribe(SubscribeRequestDto request) {
    // step 1: 채널 조회
    Channel channel = channelRepository.findById(request.getChannelId())
            .orElseThrow(() -> new CustomException(ExceptionType.CHANNEL_NOT_FOUND));

    // 해당 채널에서 구독이 허용되지 않는다면 에러
    if (!channel.getType().isCanSubscribe()) {
        throw new CustomException(ExceptionType.CHANNEL_SUBSCRIBE_NOT_ALLOWED);
    }

    // step 2: 회원 조회
    Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
            .orElse(null);

    SubscriptionStatus previousStatus = null;

    // step 3: 회원 구독 상태 업데이트
    if (member == null) {
        // 존재하지 않았던 회원이라면 생성
        member = Member.create(
                request.getName(),
                request.getPhoneNumber(),
                request.getTargetStatus()
        );
    } else {
        // 기존 회원인 경우 이름 검증 (보안상 동일한 에러 메시지)
        if (!member.getName().equals(request.getName())) {
            throw new CustomException(ExceptionType.MEMBER_NOT_FOUND);
        }

        previousStatus = member.getSubscriptionStatus();
        if (!member.isCanSubscribeTo(request.getTargetStatus())) {
            throw new CustomException(ExceptionType.INVALID_SUBSCRIPTION_STATUS);
        }
        member.changeSubscriptionStatus(request.getTargetStatus());
    }

    // step 4: 외부 API 처리
    csrngApiService.verifyExternalApi();

    // step 5: 회원 DB 저장
    memberRepository.save(member);

    // step 6: 이력 저장
    saveHistory(member, channel, ActionType.SUBSCRIBE, previousStatus, request.getTargetStatus());

    return SubscriptionResponseDto.builder()
            .phoneNumber(request.getPhoneNumber())
            .previousStatus(previousStatus)
            .newStatus(request.getTargetStatus())
            .build();
}
```

### 2. 구독 해지 API

```
POST /api/subscriptions/unsubscribe
```

**요청 예시**

```json
{
  "name": "김이박",
  "phoneNumber": "01012345678",
  "channelId": 1,
  "targetStatus": "NONE"
}
```

**응답 예시**

```json
{
  "phoneNumber": "01012345678",
  "previousStatus": "PREMIUM",
  "newStatus": "NONE"
}
```

### 3. 구독 이력 조회 API

```
GET /api/subscriptions/history?name={name}&phoneNumber={phoneNumber}
```

**응답 예시**

```json
{
  "memberName": "김이박",
  "currentStatus": "NONE",
  "memberCreatedAt": "2026-03-20T16:55:31",
  "history": [
    {
      "channelName": "홈페이지",
      "actionType": "SUBSCRIBE",
      "previousStatus": null,
      "newStatus": "STANDARD",
      "actionDate": "2026-03-20T16:55:32"
    },
    {
      "channelName": "홈페이지",
      "actionType": "SUBSCRIBE",
      "previousStatus": "STANDARD",
      "newStatus": "PREMIUM",
      "actionDate": "2026-03-20T17:30:01"
    },
    {
      "channelName": "홈페이지",
      "actionType": "UNSUBSCRIBE",
      "previousStatus": "PREMIUM",
      "newStatus": "NONE",
      "actionDate": "2026-03-20T17:31:29"
    }
  ],
  // LLM(Google Gemini)이 이력 데이터를 기반으로 생성한 자연어 요약
  "summary": "2026년 3월 20일, 김이박 회원은 홈페이지를 통해 일반 구독을 시작한 후 곧바로 프리미엄 구독으로 변경했습니다. 하지만 같은 날 최종적으로 구독을 해지한 이력이 있습니다."
}
```

---

## 외부 API 장애 대응

### Retry 전략

CSRNG 외부 API 호출 시 **Spring Retry**를 적용하여 장애에 대응합니다.

```java

@Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void verifyExternalApi() { ...}
```

| 설정          | 값      | 설명                    |
|-------------|--------|-----------------------|
| maxAttempts | 3      | 최대 3회 재시도             |
| delay       | 1000ms | 초기 대기 시간              |
| multiplier  | 2      | 지수 백오프 (1초 → 2초 → 4초) |

---

## 클라우드 아키텍처 설계

GCP Cloud Run 기반으로 배포합니다.

### Cloud Run 선택 이유

- **자동 CI/CD**: GitHub 연동으로 푸시 시 자동 빌드/배포
- **환경 변수 관리**: Cloud Run 콘솔에서 시크릿 관리
- **자동 스케일링**: 트래픽에 따라 0~N개 인스턴스 자동 조절
- **서버리스**: 인프라 관리 불필요, 사용한 만큼만 과금

### 배포 흐름

```
GitHub Push → Cloud Build (Dockerfile 기반 이미지 빌드) → Cloud Run 배포
```

- GitHub 저장소와 Cloud Run을 연동하여 main 브랜치 푸시 시 자동 배포
- 프로젝트 루트의 Dockerfile을 기반으로 컨테이너 이미지 빌드

---

## 테스트

```bash
# 전체 테스트 실행
./gradlew test
```

### 테스트 구성

| 종류     | 프레임워크             | 설명              |
|--------|-------------------|-----------------|
| 단위 테스트 | JUnit 5 + Mockito | Service 레이어 테스트 |
| DB 테스트 | H2 In-Memory      | 테스트용 인메모리 DB    |

---

## 보안

### 개인정보 암호화

- 휴대폰번호는 **AES 암호화**하여 DB에 저장
- JPA **AttributeConverter**를 활용하여 자동 암/복호화 처리

### 회원 검증

- 모든 API에서 **이름 + 휴대폰번호** 조합으로 회원 검증
- 실제 서비스에서는 불변의 고유 정보로 검증해야 하나, 본 과제에서는 이름으로 대체

### 민감 정보 관리

- API Key, DB 비밀번호 등 민감 정보는 **환경 변수**로 관리
- 소스코드에 하드코딩하지 않음
- GCP Cloud Run에서는 **환경 변수 설정**으로 시크릿 관리

---

## 확장성

### 인터페이스 분리

- **LLMService**를 인터페이스로 분리하여 다른 LLM(OpenAI 등)으로 교체 용이
- 현재는 GeminiLLMService 구현체 사용

### 동적 쿼리

- **QueryDSL** 적용으로 규모 확장 시 복잡한 동적 쿼리 생성 용이

### 인프라 확장

- Cloud Run **자동 스케일링**으로 트래픽 증가에 유연하게 대응

---

# 과제 요구사항

> 아래는 원본 과제 요구사항입니다.

## 개요

- 구독 서비스 백엔드 API를 설계하고 구현하는 과제입니다.
- 도메인 설계, API 구현, LLM 연동, 외부 API 장애 대응, 그리고 해당 시스템을 클라우드 환경에 배포/운영하기 위한 아키텍처 설계를 포함합니다.

---

## 도메인 정의

### 구독 상태

- 회원은 아래 3가지 구독 상태 중 1개의 상태만 가질 수 있습니다.

| 상태      | 설명            |
|---------|---------------|
| 구독 안함   | 구독하지 않은 상태    |
| 일반 구독   | 일반 등급 구독 상태   |
| 프리미엄 구독 | 프리미엄 등급 구독 상태 |

### 채널

- 채널이란 구독 및 해지를 수행할 수 있는 창구(접점)를 의미합니다.
- 채널은 아래 3가지 타입으로 구분됩니다.

| 타입          | 구독 | 해지 |
|-------------|----|----|
| 구독/해지 모두 가능 | O  | O  |
| 구독만 가능      | O  | X  |
| 해지만 가능      | X  | O  |

채널 예시

- 구독 서비스의 가입 및 해지는 여러 채널을 통해 이루어질 수 있습니다.

| 채널   | 구독 | 해지 |
|------|----|----|
| 홈페이지 | O  | O  |
| 모바일앱 | O  | O  |
| 네이버  | O  | X  |
| SKT  | O  | X  |
| 콜센터  | X  | O  |
| 이메일  | X  | O  |

### 외부 API (csrng)

- 구독하기 API와 구독 해지 API는 외부 API를 호출하고, 응답 결과에 따라 트랜잭션을 처리합니다.
- 호출 예시
    ```shell
    curl -X GET https://csrng.net/csrng/csrng.php?min=0&max=1
    ```
- 응답 예시
    ```json
    [{ "status": "success", "min": 0, "max": 1, "random": 1 }]
    ```
- `random` 값에 따른 처리

  | random 값 | 처리 |
                    |---|---|
  | `1` | 정상 처리 — 트랜잭션 커밋 |
  | `0` | 예외 발생 — 트랜잭션 롤백 |

---

## 요구사항

### 1. 구독하기 API

- 요청: 휴대폰번호, 채널 ID, 변경할 구독 상태
- 입력받은 채널이 구독 가능한 채널인 경우에만 구독할 수 있습니다.
- 최초 회원은 구독 안함, 일반 구독, 프리미엄 구독 중 어떤 상태로든 가입할 수 있습니다.
- 외부 API 호출 후, 응답에 따라 트랜잭션을 커밋 또는 롤백합니다.
- 구독 상태 변경 규칙

  | 현재 상태  | 변경 가능 상태              |
                    |--------|-----------------------|
  | 구독 안함  | 일반 구독, 프리미엄 구독        |
  | 일반 구독  | 프리미엄 구독               |
  | 프리미엄 구독 | _(변경 불가)_             |

### 2. 구독 해지 API

- 요청: 휴대폰번호, 채널 ID, 변경할 구독 상태
- 입력받은 채널이 해지 가능한 채널인 경우에만 해지할 수 있습니다.
- 외부 API 호출 후, 응답에 따라 트랜잭션을 커밋 또는 롤백합니다.
- 해지 상태 변경 규칙

  | 현재 상태 | 변경 가능 상태 |
                    |---|---|
  | 프리미엄 구독 | 일반 구독, 구독 안함 |
  | 일반 구독 | 구독 안함 |
  | 구독 안함 | _(변경 불가)_ |

### 3. 구독 이력 조회 API

- 요청: 휴대폰번호
- 응답:
    - 해당 회원의 구독하기, 구독해지 이력 목록(채널, 구독/해지날짜, 구독 상태 포함)
    - 이력 데이터를 기반으로 LLM API를 호출하여 생성한 자연어 요약
    - LLM API 선택은 자유입니다.
- 응답 예시:
    ```json
    {
      "history": [..],
      "summary": "2026년 1월 1일 홈페이지를 통해 일반 구독으로 가입한 뒤, 2월 1일 모바일앱에서 프리미엄 구독하였습니다. 3월 1일 콜센터를 통해 프리미엄 구독을 해지하여 구독 안함 상태입니다."
    }
    ```

### 기타

- 회원은 구독 및 해지를 여러 번 수행할 수 있습니다.
- 외부 API(csrng) 호출 시 발생할 수 있는 장애 상황에 대한 대응 전략을 구현해 주세요.
- 구현한 API 서버를 AWS 와 같은 클라우드 환경에 배포/운영한다고 가정하고, 아키텍처 및 구성, 보안, 확장성 등을 포함한 설계 문서를 포함해 주세요.
- 선택한 기술에 대한 근거, 분석 및 구현 내용 등은 `readme.md` 파일에 작성해 주세요.
- 요구사항에 명시되지 않은 부분은 일반적인 구독 서비스의 동작을 참고하여 자유롭게 구현해 주세요.

---

## 제약사항

- 언어, 프레임워크, 데이터베이스, 외부 API 등 모든 기술 선택에 제약이 없습니다.
- API Key 와 같은 인증 정보는 레포지토리에 포함되지 않도록 주의해 주세요.

---

## 평가 항목

- 아키텍처 설계 및 프로젝트 구성
- 요구사항 이해
- API 설계 및 구현
- 외부 API 장애 대응
- 클라우드 인프라 설계

---

## 제출 방법

- 안내 드린 마감일 전까지 github public repository URL을 아래 메일로 회신 부탁드립니다.
    - 메일: recruit@artinus.dev