package com.gamesprice.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExportadorComparacaoTest {

    private static Jogo jogo(String titulo, Oferta... ofertas) {
        Jogo j = new Jogo(titulo.toLowerCase(), titulo);
        for (Oferta o : ofertas) {
            j.adicionarOferta(o);
        }
        return j;
    }

    @Test
    void csvTemCabecalhoEUmaLinhaPorOferta() {
        Jogo j = jogo("Jogo X",
                new Oferta(Loja.STEAM, "Jogo X", new BigDecimal("10.00"), null, 0, "http://a", 90),
                new Oferta(Loja.GAMERSGATE, "Jogo X", new BigDecimal("12.00"), null, 0, "http://b", null));

        String csv = ExportadorComparacao.paraCsv("jogo x", List.of(j));
        String[] linhas = csv.strip().split("\n");

        assertTrue(linhas[0].startsWith("termo,jogo,loja,preco_brl"), "cabecalho esperado");
        assertEquals(3, linhas.length, "1 cabecalho + 2 ofertas");
        assertTrue(csv.contains("Steam") && csv.contains("GamersGate"));
    }

    @Test
    void csvEscapaCamposComVirgula() {
        Jogo j = jogo("Hello, World",
                new Oferta(Loja.STEAM, "Hello, World", new BigDecimal("5.00"), null, 0, "http://a", null));

        String csv = ExportadorComparacao.paraCsv("hello", List.of(j));

        assertTrue(csv.contains("\"Hello, World\""), "titulo com virgula deve vir entre aspas");
    }

    @Test
    void htmlEhDocumentoCompletoEEscapaTitulo() {
        Jogo j = jogo("Tag <b>",
                new Oferta(Loja.STEAM, "Tag <b>", new BigDecimal("5.00"), null, 0, "http://a", null));

        String html = ExportadorComparacao.paraHtml("tag", List.of(j));

        assertTrue(html.startsWith("<!DOCTYPE html>"), "documento HTML completo");
        assertTrue(html.contains("Tag &lt;b&gt;"), "titulo deve ser escapado para HTML");
        assertTrue(html.contains("</html>"));
    }

    @Test
    void htmlBloqueiaUrlComEsquemaPerigoso() {
        Jogo j = jogo("Jogo Mau",
                new Oferta(Loja.STEAM, "Jogo Mau", new BigDecimal("5.00"), null, 0, "javascript:alert(1)", null));

        String html = ExportadorComparacao.paraHtml("x", List.of(j));

        assertFalse(html.contains("javascript:alert"), "esquema perigoso nao pode ir para o href");
        assertTrue(html.contains("href=\"#\""), "URL insegura vira #");
    }

    @Test
    void csvNeutralizaInjecaoDeFormula() {
        Jogo j = jogo("=HYPERLINK(\"http://x\")",
                new Oferta(Loja.STEAM, "=HYPERLINK(\"http://x\")", new BigDecimal("5.00"), null, 0, "http://a", null));

        String csv = ExportadorComparacao.paraCsv("x", List.of(j));

        assertTrue(csv.contains("'=HYPERLINK"), "campo iniciando com = recebe apostrofo");
    }
}
