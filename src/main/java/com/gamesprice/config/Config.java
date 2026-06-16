package com.gamesprice.config;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuracao central do aplicativo (constantes de scraping e de negocio).
 *
 * <p>Concentra tudo que um avaliador/desenvolvedor pode querer ajustar: identificacao
 * do robo, timeouts, educacao com o servidor e a taxa de cambio do MVP.
 */
public final class Config {

    private Config() {
    }

    // --- Boas praticas de scraping ---

    /** User-Agent que identifica o projeto (transparencia com a loja). */
    public static final String USER_AGENT =
            "GamesPriceScraper/1.0 (trabalho academico; +https://github.com/exemplo/comparador-precos-jogos)";

    /** Timeout de conexao/leitura de cada requisicao. */
    public static final Duration TIMEOUT = Duration.ofSeconds(15);

    /** Pausa entre requisicoes a lojas diferentes, para nao sobrecarregar servidores. */
    public static final Duration PAUSA_ENTRE_REQUISICOES = Duration.ofMillis(1200);

    // --- Cache local (evita bater no servidor a cada teste) ---

    public static final boolean CACHE_HABILITADO = true;
    public static final Duration CACHE_TTL = Duration.ofHours(6);

    // --- Cambio (MVP usa taxa fixa; extra: cotacao ao vivo) ---

    /** Taxa fixa USD->BRL usada quando uma loja entrega precos em dolar (fallback). */
    public static final BigDecimal USD_BRL = new BigDecimal("5.40");

    /** Fonte SSR da cotacao USD/BRL ao vivo (JSON da AwesomeAPI, GET simples). */
    public static final String URL_COTACAO_USD_BRL = "https://economia.awesomeapi.com.br/last/USD-BRL";

    /** Quanto tempo a cotacao ao vivo vale antes de ser buscada de novo. */
    public static final Duration COTACAO_TTL = Duration.ofHours(1);

    // --- Cruzamento aproximado de titulos (extra) ---

    /**
     * Limiar de similaridade (0..1, Levenshtein) para fundir titulos quase iguais entre
     * lojas (ex.: "The Witcher 3" x "Witcher 3"). 0 ou menos desliga o match aproximado.
     */
    public static final double LIMIAR_SIMILARIDADE = 0.84;

    // --- Ranking "Vale a pena" (extra: menor preco + nota das reviews) ---

    /** Peso do preco no score de custo-beneficio (preco + nota somam 1.0). */
    public static final double PESO_PRECO = 0.6;

    /** Peso da nota das reviews no score de custo-beneficio. */
    public static final double PESO_NOTA = 0.4;

    /** Nota neutra (0..1) atribuida a jogos sem avaliacao, para nao premiar nem punir. */
    public static final double NOTA_NEUTRA = 0.70;

    // --- Servidor web (UI) ---

    /** Porta padrao do servidor web embutido (modo --web). */
    public static final int PORTA_WEB = 8080;

    /** Quantos jogos aparecem na secao "Maiores descontos do momento" da home. */
    public static final int QTD_MAIORES_DESCONTOS = 12;
}
