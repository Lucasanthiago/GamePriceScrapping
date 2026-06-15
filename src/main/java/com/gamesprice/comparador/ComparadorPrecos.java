package com.gamesprice.comparador;

import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.normalizacao.NormalizadorTitulo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a comparacao: roda todas as {@link FonteLoja} para um termo, cruza as
 * ofertas pelo titulo normalizado e devolve os jogos com suas ofertas agrupadas.
 *
 * <p>O cruzamento e feito aqui, no codigo: nao vem pronto de nenhuma loja. A chave de
 * agrupamento e o titulo normalizado por {@link NormalizadorTitulo}.
 */
public final class ComparadorPrecos {

    private final List<FonteLoja> fontes;

    public ComparadorPrecos(List<FonteLoja> fontes) {
        this.fontes = List.copyOf(fontes);
    }

    /**
     * Busca o termo em todas as fontes e cruza os resultados.
     *
     * @return jogos encontrados, ordenados: primeiro os comparaveis (em mais de uma
     *         loja), depois pelo menor preco.
     */
    public List<Jogo> comparar(String termo) {
        List<Oferta> todas = coletarOfertas(termo);
        Map<String, Jogo> porTitulo = agruparPorTituloNormalizado(todas);
        return ordenar(porTitulo.values());
    }

    private List<Oferta> coletarOfertas(String termo) {
        List<Oferta> todas = new ArrayList<>();
        for (FonteLoja fonte : fontes) {
            todas.addAll(fonte.buscar(termo));
        }
        return todas;
    }

    private Map<String, Jogo> agruparPorTituloNormalizado(List<Oferta> ofertas) {
        // LinkedHashMap preserva a ordem de descoberta antes da ordenacao final.
        Map<String, Jogo> mapa = new LinkedHashMap<>();
        for (Oferta oferta : ofertas) {
            String chave = NormalizadorTitulo.normalizar(oferta.tituloOriginal());
            if (chave.isEmpty()) {
                continue;
            }
            Jogo jogo = mapa.computeIfAbsent(chave, k -> new Jogo(k, oferta.tituloOriginal()));
            jogo.adicionarOferta(oferta);
        }
        return mapa;
    }

    private List<Jogo> ordenar(java.util.Collection<Jogo> jogos) {
        List<Jogo> lista = new ArrayList<>(jogos);
        lista.sort(
                Comparator.comparing(Jogo::comparavel).reversed()
                        .thenComparing(j -> j.ofertaMaisBarata().precoBRL()));
        return lista;
    }
}
