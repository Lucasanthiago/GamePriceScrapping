package com.gamesprice.ranking;

import com.gamesprice.config.Config;
import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Ranking "Vale a pena": combina <b>menor preco</b> e <b>nota das reviews</b> num unico
 * score de custo-beneficio (0..100), respondendo "qual jogo da lista compensa mais?".
 *
 * <p>Para cada jogo usa-se a oferta mais barata e a melhor nota disponivel entre as lojas.
 * O preco e normalizado dentro do conjunto pesquisado (o mais barato vale 1, o mais caro 0),
 * de modo que o ranking so faz sentido relativo aos resultados daquela busca. A nota entra
 * como fracao de 0..1; jogos sem avaliacao recebem uma nota neutra ({@link Config#NOTA_NEUTRA})
 * para nao serem injustamente premiados nem punidos.
 *
 * <pre>
 *   score = 100 * (PESO_PRECO * componentePreco + PESO_NOTA * componenteNota)
 * </pre>
 *
 * <p>Classe utilitaria sem estado.
 */
public final class ValeAPena {

    private ValeAPena() {
    }

    /**
     * Score de custo-beneficio de um jogo dentro do conjunto pesquisado, com os
     * componentes que o originaram (uteis para explicar o ranking na UI).
     *
     * @param jogo            jogo pontuado
     * @param valor           score final 0..100 (maior = compensa mais)
     * @param componentePreco quao barato e (0..1; 1 = o mais barato do conjunto)
     * @param componenteNota  quao bem avaliado e (0..1; nota/100, ou neutra se sem reviews)
     * @param notaReviews     melhor nota de reviews encontrada, ou null se nenhuma loja expoe
     */
    public record Pontuacao(
            Jogo jogo,
            double valor,
            double componentePreco,
            double componenteNota,
            Integer notaReviews) {
    }

    /**
     * Ranqueia os jogos do mais ao menos "vale a pena". Jogos sem oferta sao ignorados.
     *
     * @return pontuacoes ordenadas por score decrescente (empate: mais barato e depois titulo)
     */
    public static List<Pontuacao> ranquear(Collection<Jogo> jogos) {
        List<Jogo> comOferta = new ArrayList<>();
        for (Jogo jogo : jogos) {
            if (jogo.ofertaMaisBarata() != null) {
                comOferta.add(jogo);
            }
        }

        BigDecimal min = null;
        BigDecimal max = null;
        for (Jogo jogo : comOferta) {
            BigDecimal preco = jogo.ofertaMaisBarata().precoBRL();
            if (min == null || preco.compareTo(min) < 0) {
                min = preco;
            }
            if (max == null || preco.compareTo(max) > 0) {
                max = preco;
            }
        }

        List<Pontuacao> ranking = new ArrayList<>();
        for (Jogo jogo : comOferta) {
            ranking.add(pontuar(jogo, min, max));
        }

        ranking.sort(
                Comparator.comparingDouble(Pontuacao::valor).reversed()
                        .thenComparing(p -> p.jogo().ofertaMaisBarata().precoBRL())
                        .thenComparing(p -> p.jogo().tituloExibicao()));
        return ranking;
    }

    /**
     * Pontua um jogo dado o intervalo de precos [min, max] do conjunto. Exposto para testes
     * e para quem queira pontuar um jogo isolado contra uma faixa conhecida.
     */
    public static Pontuacao pontuar(Jogo jogo, BigDecimal min, BigDecimal max) {
        Oferta maisBarata = jogo.ofertaMaisBarata();
        double componentePreco = normalizarPreco(maisBarata.precoBRL(), min, max);

        Integer nota = melhorNota(jogo);
        double componenteNota = nota != null ? nota / 100.0 : Config.NOTA_NEUTRA;

        double valor = 100.0 * (Config.PESO_PRECO * componentePreco + Config.PESO_NOTA * componenteNota);
        return new Pontuacao(jogo, arredondar(valor), componentePreco, componenteNota, nota);
    }

    /** Melhor (maior) nota de reviews entre as ofertas do jogo, ou null se nenhuma tem. */
    public static Integer melhorNota(Jogo jogo) {
        Integer melhor = null;
        for (Oferta oferta : jogo.ofertasOrdenadas()) {
            Integer nota = oferta.notaReviewsPerc();
            if (nota != null && (melhor == null || nota > melhor)) {
                melhor = nota;
            }
        }
        return melhor;
    }

    /** 1 para o mais barato, 0 para o mais caro; 1 quando todos custam o mesmo. */
    private static double normalizarPreco(BigDecimal preco, BigDecimal min, BigDecimal max) {
        if (min == null || max == null || max.compareTo(min) == 0) {
            return 1.0;
        }
        double faixa = max.subtract(min).doubleValue();
        double acima = preco.subtract(min).doubleValue();
        return 1.0 - (acima / faixa);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
