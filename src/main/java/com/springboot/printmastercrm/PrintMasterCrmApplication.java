package com.springboot.printmastercrm;

import com.springboot.printmastercrm.config.BrowserSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrintMasterCrmApplication {

    public static void main(String[] args) {
        // Повторный запуск приложения (ярлык, .app): если программа уже работает — просто открыть браузер
        int port = Integer.parseInt(System.getProperty("server.port", "8765"));
        if (Boolean.getBoolean("printmaster.open-browser") && BrowserSupport.isRunning(port)) {
            BrowserSupport.open("http://localhost:" + port);
            return;
        }
        SpringApplication.run(PrintMasterCrmApplication.class, args);
    }

}
