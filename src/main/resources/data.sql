-- 1. 사용자 (User)
INSERT INTO users (name, email, password, created_at, last_modified_at)
VALUES ('박민석', 'user1@gmail.com', 'password1', NOW(), NOW()),
       ('최일구', 'user2@gmail.com', 'password2', NOW(), NOW()),
       ('성윤모', 'user3@gmail.com', 'password3', NOW(), NOW());

-- 2. 이벤트 (Event) - Category: CONCERT, SPORTS
INSERT INTO events (title, category, start_at, end_at, venue, created_at, last_modified_at)
VALUES ('아이유 콘서트', 'CONCERT', '2026-05-01 19:00:00', '2026-05-01 22:00:00', '잠실 주경기장', NOW(),
        NOW()),
       ('손흥민 은퇴 경기', 'SPORTS', '2026-06-15 20:00:00', '2026-06-15 23:00:00', '상암 월드컵경기장', NOW(),
        NOW());

-- 3. 좌석 (Seat) - Event 1(아이유 콘서트)에 대한 좌석
-- 상태: AVAILABLE(예약 가능), BOOKED(예약중), SOLD(판매 완료)
-- 주의: Seat 엔티티의 @JoinColumn(name = "concert_id") 반영
INSERT INTO seats (event_id, seat_number, price, seat_status, created_at, last_modified_at)
VALUES (1, 'A1', 150000, 'BOOKED', NOW(), NOW()),      -- 이미 팔린 좌석
       (1, 'A2', 150000, 'AVAILABLE', NOW(), NOW()), -- 구매 가능
       (1, 'A3', 150000, 'AVAILABLE', NOW(), NOW()), -- 구매 가능
       (1, 'B1', 120000, 'BOOKED', NOW(), NOW()),  -- 누군가 결제 진행 중 (점유)
       (1, 'B2', 120000, 'AVAILABLE', NOW(), NOW());

-- 4. 예매 내역 (Booking)
-- User 1(탄지로)가 Event 1의 Seat 1(A1)을 예매했다고 가정
-- 총 가격: 150000원
INSERT INTO bookings (user_id, total_price, booked_at, booking_status, created_at, last_modified_at)
VALUES (1, 150000, '2026-04-01 10:00:00', 'BOOKED', NOW(), NOW());

-- 5. 예매-좌석 매핑 (BookingSeat)
-- Booking 1과 Seat 1(A1) 연결
INSERT INTO booking_seats (booking_id, seat_id, price, created_at, last_modified_at)
VALUES (1, 1, 150000, NOW(), NOW());