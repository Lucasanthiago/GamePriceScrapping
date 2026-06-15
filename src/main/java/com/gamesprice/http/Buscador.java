package com.gamesprice.http;

import java.io.IOException;
import java.util.Map;
import org.jsoup.nodes.Document;

/**
 * Abstracao para obter o HTML de uma URL como um {@link Document} do Jsoup.
 *
 * <p>Existe para isolar as fontes da forma de busca: a implementacao real usa rede
 * (Jsoup), mas pode ser decorada com cache ou substituida por um duble em testes.
 */
public interface Buscador {

    /**
     * @param url     endereco a buscar
     * @param cookies cookies a enviar (ex.: {@code birthtime} na Steam); pode ser vazio
     * @return documento HTML ja parseado
     * @throws IOException em falha de rede ou status HTTP de erro
     */
    Document obter(String url, Map<String, String> cookies) throws IOException;
}
