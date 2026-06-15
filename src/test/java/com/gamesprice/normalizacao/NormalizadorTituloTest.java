package com.gamesprice.normalizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NormalizadorTituloTest {

    @Test
    void variantesDoMesmoJogoColapsamParaMesmaChave() {
        String esperado = NormalizadorTitulo.normalizar("Elden Ring");
        assertEquals(esperado, NormalizadorTitulo.normalizar("ELDEN RING"));
        assertEquals(esperado, NormalizadorTitulo.normalizar("Elden Ring®"));
        assertEquals(esperado, NormalizadorTitulo.normalizar("  elden   ring  "));
        assertEquals(esperado, NormalizadorTitulo.normalizar("Elden Ring - Deluxe Edition"));
        assertEquals(esperado, NormalizadorTitulo.normalizar("Elden Ring: Game of the Year"));
    }

    @Test
    void removeAcentos() {
        assertEquals("assassins creed odyssey",
                NormalizadorTitulo.normalizar("Assassin's Creed Odyssey"));
        assertEquals("uncharted", NormalizadorTitulo.normalizar("Uncharted™"));
    }

    @Test
    void naoRemoveNumerosNemPalavrasDoTitulo() {
        assertEquals("portal 2", NormalizadorTitulo.normalizar("Portal 2"));
        assertEquals("the witcher 3 wild hunt",
                NormalizadorTitulo.normalizar("The Witcher 3: Wild Hunt"));
    }

    @Test
    void entradaNulaOuVaziaViraStringVazia() {
        assertEquals("", NormalizadorTitulo.normalizar(null));
        assertEquals("", NormalizadorTitulo.normalizar("   "));
    }
}
