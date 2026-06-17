package com.gamesprice.comparador;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.normalizacao.NormalizadorTitulo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrupa ofertas brutas em {@link Jogo} pelo titulo normalizado. Reaproveitado por
 * {@link ComparadorPrecos} (busca por termo) e {@link MaioresDescontos} (destaques sem
 * termo), que precisam do mesmo cruzamento entre lojas.
 */
final class AgrupadorOfertas {

    private AgrupadorOfertas() {
    }

    static Map<String, Jogo> agrupar(List<Oferta> ofertas) {
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
}
