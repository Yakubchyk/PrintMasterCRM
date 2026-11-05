package com.springboot.printmastercrm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

/** Служебные адреса: проверка «программа уже запущена» и кнопка «Выход» (корректно закрывает базу). */
@Controller
@RequiredArgsConstructor
public class AppController {

    private final ConfigurableApplicationContext context;

    @GetMapping("/app/ping")
    @ResponseBody
    public String ping() {
        return "printmaster";
    }

    @PostMapping(value = "/app/shutdown", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> shutdown(@RequestHeader(value = "Sec-Fetch-Site", required = false) String site) {
        // запрос с чужого сайта в браузере не принимаем
        if ("cross-site".equals(site)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Запрещено");
        }
        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int code = SpringApplication.exit(context, () -> 0);
            System.exit(code);
        }, "printmaster-shutdown");
        stopper.setDaemon(false);
        stopper.start();
        return ResponseEntity.ok("""
                <!DOCTYPE html><html lang="ru"><head><meta charset="UTF-8"><title>PrintMasterCRM остановлена</title>
                <style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#f6f1e3;
                font:16px -apple-system,Segoe UI,Arial,sans-serif;color:#3f4e3a;text-align:center}
                div{background:#fffdf7;padding:40px 56px;border-radius:22px;box-shadow:0 10px 30px rgba(86,80,50,.15)}
                h1{margin:0 0 8px;font-size:24px}p{margin:0;color:#6d7261}</style></head>
                <body><div><h1>Программа остановлена</h1><p>Данные сохранены. Эту вкладку можно закрыть.</p></div></body></html>
                """);
    }
}
