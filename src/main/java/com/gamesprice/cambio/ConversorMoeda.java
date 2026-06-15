package com.gamesprice.cambio;

import java.math.BigDecimal;

/**
 * Converte um valor de uma moeda para BRL.
 *
 * <p>Interface para permitir trocar a estrategia sem mexer nas fontes: o MVP usa
 * {@link TaxaFixaConversor}; um extra futuro pode raspar a cotacao ao vivo.
 */
public interface ConversorMoeda {

    /**
     * @param valor  quantia na moeda de origem
     * @param moeda  codigo ISO da moeda de origem (ex.: "USD", "BRL")
     * @return valor equivalente em BRL
     */
    BigDecimal paraBRL(BigDecimal valor, String moeda);
}
