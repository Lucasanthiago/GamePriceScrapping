package com.gamesprice.web;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.ranking.ValeAPena;
import com.gamesprice.util.Precos;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Monta a estrutura (Map/List) da resposta da busca para a API web, ja com o ranking
 * "Vale a pena" embutido. Separa o mapeamento dominio -> JSON do transporte HTTP.
 */
public final class RespostaBusca {

    private RespostaBusca() {
    }

    public static Map<String, Object> montar(String termo, List<Jogo> jogos, Optional<BigDecimal> cotacaoUsdBrl) {
        Map<Jogo, ValeAPena.Pontuacao> ranking = new LinkedHashMap<>();
        for (ValeAPena.Pontuacao p : ValeAPena.ranquear(jogos)) {
            ranking.put(p.jogo(), p);
        }

        List<Object> lista = new ArrayList<>();
        for (Jogo jogo : jogos) {
            lista.add(jogoJson(jogo, ranking.get(jogo)));
        }

        long comparaveis = jogos.stream().filter(Jogo::comparavel).count();

        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("termo", termo);
        raiz.put("total", jogos.size());
        raiz.put("comparaveis", comparaveis);
        raiz.put("cotacaoUsdBrl", cotacaoUsdBrl.map(BigDecimal::toPlainString).orElse(null));
        raiz.put("jogos", lista);
        return raiz;
    }

    private static Map<String, Object> jogoJson(Jogo jogo, ValeAPena.Pontuacao pontuacao) {
        Oferta maisBarata = jogo.ofertaMaisBarata();

        Map<String, Object> j = new LinkedHashMap<>();
        j.put("titulo", jogo.tituloExibicao());
        j.put("comparavel", jogo.comparavel());
        j.put("quantidadeLojas", jogo.quantidadeLojas());
        j.put("lojaMaisBarata", maisBarata.loja().rotulo());
        j.put("precoMaisBarato", maisBarata.precoBRL());
        j.put("precoMaisBaratoFormatado", Precos.formatarBRL(maisBarata.precoBRL()));
        j.put("valeAPena", pontuacao == null ? null : pontuacao.valor());
        j.put("notaReviews", pontuacao == null ? null : pontuacao.notaReviews());

        List<Object> ofertas = new ArrayList<>();
        for (Oferta oferta : jogo.ofertasOrdenadas()) {
            ofertas.add(ofertaJson(oferta, oferta == maisBarata));
        }
        j.put("ofertas", ofertas);
        return j;
    }

    private static Map<String, Object> ofertaJson(Oferta oferta, boolean maisBarata) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("loja", oferta.loja().rotulo());
        o.put("tituloOriginal", oferta.tituloOriginal());
        o.put("preco", oferta.precoBRL());
        o.put("precoFormatado", Precos.formatarBRL(oferta.precoBRL()));
        o.put("precoOriginal", oferta.precoOriginalBRL());
        o.put("precoOriginalFormatado", Precos.formatarBRL(oferta.precoOriginalBRL()));
        o.put("desconto", oferta.descontoPerc());
        o.put("temDesconto", oferta.temDesconto());
        o.put("nota", oferta.notaReviewsPerc());
        o.put("url", oferta.url());
        o.put("maisBarata", maisBarata);
        return o;
    }
}
