-- seed-users.sql
-- user201@gmail.com ~ user5000@gmail.com (4800명 추가)
-- 기존 user1~user200은 data.sql에서 생성되므로 제외
--
-- 실행 방법:
--   mysql -u root -p0000 open_ticket < seed-users.sql
--
-- 주의: local 프로파일에서 ddl-auto=create로 서버를 기동하면
--       테이블이 재생성되므로, 서버 기동 후 data.sql 반영이 완료된 뒤
--       이 SQL을 실행하세요.
--
-- 비밀번호 포맷: 평문 'password{N}' (서버 로그인 API와 일치해야 함)
-- 실제 운영에서는 BCrypt 해시값을 사용해야 하지만,
-- 테스트 환경에서는 평문 패스워드를 사용합니다.

INSERT INTO users (name, email, password, created_at, last_modified_at)
SELECT
    CONCAT('user', n)            AS name,
    CONCAT('user', n, '@gmail.com') AS email,
    CONCAT('password', n)        AS password,
    NOW()                        AS created_at,
    NOW()                        AS last_modified_at
FROM (
    SELECT @row := @row + 1 AS n
    FROM information_schema.columns c1,
         information_schema.columns c2,
         (SELECT @row := 200) r
    LIMIT 4800
) t;
