package com.gamesprice.cambio;

import com.gamesprice.config.Config;
import com.gamesprice.util.Log;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/**
 * Conversor que usa a cotacao <b>USD/BRL ao vivo</b>, buscada de uma fonte publica via GET
 * simples (mesma tecnica do projeto: HTTP + extracao do valor por padrao textual). A cotacao
 * e cacheada por um TTL ({@link Config#COTACAO_TTL}) para nao bater na fonte a cada preco.
 *
 * <p>Degrada com seguranca: qualquer falha de rede/parse cai no conversor de fallback (a
 * {@link TaxaFixaConversor}), de modo que a comparacao nunca quebra por causa do cambio.
 * Moedas diferentes de USD (BRL, desconhecidas) sao sempre delegadas ao fallback.
 *
 * <p>A fonte ({@code economia.awesomeapi.com.br}) responde JSON; por isso este conversor faz
 * seu proprio GET (com {@code ignoreContentType}) em vez de usar o {@code Buscador}, que
 * devolve HTML. Mantem as boas praticas (User-Agent e timeout da {@link Config}).
 */
public final class CotacaoAoVivoConversor implements ConversorMoeda {

    /** Captura o valor de compra ("bid") no JSON: ...,"bid":"5.0481",... */
    private static final Pattern BID = Pattern.compile("\"bid\"\\s*:\\s*\"([0-9]+(?:\\.[0-9]+)?)\"");

    private final ConversorMoeda fallback;
    private final String url;
    private final Duration ttl;

    private volatile BigDecimal taxaCache;     // ultima taxa boa obtida (ou null)
    private volatile Instant ultimaTentativa;  // quando tentamos buscar (sucesso OU falha)
    private final AtomicBoolean buscando = new AtomicBoolean(false);

    public CotacaoAoVivoConversor(ConversorMoeda fallback) {
        this(fallback, Config.URL_COTACAO_USD_BRL, Config.COTACAO_TTL);
    }

    public CotacaoAoVivoConversor(ConversorMoeda fallback, String url, Duration ttl) {
        this.fallback = fallback;
        this.url = url;
        this.ttl = ttl;
    }

    @Override
    public BigDecimal paraBRL(BigDecimal valor, String moeda) {
        if (valor == null) {
            return null;
        }
        String codigo = moeda == null ? "BRL" : moeda.trim().toUpperCase();
        if (!"USD".equals(codigo)) {
            return fallback.paraBRL(valor, moeda);
        }
        BigDecimal taxa = taxaAtual();
        if (taxa == null) {
            return fallback.paraBRL(valor, moeda);
        }
        return valor.multiply(taxa).setScale(2, RoundingMode.HALF_UP);
    }

    /** Cotacao USD/BRL em uso (cacheada ou recem-buscada); vazia se indisponivel. */
    public Optional<BigDecimal> cotacaoUsdBrl() {
        return Optional.ofNullable(taxaAtual());
    }

    /**
     * Taxa em uso (cacheada ou recem-buscada); pode ser null se a fonte nunca respondeu.
     *
     * <p>Nao bloqueia: a busca de rede roda <b>fora</b> de qualquer lock e em regime
     * "single-flight" (so um fetch por vez via {@link AtomicBoolean}); as demais threads
     * recebem o cache atual na hora. O timestamp da tentativa e gravado mesmo em falha
     * (cache negativo), entao uma fonte fora do ar nao e re-consultada a cada preco — so
     * depois que o TTL expira.
     */
    private BigDecimal taxaAtual() {
        Instant tentativa = ultimaTentativa;
        boolean fresca = tentativa != null && tentativa.plus(ttl).isAfter(Instant.now());

        if (!fresca && buscando.compareAndSet(false, true)) {
            try {
                BigDecimal nova = buscar();        // rede fora do lock
                ultimaTentativa = Instant.now();   // marca a tentativa (ate em falha)
                if (nova != null) {
                    taxaCache = nova;              // mantem a ultima boa se a nova falhar
                }
            } finally {
                buscando.set(false);
            }
        }
        return taxaCache;
    }

    private BigDecimal buscar() {
        try {
            String corpo = Jsoup.connect(url)
                    .userAgent(Config.USER_AGENT)
                    .timeout((int) Config.TIMEOUT.toMillis())
                    .ignoreContentType(true)
                    .execute()
                    .body();
            BigDecimal taxa = extrairTaxa(corpo);
            if (taxa != null) {
                Log.info("Cotacao USD/BRL ao vivo: " + taxa);
            }
            return taxa;
        } catch (Exception e) {
            Log.aviso("nao foi possivel obter a cotacao ao vivo: " + e.getMessage());
            return null;
        }
    }

    /** Extrai a taxa do corpo da resposta. Exposto para teste sem rede. Pode ser null. */
    public static BigDecimal extrairTaxa(String corpo) {
        if (corpo == null) {
            return null;
        }
        Matcher m = BID.matcher(corpo);
        if (!m.find()) {
            return null;
        }
        try {
            BigDecimal taxa = new BigDecimal(m.group(1));
            return taxa.signum() > 0 ? taxa : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
