package com.example.openticket;

import com.example.openticket.config.QueryDslConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Import(QueryDslConfig.class)
public abstract class IntegrationTestSupport {

}