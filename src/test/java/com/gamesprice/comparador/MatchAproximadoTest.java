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

class MatchAproximadoTest {

    private static FonteLoja fonteFake(Loja loja, String titulo, String preco) {
        Oferta oferta = new Oferta(loja, titulo, new BigDecimal(preco), null, 0, "http://x", null);
        return new FonteLoja() {
            @Override
            public Loja loja() {
                return loja;
            }

            @Override
            public List<Oferta> buscar(String termo) {
                return List.of(oferta);
            }

            @Override
            public List<Oferta> buscarDestaques() {
                return List.of(oferta);
            }
        };
    }

    @Test
    void titulosQuaseIguaisSaoFundidosComLimiar() {
        // "the witcher 3 wild hunt" x "witcher 3 wild hunt": diferenca pequena, mesmos numeros
        FonteLoja steam = fonteFake(Loja.STEAM, "The Witcher 3: Wild Hunt", "50.00");
        FonteLoja gp = fonteFake(Loja.GAMESPLANET, "Witcher 3 Wild Hunt", "60.00");

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gp), 0.80).comparar("witcher");

        assertEquals(1, jogos.size(), "os dois titulos parecidos viram o mesmo jogo");
        assertTrue(jogos.get(0).comparavel());
        assertEquals(2, jogos.get(0).quantidadeLojas());
    }

    @Test
    void sequenciasComNumerosDiferentesNuncaSaoFundidas() {
        // mesmo com limiar baixo, "portal" e "portal 2" sao jogos distintos
        FonteLoja steam = fonteFake(Loja.STEAM, "Portal", "10.00");
        FonteLoja gp = fonteFake(Loja.GAMESPLANET, "Portal 2", "20.00");

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gp), 0.50).comparar("portal");

        assertEquals(2, jogos.size(), "numeros diferentes no titulo impedem a fusao (guarda de sequencia)");
    }

    @Test
    void clusterDeTresVariantesColapsaNumJogoSo() {
        // exercita o match contra TODAS as chaves ja absorvidas pelo grupo (nao so a 1a)
        FonteLoja steam = fonteFake(Loja.STEAM, "Stardew Valley", "20.00");
        FonteLoja gg = fonteFake(Loja.GAMERSGATE, "Stardew Valey", "18.00");
        FonteLoja gp = fonteFake(Loja.GAMESPLANET, "Stardew Valleyy", "22.00");

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gg, gp), 0.84).comparar("stardew");

        assertEquals(1, jogos.size(), "as tres variantes parecidas viram um jogo so");
        assertEquals(3, jogos.get(0).quantidadeLojas());
    }

    @Test
    void limiarZeroMantemApenasCruzamentoExato() {
        FonteLoja steam = fonteFake(Loja.STEAM, "The Witcher 3: Wild Hunt", "50.00");
        FonteLoja gp = fonteFake(Loja.GAMESPLANET, "Witcher 3 Wild Hunt", "60.00");

        List<Jogo> jogos = new ComparadorPrecos(List.of(steam, gp), 0.0).comparar("witcher");

        assertEquals(2, jogos.size(), "sem match aproximado, titulos diferentes nao se juntam");
    }
}
