# Git Commit Message Instructions

You must generate the commit message based on the following rules.

## 1. Commit Message Structure
Use the **Conventional Commits** format:
`<type>(<scope>): <subject>`
<BLANK LINE>
<body>

## 2. Rules for '<type>'
Use one of the following types (keep strictly in English):
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
- `refactor`: A code change that neither fixes a bug nor adds a feature (Includes removing unused files or code)
- `chore`: Changes to the build process, auxiliary tools, or deleting configuration files
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools and libraries
- `ci`: Changes to our CI configuration files and scripts

## 3. Rules for '<scope>' (Optional but Recommended)
Determine the scope based on the directory or domain changed.
Based on the project structure, use one of the following scopes:
- **Domains**: `booking`, `event`, `seat`, `user`
- **Layers**: `api`, `domain`, `service`, `config`
- **Global**: `global` (if the change affects multiple modules)

## 4. Rules for '<subject>' (Header)
- **Language**: Korean (한국어).
- **Style**: Use **"Noun-ending style (개조식)"**. Do not end with a verb or full sentence.
    - Good: `예약 생성 API 구현` (Implement booking creation API)
    - Bad: `예약 생성 API를 구현했습니다` (I implemented booking creation API)
    - Bad: `예약 생성 API 구현함` (Implemented booking creation API)
- **Length**: Keep it under 50 characters.
- **Punctuation**: Do not end with a period.

## 5. Rules for '<body>'
- **Language**: Korean (한국어).
- **Content**: Explain **what** and **why** (not just how).
- **Format**: Use bullet points (`- `) for multiple details.
- **Detail**: Mention specific class names or logic changes if necessary (e.g., `BookingService`의 트랜잭션 격리 수준 변경).

## 6. Examples
### Example 1 (Feature)
feat(booking): 좌석 예매 요청 API 구현

- `BookingController`에 예매 요청 엔드포인트(`POST /api/v1/bookings`) 추가
- 동시성 처리를 위해 `BookingService`에 락 획득 로직 적용
- 예매 완료 시 `BookingResponse` 반환하도록 DTO 매핑

### Example 2 (Bug Fix)
fix(seat): 좌석 중복 선택 시 예외가 발생하지 않는 오류 수정

- `SeatRepository` 조회 시 비관적 락(Pessimistic Lock) 적용
- 이미 예약된 좌석일 경우 `SeatAlreadyBookedException`을 던지도록 수정

### Example 3 (Refactor)
refactor(user): 회원 가입 로직을 도메인 서비스로 분리

- `UserService`의 비대해진 로직을 `UserDomainService`로 이관
- 회원 유효성 검사 로직(`UserValidator`) 분리

### Example 4 (Deletion)
refactor(event): 사용하지 않는 이벤트 리스너 삭제

- 더 이상 사용되지 않는 `OldEventListener` 클래스 삭제
- `EventService`에서 해당 리스너 호출 부분 제거