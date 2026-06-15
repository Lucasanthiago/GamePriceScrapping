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

    // --- Cambio (MVP usa taxa fixa) ---

    /** Taxa fixa USD->BRL usada quando uma loja entrega precos em dolar. */
    public static final BigDecimal USD_BRL = new BigDecimal("5.40");
}
