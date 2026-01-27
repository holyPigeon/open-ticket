package com.example.openticket.support;

import com.example.openticket.config.QueryDslConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Import(QueryDslConfig.class)
//@Testcontainers
public abstract class IntegrationTestSupport {

//    @Container
//    @ServiceConnection
//    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//            .withDatabaseName("testdb")
//            .withUsername("test")
//            .withPassword("test")
//            .withCommand("--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci");
}