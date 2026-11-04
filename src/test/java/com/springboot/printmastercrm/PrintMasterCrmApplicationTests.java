package com.springboot.printmastercrm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Полный запуск контекста пишет в рабочую базу в домашней папке, поэтому тест выполняется
 * только если задана PRINTMASTER_DATA (например, во временную папку):
 * PRINTMASTER_DATA=/tmp/pm-test ./gradlew test
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "PRINTMASTER_DATA", matches = ".+")
class PrintMasterCrmApplicationTests {

    @Test
    void contextLoads() {
    }
}
