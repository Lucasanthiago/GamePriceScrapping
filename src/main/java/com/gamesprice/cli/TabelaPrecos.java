package com.gamesprice.cli;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.util.Precos;
import java.util.List;

/**
 * Formata o resultado da comparacao como texto para o terminal.
 *
 * <p>Separada da logica de comparacao para que a forma de apresentar (tabela, CSV,
 * HTML...) possa mudar sem tocar no comparador.
 */
public final class TabelaPrecos {

    private TabelaPrecos() {
    }

    /** Monta o relatorio completo de uma busca. */
    public static String render(String termo, List<Jogo> jogos) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Resultados para \"").append(termo).append("\" ===\n");

        if (jogos.isEmpty()) {
            sb.append("Nenhuma oferta encontrada nas lojas suportadas.\n");
            return sb.toString();
        }

        for (Jogo jogo : jogos) {
            sb.append(renderJogo(jogo));
        }
        sb.append(rodape(jogos));
        return sb.toString();
    }

    private static String renderJogo(Jogo jogo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(jogo.tituloExibicao());
        if (jogo.comparavel()) {
            sb.append("  [comparavel em ").append(jogo.quantidadeLojas()).append(" lojas]");
        }
        sb.append("\n");

        Oferta maisBarata = jogo.ofertaMaisBarata();
        sb.append("  ").append(linhaCabecalho()).append("\n");
        for (Oferta oferta : jogo.ofertasOrdenadas()) {
            boolean melhor = oferta == maisBarata;
            sb.append("  ").append(linhaOferta(oferta, melhor)).append("\n");
        }
        return sb.toString();
    }

    private static String linhaCabecalho() {
        return String.format("%-2s %-12s %-14s %-9s %-7s %s",
                "", "Loja", "Preco", "Desconto", "Nota", "Link");
    }

    private static String linhaOferta(Oferta oferta, boolean melhor) {
        String marcador = melhor ? ">>" : "  ";
        String desconto = oferta.temDesconto() ? "-" + oferta.descontoPerc() + "%" : "-";
        String nota = oferta.nota().map(n -> n + "%").orElse("-");
        return String.format("%-2s %-12s %-14s %-9s %-7s %s",
                marcador,
                oferta.loja().rotulo(),
                Precos.formatarBRL(oferta.precoBRL()),
                desconto,
                nota,
                oferta.url());
    }

    private static String rodape(List<Jogo> jogos) {
        long comparaveis = jogos.stream().filter(Jogo::comparavel).count();
        return String.format(
                "%n%d jogo(s) listado(s); %d com preco comparavel entre lojas.%n"
                        + "(>> marca a loja mais barata de cada jogo)%n",
                jogos.size(), comparaveis);
    }
}
