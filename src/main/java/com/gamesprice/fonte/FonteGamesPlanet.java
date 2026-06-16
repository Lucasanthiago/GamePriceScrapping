package com.gamesprice.fonte;

import com.gamesprice.cambio.ConversorMoeda;
import com.gamesprice.http.Buscador;
import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import com.gamesprice.util.Log;
import com.gamesprice.util.Precos;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Raspa a busca da GamesPlanet (loja US). A pagina de resultados e renderizada no
 * servidor: cada produto vem como {@code div.game_list_small} com titulo, link e preco
 * em {@code span}s no HTML cru (passou no "teste dos 10 segundos").
 *
 * <p>O dominio US entrega precos em <b>USD</b> ({@code $8.99}); por isso, como a Steam
 * faz com R$, o preco passa pelo {@link ConversorMoeda} para virar BRL e poder ser
 * comparado com as demais lojas.
 */
public final class FonteGamesPlanet implements FonteLoja {

    private static final String BASE = "https://us.gamesplanet.com";
    private static final String URL_BUSCA = BASE + "/search?query=";

    /** Mesma busca, filtrada para "game_special" e ordenada por maior desconto (cached_saving). */
    private static final String URL_DESTAQUES = BASE + "/search?av=rel&s=cached_saving&t=game%2Bgame_special";

    private static final String MOEDA = "USD";
    private static final Map<String, String> SEM_COOKIES = Map.of();

    private final Buscador buscador;
    private final ConversorMoeda conversor;

    public FonteGamesPlanet(Buscador buscador, ConversorMoeda conversor) {
        this.buscador = buscador;
        this.conversor = conversor;
    }

    @Override
    public Loja loja() {
        return Loja.GAMESPLANET;
    }

    @Override
    public List<Oferta> buscar(String termo) {
        String url = URL_BUSCA + URLEncoder.encode(termo, StandardCharsets.UTF_8);
        return buscarUrl(url, "para \"" + termo + "\"");
    }

    @Override
    public List<Oferta> buscarDestaques() {
        return buscarUrl(URL_DESTAQUES, "em destaque");
    }

    private List<Oferta> buscarUrl(String url, String descricaoLog) {
        List<Oferta> ofertas = new ArrayList<>();
        try {
            Document doc = buscador.obter(url, SEM_COOKIES);

            for (Element item : doc.select("div.game_list_small")) {
                Oferta oferta = extrairOferta(item);
                if (oferta != null) {
                    ofertas.add(oferta);
                }
            }
            Log.info("GamesPlanet: " + ofertas.size() + " ofertas " + descricaoLog);
        } catch (Exception e) {
            Log.erro("GamesPlanet falhou ao buscar " + descricaoLog, e);
        }
        return ofertas;
    }

    private Oferta extrairOferta(Element item) {
        Element tituloEl = item.selectFirst("h4 a");
        Element precoEl = item.selectFirst("span.price_current");
        if (tituloEl == null || precoEl == null) {
            return null; // cabecalho/anuncio sem produto ou sem preco utilizavel
        }

        String titulo = tituloEl.text().trim();
        BigDecimal precoFinalUsd = Precos.parsePonto(precoEl.text());
        if (titulo.isEmpty() || precoFinalUsd == null || precoFinalUsd.signum() <= 0) {
            return null;
        }
        BigDecimal precoFinalBRL = conversor.paraBRL(precoFinalUsd, MOEDA);

        // preco cheio (base) e o desconto so aparecem quando o jogo esta em promocao
        BigDecimal precoOriginalBRL = precoFinalBRL;
        Element baseEl = item.selectFirst("span.price_base");
        if (baseEl != null) {
            BigDecimal cheioUsd = Precos.parsePonto(baseEl.text());
            if (cheioUsd != null && cheioUsd.signum() > 0) {
                precoOriginalBRL = conversor.paraBRL(cheioUsd, MOEDA);
            }
        }

        int desconto = extrairDesconto(item, precoOriginalBRL, precoFinalBRL);
        String url = BASE + tituloEl.attr("href");

        return new Oferta(Loja.GAMESPLANET, titulo, precoFinalBRL, precoOriginalBRL, desconto, url, null);
    }

    /** Le o desconto do rotulo ("-10%"); se ausente, deriva dos precos cheio x final. */
    private int extrairDesconto(Element item, BigDecimal original, BigDecimal finalBRL) {
        Element label = item.selectFirst("span.price_saving");
        if (label != null) {
            String digitos = label.text().replaceAll("[^0-9]", "");
            if (!digitos.isEmpty()) {
                try {
                    long v = Long.parseLong(digitos);
                    return (int) Math.min(100, Math.max(0, v));
                } catch (NumberFormatException e) {
                    // digitos absurdos (mudanca de markup): cai para a derivacao por precos
                }
            }
        }
        if (original.signum() > 0 && original.compareTo(finalBRL) > 0) {
            return original.subtract(finalBRL)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(original, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }
}
