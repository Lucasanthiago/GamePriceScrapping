package com.gamesprice.fonte;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Raspa a busca da Steam (HTML renderizado no servidor, em R$ para a regiao Brasil).
 *
 * <p>Tecnica central: seletores CSS sobre o HTML retornado por um GET simples.
 * Cada resultado e um {@code a.search_result_row} contendo titulo, bloco de preco
 * (com preco final em centavos no atributo {@code data-price-final} e desconto em
 * {@code data-discount}) e o resumo de avaliacoes.
 *
 * <p>O cookie {@code birthtime} evita o redirecionamento de verificacao de idade em
 * jogos adultos, garantindo que esses tambem aparecam na busca.
 */
public final class FonteSteam implements FonteLoja {

    private static final String URL_BUSCA = "https://store.steampowered.com/search/?term=";

    /** Mesma busca da Steam, filtrada para itens em promocao e ordenada por maior desconto. */
    private static final String URL_DESTAQUES =
            "https://store.steampowered.com/search/?specials=1&sort_by=Discount_DESC";

    /** birthtime no passado = "maior de idade", pula a tela de verificacao. */
    private static final Map<String, String> COOKIES = Map.of(
            "birthtime", "189302401",
            "mature_content", "1",
            "lastagecheckage", "1-0-1990");

    /** Captura o percentual de avaliacoes positivas no tooltip das reviews. */
    private static final Pattern PCT_REVIEWS =
            Pattern.compile("(\\d+)%\\s+of the");

    private final Buscador buscador;

    public FonteSteam(Buscador buscador) {
        this.buscador = buscador;
    }

    @Override
    public Loja loja() {
        return Loja.STEAM;
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
            Document doc = buscador.obter(url, COOKIES);

            for (Element linha : doc.select("a.search_result_row")) {
                Oferta oferta = extrairOferta(linha);
                if (oferta != null) {
                    ofertas.add(oferta);
                }
            }
            Log.info("Steam: " + ofertas.size() + " ofertas " + descricaoLog);
        } catch (Exception e) {
            Log.erro("Steam falhou ao buscar " + descricaoLog, e);
        }
        return ofertas;
    }

    private Oferta extrairOferta(Element linha) {
        Element tituloEl = linha.selectFirst("span.title");
        Element blocoPreco = linha.selectFirst("div.discount_block");
        if (tituloEl == null || blocoPreco == null) {
            return null; // linha de bundle/anuncio sem preco utilizavel
        }

        String titulo = tituloEl.text().trim();

        // preco final: data-price-final vem em centavos (ex.: 27450 = R$274,50)
        long centavosFinal = blocoPreco.attr("data-price-final").isEmpty()
                ? 0
                : parseLong(blocoPreco.attr("data-price-final"));
        if (centavosFinal <= 0) {
            return null; // gratis, "em breve" ou sem preco -> nao entra na comparacao
        }
        BigDecimal precoFinal = Precos.deCentavos(centavosFinal);

        int desconto = parseIntSeguro(blocoPreco.attr("data-discount"));

        // preco original: so existe no HTML quando ha desconto
        BigDecimal precoOriginal = precoFinal;
        Element originalEl = linha.selectFirst("div.discount_original_price");
        if (originalEl != null) {
            BigDecimal parsed = Precos.parseBrasileiro(originalEl.text());
            if (parsed != null) {
                precoOriginal = parsed;
            }
        }

        String url = limparUrl(linha.attr("href"));
        Integer nota = extrairNotaReviews(linha);

        return new Oferta(Loja.STEAM, titulo, precoFinal, precoOriginal, desconto, url, nota);
    }

    private Integer extrairNotaReviews(Element linha) {
        Element review = linha.selectFirst("span.search_review_summary");
        if (review == null) {
            return null;
        }
        Matcher m = PCT_REVIEWS.matcher(review.attr("data-tooltip-html"));
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** Remove o rastreador "?snr=..." deixando a URL limpa do produto. */
    private String limparUrl(String href) {
        int corte = href.indexOf("?snr=");
        return corte > 0 ? href.substring(0, corte) : href;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntSeguro(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
