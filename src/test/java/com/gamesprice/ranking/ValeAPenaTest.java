package com.gamesprice.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import com.gamesprice.normalizacao.NormalizadorTitulo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValeAPenaTest {

    private static Jogo jogo(String titulo, String preco, Integer nota) {
        Jogo j = new Jogo(NormalizadorTitulo.normalizar(titulo), titulo);
        j.adicionarOferta(new Oferta(Loja.STEAM, titulo, new BigDecimal(preco), null, 0, "http://x", nota));
        return j;
    }

    @Test
    void maisBaratoEbemAvaliadoVemPrimeiro() {
        Jogo barato = jogo("Barato Bom", "10.00", 90);
        Jogo caro = jogo("Caro Bom", "100.00", 90);

        List<ValeAPena.Pontuacao> ranking = ValeAPena.ranquear(List.of(caro, barato));

        assertEquals(barato, ranking.get(0).jogo(), "o mais barato (mesma nota) vale mais a pena");
        // barato: 100*(0.6*1 + 0.4*0.9) = 96.0 ; caro: 100*(0.6*0 + 0.4*0.9) = 36.0
        assertEquals(96.0, ranking.get(0).valor(), 0.001);
        assertEquals(36.0, ranking.get(1).valor(), 0.001);
    }

    @Test
    void notaPesaQuandoOsPrecosSaoIguais() {
        Jogo bemAvaliado = jogo("A", "50.00", 95);
        Jogo malAvaliado = jogo("B", "50.00", 40);

        List<ValeAPena.Pontuacao> ranking = ValeAPena.ranquear(List.of(malAvaliado, bemAvaliado));

        assertEquals(bemAvaliado, ranking.get(0).jogo(), "com mesmo preco, a melhor nota desempata");
    }

    @Test
    void jogoSemNotaUsaNotaNeutra() {
        Jogo unico = jogo("Sem Reviews", "20.00", null);

        ValeAPena.Pontuacao p = ValeAPena.ranquear(List.of(unico)).get(0);

        // preco unico -> componentePreco 1.0 ; nota neutra 0.70 -> 100*(0.6 + 0.4*0.70) = 88.0
        assertEquals(88.0, p.valor(), 0.001);
        assertNull(p.notaReviews(), "sem reviews, a nota exibida e nula");
    }

    @Test
    void melhorNotaEntreAsLojas() {
        Jogo j = new Jogo("multi", "Multi");
        j.adicionarOferta(new Oferta(Loja.STEAM, "Multi", new BigDecimal("30.00"), null, 0, "http://a", 70));
        j.adicionarOferta(new Oferta(Loja.GAMERSGATE, "Multi", new BigDecimal("25.00"), null, 0, "http://b", null));

        assertEquals(70, ValeAPena.melhorNota(j));
    }
}
