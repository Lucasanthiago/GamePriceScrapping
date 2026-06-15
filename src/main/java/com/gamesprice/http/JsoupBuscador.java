package com.gamesprice.http;

import com.gamesprice.config.Config;
import com.gamesprice.util.Log;
import java.io.IOException;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Implementacao de {@link Buscador} baseada em Jsoup, aplicando as boas praticas de
 * scraping num so lugar: User-Agent identificavel, timeout e uma pausa de cortesia
 * entre requisicoes para nao sobrecarregar os servidores.
 */
public final class JsoupBuscador implements Buscador {

    @Override
    public Document obter(String url, Map<String, String> cookies) throws IOException {
        pausaDeCortesia();
        Log.info("GET " + url);
        return Jsoup.connect(url)
                .userAgent(Config.USER_AGENT)
                .timeout((int) Config.TIMEOUT.toMillis())
                .cookies(cookies)
                .followRedirects(true)
                .get();
    }

    private void pausaDeCortesia() {
        try {
            Thread.sleep(Config.PAUSA_ENTRE_REQUISICOES.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
