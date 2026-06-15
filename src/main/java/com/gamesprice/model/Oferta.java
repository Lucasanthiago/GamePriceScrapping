package com.gamesprice.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Uma oferta de um jogo numa loja especifica, ja com o preco convertido para BRL
 * para permitir a comparacao direta entre lojas.
 *
 * <p>Imutavel (record). O preco e sempre em Reais ({@link #precoBRL}); a conversao
 * de moeda, quando necessaria, e responsabilidade da {@code FonteLoja}.
 *
 * @param loja            loja de origem
 * @param tituloOriginal  titulo exatamente como veio da loja (para exibicao/depuracao)
 * @param precoBRL        preco final ja convertido para BRL
 * @param precoOriginalBRL preco cheio (antes do desconto) em BRL; igual ao final se nao houver desconto
 * @param descontoPerc    percentual de desconto (0 a 100)
 * @param url             link direto para o produto
 * @param notaReviewsPerc percentual de avaliacoes positivas (apenas algumas lojas expoem); pode ser null
 */
public record Oferta(
        Loja loja,
        String tituloOriginal,
        BigDecimal precoBRL,
        BigDecimal precoOriginalBRL,
        int descontoPerc,
        String url,
        Integer notaReviewsPerc) {

    public Oferta {
        Objects.requireNonNull(loja, "loja");
        Objects.requireNonNull(tituloOriginal, "tituloOriginal");
        Objects.requireNonNull(precoBRL, "precoBRL");
        if (precoOriginalBRL == null) {
            precoOriginalBRL = precoBRL;
        }
        if (descontoPerc < 0 || descontoPerc > 100) {
            throw new IllegalArgumentException("descontoPerc fora de 0..100: " + descontoPerc);
        }
    }

    public boolean temDesconto() {
        return descontoPerc > 0;
    }

    public Optional<Integer> nota() {
        return Optional.ofNullable(notaReviewsPerc);
    }
}
