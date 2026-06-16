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
 * Raspa a busca da GamersGate. A loja e um app React, mas pre-renderiza os produtos
 * no HTML do GET, expondo atributos {@code data-*} limpos e confiaveis em cada
 * {@code div.catalog-item.product--item}: nome, preco, moeda e URL do produto.
 *
 * <p>A moeda pode variar conforme a geolocalizacao (BRL ou USD). Por isso o preco e
 * sempre passado pelo {@link ConversorMoeda}, que converte para BRL quando preciso.
 */
public final class FonteGamersGate implements FonteLoja {

    private static final String BASE = "https://www.gamersgate.com";
    private static final String URL_BUSCA = BASE + "/games/?query=";

    /** Mesma listagem de jogos, filtrada para itens em promocao. */
    private static final String URL_DESTAQUES = BASE + "/games/?on_sale=1";

    private static final Map<String, String> SEM_COOKIES = Map.of();

    private final Buscador buscador;
    private final ConversorMoeda conversor;

    public FonteGamersGate(Buscador buscador, ConversorMoeda conversor) {
        this.buscador = buscador;
        this.conversor = conversor;
    }

    @Override
    public Loja loja() {
        return Loja.GAMERSGATE;
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

            for (Element item : doc.select("div.catalog-item.product--item")) {
                Oferta oferta = extrairOferta(item);
                if (oferta != null) {
                    ofertas.add(oferta);
                }
            }
            Log.info("GamersGate: " + ofertas.size() + " ofertas " + descricaoLog);
        } catch (Exception e) {
            Log.erro("GamersGate falhou ao buscar " + descricaoLog, e);
        }
        return ofertas;
    }

    private Oferta extrairOferta(Element item) {
        String titulo = item.attr("data-name").trim();
        BigDecimal precoFinalOrigem = Precos.parsePonto(item.attr("data-price"));
        if (titulo.isEmpty() || precoFinalOrigem == null || precoFinalOrigem.signum() <= 0) {
            return null;
        }

        String moeda = item.attr("data-currency");
        BigDecimal precoFinalBRL = conversor.paraBRL(precoFinalOrigem, moeda);

        // preco cheio (full-price) so aparece quando ha desconto; vem na mesma moeda
        BigDecimal precoOriginalBRL = precoFinalBRL;
        Element fullPrice = item.selectFirst("div.catalog-item--full-price");
        if (fullPrice != null) {
            BigDecimal cheio = Precos.parsePonto(fullPrice.text());
            if (cheio != null) {
                precoOriginalBRL = conversor.paraBRL(cheio, moeda);
            }
        }

        int desconto = extrairDesconto(item, precoOriginalBRL, precoFinalBRL);
        String url = BASE + item.attr("data-url");

        return new Oferta(Loja.GAMERSGATE, titulo, precoFinalBRL, precoOriginalBRL, desconto, url, null);
    }

    /**
     * Le o desconto do rotulo ("-10%"); se ausente, deriva dos precos cheio x final.
     */
    private int extrairDesconto(Element item, BigDecimal original, BigDecimal finalBRL) {
        Element label = item.selectFirst("div.product--label-discount");
        if (label != null) {
            String digitos = label.text().replaceAll("[^0-9]", "");
            if (!digitos.isEmpty()) {
                return Math.min(100, Integer.parseInt(digitos));
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
