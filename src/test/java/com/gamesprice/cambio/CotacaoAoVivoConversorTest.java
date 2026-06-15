package com.gamesprice.cambio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CotacaoAoVivoConversorTest {

    @Test
    void extraiTaxaDoJsonDaFonte() {
        String json = "{\"USDBRL\":{\"code\":\"USD\",\"bid\":\"5.0481\",\"ask\":\"5.0511\"}}";
        assertEquals(new BigDecimal("5.0481"), CotacaoAoVivoConversor.extrairTaxa(json));
    }

    @Test
    void corpoSemTaxaOuInvalidoRetornaNull() {
        assertNull(CotacaoAoVivoConversor.extrairTaxa("sem cotacao aqui"));
        assertNull(CotacaoAoVivoConversor.extrairTaxa(null));
        assertNull(CotacaoAoVivoConversor.extrairTaxa("{\"bid\":\"0\"}"), "taxa zero e invalida");
    }

    @Test
    void brlEMoedaDesconhecidaDelegamAoFallbackSemRede() {
        ConversorMoeda fallback = new TaxaFixaConversor(new BigDecimal("5.40"));
        CotacaoAoVivoConversor conversor = new CotacaoAoVivoConversor(fallback);

        // moedas != USD nao disparam busca de rede: vao direto ao fallback
        assertEquals(new BigDecimal("45.91"), conversor.paraBRL(new BigDecimal("45.91"), "BRL"));
        assertEquals(new BigDecimal("10.00"), conversor.paraBRL(new BigDecimal("10"), "XYZ"));
        assertNull(conversor.paraBRL(null, "USD"));
    }
}
