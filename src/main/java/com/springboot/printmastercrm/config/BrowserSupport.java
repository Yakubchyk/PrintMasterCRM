package com.springboot.printmastercrm.config;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Открытие браузера и проверка, не запущена ли программа уже. */
public final class BrowserSupport {

    private BrowserSupport() {
    }

    /** true, если на этом порту уже отвечает именно PrintMasterCRM. */
    public static boolean isRunning(int port) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/app/ping").toURL().openConnection();
            c.setConnectTimeout(700);
            c.setReadTimeout(700);
            if (c.getResponseCode() != 200) {
                return false;
            }
            return "printmaster".equals(new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            return false;
        }
    }

    /** Открывает адрес в браузере по умолчанию (macOS, Windows, Linux). */
    public static void open(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            System.err.println("Не удалось открыть браузер, откройте вручную: " + url);
        }
    }
}
