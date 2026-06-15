package com.gamesprice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PrecosTest {

    @Test
    void centavosViramReais() {
        assertEquals(new BigDecimal("274.50"), Precos.deCentavos(27450));
        assertEquals(new BigDecimal("9.65"), Precos.deCentavos(965));
    }

    @Test
    void parseBrasileiroLidaComMilharEDecimal() {
        assertEquals(new BigDecimal("1234.56"), Precos.parseBrasileiro("R$ 1.234,56"));
        assertEquals(new BigDecimal("13.79"), Precos.parseBrasileiro("R$13,79"));
    }

    @Test
    void parsePontoLidaComDecimalPorPonto() {
        assertEquals(new BigDecimal("51.02"), Precos.parsePonto("R$ 51.02"));
        assertEquals(new BigDecimal("45.91"), Precos.parsePonto("45.91"));
    }

    @Test
    void textosSemNumeroRetornamNull() {
        assertNull(Precos.parseBrasileiro("Gratis"));
        assertNull(Precos.parsePonto(""));
    }

    @Test
    void formatacaoBRL() {
        assertEquals("R$ 274,50", Precos.formatarBRL(new BigDecimal("274.50")));
        assertEquals("R$ 1.234,56", Precos.formatarBRL(new BigDecimal("1234.56")));
    }
}
