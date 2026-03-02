/**
 * generate-tokens.js — 테스트용 JWT 사전 생성 스크립트
 *
 * DB에서 테스트 유저 목록을 조회한 뒤 JWT를 생성하여 tokens.csv로 저장합니다.
 * k6 load test 실행 전에 한 번만 실행하면 됩니다.
 *
 * 사전 준비:
 *   npm install jsonwebtoken mysql2
 *
 * 실행:
 *   node generate-tokens.js
 *
 * 환경 변수 (기본값은 application.yml 로컬 설정과 일치):
 *   JWT_SECRET    = 'secret-key-must-be-very-long-for-security-reasons'
 *   JWT_EXPIRY    = '24h'
 *   DB_HOST       = 'localhost'
 *   DB_PORT       = 3307
 *   DB_USER       = 'root'
 *   DB_PASS       = '0000'
 *   DB_NAME       = 'open_ticket'
 *   USER_PATTERN  = '^user[0-9]+@gmail\\.com'
 *   MIN_USER_ID   = 2   (userId 1은 본인 계정이므로 제외)
 *   OUTPUT        = 'tokens.csv'
 */

const jwt    = require('jsonwebtoken');
const mysql  = require('mysql2/promise');
const fs     = require('fs');
const path   = require('path');

const JWT_SECRET   = process.env.JWT_SECRET  || 'secret-key-must-be-very-long-for-security-reasons';
const JWT_EXPIRY   = process.env.JWT_EXPIRY  || '24h';
const DB_HOST      = process.env.DB_HOST     || 'localhost';
const DB_PORT      = Number(process.env.DB_PORT || 3307);
const DB_USER      = process.env.DB_USER     || 'root';
const DB_PASS      = process.env.DB_PASS     || '0000';
const DB_NAME      = process.env.DB_NAME     || 'open_ticket';
const USER_PATTERN = process.env.USER_PATTERN || '^user[0-9]+@gmail\\.com';
const MIN_USER_ID  = Number(process.env.MIN_USER_ID || 2);
const OUTPUT       = process.env.OUTPUT      || 'tokens.csv';

async function main() {
  const connection = await mysql.createConnection({
    host:     DB_HOST,
    port:     DB_PORT,
    user:     DB_USER,
    password: DB_PASS,
    database: DB_NAME,
  });

  try {
    console.log(`DB 접속 완료: ${DB_HOST}:${DB_PORT}/${DB_NAME}`);

    const [rows] = await connection.execute(
      'SELECT id, email FROM users WHERE email REGEXP ? AND id >= ? ORDER BY id',
      [USER_PATTERN, MIN_USER_ID]
    );

    console.log(`유저 ${rows.length}명 조회 완료`);

    const lines = ['userId,email,token'];

    for (const row of rows) {
      const token = jwt.sign(
        { sub: String(row.id) },
        JWT_SECRET,
        { algorithm: 'HS256', expiresIn: JWT_EXPIRY }
      );
      lines.push(`${row.id},${row.email},${token}`);
    }

    const outputPath = path.resolve(OUTPUT);
    fs.writeFileSync(outputPath, lines.join('\n') + '\n', 'utf8');

    console.log(`tokens.csv 저장 완료: ${outputPath} (${rows.length}행)`);
    console.log(`확인: wc -l ${OUTPUT}`);
  } finally {
    await connection.end();
  }
}

main().catch((err) => {
  console.error('오류 발생:', err.message);
  process.exit(1);
});
