package com.gamesprice.comparador;

import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Espelha {@link ComparadorPrecos}, mas sem termo de busca: consulta a pagina de
 * "destaques/promocoes" de cada {@link FonteLoja}, cruza pelo titulo normalizado e
 * devolve os jogos com maior desconto agora, limitados a um total fixo.
 */
public final class MaioresDescontos {

    private final List<FonteLoja> fontes;
    private final int limite;

    public MaioresDescontos(List<FonteLoja> fontes, int limite) {
        this.fontes = List.copyOf(fontes);
        this.limite = limite;
    }

    /** Jogos em maior desconto agora, ordenados do maior para o menor desconto. */
    public List<Jogo> destaques() {
        List<Oferta> todas = coletarDestaques();
        Map<String, Jogo> porTitulo = AgrupadorOfertas.agrupar(todas);
        return ordenarPorDesconto(porTitulo.values());
    }

    private List<Oferta> coletarDestaques() {
        List<Oferta> todas = new ArrayList<>();
        for (FonteLoja fonte : fontes) {
            todas.addAll(fonte.buscarDestaques());
        }
        return todas;
    }

    private List<Jogo> ordenarPorDesconto(Collection<Jogo> jogos) {
        List<Jogo> lista = new ArrayList<>();
        for (Jogo jogo : jogos) {
            if (jogo.maiorDesconto() > 0) {
                lista.add(jogo);
            }
        }
        lista.sort(Comparator.comparing(Jogo::maiorDesconto).reversed());
        return lista.size() > limite ? lista.subList(0, limite) : lista;
    }
}
