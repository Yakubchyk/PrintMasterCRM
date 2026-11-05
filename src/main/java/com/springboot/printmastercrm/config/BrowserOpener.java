package com.springboot.printmastercrm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** После старта открывает браузер, если включено printmaster.open-browser (его включают скрипты запуска и приложение .app). */
@Component
public class BrowserOpener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${printmaster.open-browser:false}")
    private boolean openBrowser;

    @Value("${server.port:8765}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (openBrowser) {
            BrowserSupport.open("http://localhost:" + port);
        }
    }
}
