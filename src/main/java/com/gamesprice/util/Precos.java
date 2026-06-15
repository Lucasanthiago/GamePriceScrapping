package com.gamesprice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Conversao e parsing de valores monetarios.
 *
 * <p>As lojas expoem precos em formatos diferentes: a Steam traz centavos inteiros
 * ({@code data-price-final="27450"}) e textos no padrao brasileiro ("R$1.234,56");
 * a GamersGate usa ponto como separador decimal ("R$ 51.02"). Centralizar aqui evita
 * espalhar logica fragil de parsing pelas fontes.
 */
public final class Precos {

    private static final DecimalFormat FORMATO_BRL = criarFormatoBrl();

    private Precos() {
    }

    private static DecimalFormat criarFormatoBrl() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", simbolos);
    }

    /** Formata um valor BRL para exibicao: 274.5 -> "R$ 274,50". */
    public static String formatarBRL(BigDecimal valor) {
        return "R$ " + FORMATO_BRL.format(valor);
    }

    /** Converte centavos inteiros (ex.: 27450) em reais (274.50). */
    public static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos).movePointLeft(2);
    }

    /**
     * Parse de preco no padrao brasileiro: ponto como milhar e virgula como decimal.
     * Ex.: "R$ 1.234,56" -> 1234.56. Retorna null se nao houver numero.
     */
    public static BigDecimal parseBrasileiro(String texto) {
        if (texto == null) {
            return null;
        }
        String somenteNumero = texto.replaceAll("[^0-9.,]", "");
        if (somenteNumero.isEmpty()) {
            return null;
        }
        somenteNumero = somenteNumero.replace(".", "").replace(",", ".");
        return parseSeguro(somenteNumero);
    }

    /**
     * Parse de preco com ponto decimal: "R$ 51.02" -> 51.02. Retorna null se vazio.
     */
    public static BigDecimal parsePonto(String texto) {
        if (texto == null) {
            return null;
        }
        String somenteNumero = texto.replaceAll("[^0-9.]", "");
        return parseSeguro(somenteNumero);
    }

    private static BigDecimal parseSeguro(String valor) {
        if (valor.isEmpty() || valor.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
