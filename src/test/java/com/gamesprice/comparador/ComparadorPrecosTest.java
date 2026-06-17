package com.gamesprice.comparador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.model.Jogo;
import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComparadorPrecosTest {

    /** Fonte falsa: devolve ofertas fixas, sem rede (testa o cruzamento isoladamente). */
    private static FonteLoja fonteFake(Loja loja, Oferta... ofertas) {
        return new FonteLoja() {
            @Override
            public Loja loja() {
                return loja;
            }

            @Override
            public List<Oferta> buscar(String termo) {
                return List.of(ofertas);
            }

            @Override
            public List<Oferta> buscarDestaques() {
                return List.of(ofertas);
            }
        };
    }

    private static Oferta oferta(Loja loja, String titulo, String preco) {
        return new Oferta(loja, titulo, new BigDecimal(preco), null, 0, "http://x", null);
    }

    @Test
    void cruzaTitulosEquivalentesDeLojasDiferentesNoMesmoJogo() {
        FonteLoja steam = fonteFake(Loja.STEAM, oferta(Loja.STEAM, "ELDEN RING", "274.50"));
        FonteLoja gg = fonteFake(Loja.GAMERSGATE,
                oferta(Loja.GAMERSGATE, "Elden Ring®", "306.39"));

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gg)).comparar("elden ring");

        assertEquals(1, jogos.size(), "as duas ofertas sao o mesmo jogo");
        Jogo elden = jogos.get(0);
        assertTrue(elden.comparavel());
        assertEquals(2, elden.quantidadeLojas());
        assertEquals(Loja.STEAM, elden.ofertaMaisBarata().loja());
        assertEquals(new BigDecimal("274.50"), elden.ofertaMaisBarata().precoBRL());
    }

    @Test
    void jogosComparaveisAparecemAntesDosExclusivos() {
        FonteLoja steam = fonteFake(Loja.STEAM,
                oferta(Loja.STEAM, "Barato Exclusivo", "10.00"),
                oferta(Loja.STEAM, "Jogo Comum", "200.00"));
        FonteLoja gg = fonteFake(Loja.GAMERSGATE,
                oferta(Loja.GAMERSGATE, "Jogo Comum", "150.00"));

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gg)).comparar("x");

        assertEquals("Jogo Comum", jogos.get(0).tituloExibicao(),
                "comparavel vem primeiro mesmo sendo mais caro que o exclusivo");
        assertEquals(new BigDecimal("150.00"), jogos.get(0).ofertaMaisBarata().precoBRL());
    }
}
