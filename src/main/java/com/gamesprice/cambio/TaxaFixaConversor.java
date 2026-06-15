package com.gamesprice.cambio;

import com.gamesprice.config.Config;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversor que usa taxas fixas configuradas (estrategia do MVP).
 *
 * <p>Valores ja em BRL passam direto. USD usa {@link Config#USD_BRL}. Moedas
 * desconhecidas sao tratadas como BRL (sem conversao) e seguem como estao, evitando
 * quebrar a comparacao por causa de uma loja nova nao mapeada.
 */
public final class TaxaFixaConversor implements ConversorMoeda {

    private final BigDecimal usdBrl;

    public TaxaFixaConversor() {
        this(Config.USD_BRL);
    }

    public TaxaFixaConversor(BigDecimal usdBrl) {
        this.usdBrl = usdBrl;
    }

    @Override
    public BigDecimal paraBRL(BigDecimal valor, String moeda) {
        if (valor == null) {
            return null;
        }
        String codigo = moeda == null ? "BRL" : moeda.trim().toUpperCase();
        BigDecimal convertido = switch (codigo) {
            case "BRL" -> valor;
            case "USD" -> valor.multiply(usdBrl);
            default -> valor;
        };
        return convertido.setScale(2, RoundingMode.HALF_UP);
    }
}
