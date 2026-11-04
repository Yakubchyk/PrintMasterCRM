package com.springboot.printmastercrm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Полный запуск контекста нужен живой PostgreSQL, поэтому тест выполняется только
 * если задана переменная окружения DB_URL (например, DB_URL=jdbc:postgresql://localhost:5433/postgres).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PrintMasterCrmApplicationTests {

    @Test
    void contextLoads() {
    }

}
