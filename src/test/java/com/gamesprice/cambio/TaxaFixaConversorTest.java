package com.gamesprice.cambio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxaFixaConversorTest {

    private final ConversorMoeda conversor = new TaxaFixaConversor(new BigDecimal("5.40"));

    @Test
    void brlPassaSemConversao() {
        assertEquals(new BigDecimal("45.91"), conversor.paraBRL(new BigDecimal("45.91"), "BRL"));
    }

    @Test
    void usdConverteComTaxaFixa() {
        assertEquals(new BigDecimal("54.00"), conversor.paraBRL(new BigDecimal("10.00"), "USD"));
    }

    @Test
    void moedaDesconhecidaNaoQuebraEPassaDireto() {
        assertEquals(new BigDecimal("10.00"), conversor.paraBRL(new BigDecimal("10"), "XYZ"));
    }
}
