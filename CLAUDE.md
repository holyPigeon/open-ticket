# CLAUDE.md

## Git

- Default branch: `develop`

## Build & Test Commands

```bash
./gradlew build        # compile + test
./gradlew bootRun      # run app (local profile by default)
./gradlew test         # run all tests
./gradlew test --tests "com.example.openticket.domain.booking.BookingTest"  # single class test
./gradlew clean        # delete build artifacts + QueryDSL generated sources (src/main/generated)
```

## Architecture Overview

**Spring Boot 4.0.1 / Java 21** — `jakarta.*` namespaces, Jackson 3.x (`tools.jackson`), `@MockitoBean`

### Package Structure (`com.example.openticket`)

```
api/
  controller/<domain>/     # @RestController, request DTOs (@Valid)
  service/<domain>/        # business logic, service DTOs
  ApiResponse.java         # common response wrapper (code, status, message, data)
  ApiControllerAdvice.java # BindException→400, UnauthorizedException→401
domain/<domain>/           # JPA entities, repositories, enums
  BaseEntity               # @MappedSuperclass (createdAt, lastModifiedAt)
global/
  auth/                    # JwtProvider, AuthenticationInterceptor, @LoginUser
  config/                  # QueryDslConfig, WebConfig, JpaAuditingConfig
  exception/               # UnauthorizedException
```

### Authentication (JWT, no Spring Security)

- `AuthenticationInterceptor`: extracts token from `Authorization: Bearer <token>` header → validates via `JwtProvider` → stores userId as request attribute
- `LoginUserArgumentResolver`: injects current user into `@LoginUser User` parameters
- Excluded paths: `/api/v1/auth/login`, `/api/v1/users/signup`, `/api/v1/events/**`

### QueryDSL

- Generated sources location: `src/main/generated`
- `EventRepositoryImpl` handles dynamic search conditions (returns null BooleanExpression to skip condition)
- Pagination via `PageableExecutionUtils.getPage()`

### DB Configuration

- Local: MySQL (`localhost:3307/open_ticket`), `ddl-auto: create`, seed data via `data.sql`
- Test: H2 in-memory (`@ActiveProfiles("test")`)

## Key Patterns

### DTO Layering

- Controller request DTO → `.toServiceRequest()` → Service request DTO
- Response DTOs: static factory methods (`BookingResponse.of()`, `EventResponse.from()`)
- All DTOs use Java records

### Test Support Classes

- **`IntegrationTestSupport`**: `@SpringBootTest @Transactional @Import(QueryDslConfig.class)` — base for service/repository integration tests
- **`ControllerTestSupport`**: `@WebMvcTest` — MockMvc + `@MockitoBean` for controller slice tests
- Domain unit tests: plain JUnit without Spring context

### Entities

- `@NoArgsConstructor(access = PROTECTED)` + static factory methods/builders
- `BookingSeat`: join entity between Booking and Seat (`CascadeType.ALL`, `orphanRemoval = true`)

### API Response

- All controllers return `ApiResponse<T>` directly (no `ResponseEntity`)
- Tests assert `$.code` (int), `$.status` (string, e.g. "200 OK"), `$.message`, `$.data`

## Commit Convention

Conventional Commits format with **Korean noun-phrase style** subjects (max 50 chars, no trailing period):

```
<type>(<scope>): <Korean subject>

- Body in Korean, use bullet points
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `chore`, `perf`, `test`, `ci`
**Scopes**: `booking`, `event`, `seat`, `user`, `api`, `domain`, `service`, `config`, `global`

Example:
```
feat(booking): 좌석 예매 요청 API 구현

- `BookingController`에 예매 요청 엔드포인트(`POST /api/v1/bookings`) 추가
- 동시성 처리를 위해 `BookingService`에 락 획득 로직 적용
```
