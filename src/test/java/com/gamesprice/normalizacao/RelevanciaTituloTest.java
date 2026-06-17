package com.gamesprice.normalizacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RelevanciaTituloTest {

    /** Atalho: o titulo e relevante para o termo? (normaliza dos dois lados). */
    private static boolean relevante(String termo, String titulo) {
        return RelevanciaTitulo.relevante(
                NormalizadorTitulo.normalizar(titulo),
                RelevanciaTitulo.termosSignificativos(termo));
    }

    @Test
    void mantemTitulosComTodosOsTermos() {
        assertTrue(relevante("elden ring", "ELDEN RING"));
        assertTrue(relevante("elden ring", "Elden Ring: Shadow of the Erdtree"));
        assertTrue(relevante("elden ring", "ELDEN RING NIGHTREIGN"));
    }

    @Test
    void descartaTitulosTangenciais() {
        assertFalse(relevante("elden ring", "Goose Evolution"));
        assertFalse(relevante("elden ring", "Ring of Pain")); // tem "ring", falta "elden"
        assertFalse(relevante("elden ring", "Tiny Tina's Wonderlands"));
    }

    @Test
    void palavrasVaziasNaoSaoExigidas() {
        // "the" e palavra vazia: nao precisa estar no titulo.
        assertTrue(relevante("the witcher", "Witcher 3: Wild Hunt"));
        assertTrue(relevante("the witcher", "The Witcher 3"));
    }

    @Test
    void numeroDistingueSequencias() {
        assertTrue(relevante("portal", "Portal 2"));     // "portal" casa com qualquer Portal
        assertTrue(relevante("portal 2", "Portal 2"));
        assertFalse(relevante("portal 2", "Portal"));    // falta o "2"
        assertFalse(relevante("fifa 23", "FIFA 22"));
    }

    @Test
    void detectaConteudoExtraNaoJogo() {
        assertTrue(RelevanciaTitulo.pareceConteudoExtra(
                NormalizadorTitulo.normalizar("The Witcher 3: Wild Hunt Soundtrack")));
        assertTrue(RelevanciaTitulo.pareceConteudoExtra(
                NormalizadorTitulo.normalizar("Elden Ring Digital Artbook")));
        assertFalse(RelevanciaTitulo.pareceConteudoExtra(
                NormalizadorTitulo.normalizar("The Witcher 3: Wild Hunt")));
        assertFalse(RelevanciaTitulo.pareceConteudoExtra(
                NormalizadorTitulo.normalizar("Ghostrunner"))); // "ost" e substring, nao token
    }

    @Test
    void termoVazioNaoFiltra() {
        assertTrue(RelevanciaTitulo.termosSignificativos("").isEmpty());
        assertTrue(relevante("", "qualquer coisa"));
    }

    @Test
    void termoSoComPalavrasVaziasUsaTodosOsTokens() {
        // Sem token significativo, cai para exigir todos os tokens (inclui "the").
        assertFalse(RelevanciaTitulo.termosSignificativos("the of").isEmpty());
    }
}
