package com.gamesprice.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Um jogo, identificado pelo titulo normalizado, agrupando as ofertas de todas as
 * lojas que o oferecem. E o resultado do "cruzamento" de dados entre fontes.
 */
public final class Jogo {

    private final String tituloNormalizado;
    private final String tituloExibicao;
    private final List<Oferta> ofertas = new ArrayList<>();

    public Jogo(String tituloNormalizado, String tituloExibicao) {
        this.tituloNormalizado = Objects.requireNonNull(tituloNormalizado, "tituloNormalizado");
        this.tituloExibicao = Objects.requireNonNull(tituloExibicao, "tituloExibicao");
    }

    public void adicionarOferta(Oferta oferta) {
        ofertas.add(Objects.requireNonNull(oferta, "oferta"));
    }

    public String tituloNormalizado() {
        return tituloNormalizado;
    }

    public String tituloExibicao() {
        return tituloExibicao;
    }

    /** Ofertas ordenadas da mais barata para a mais cara. */
    public List<Oferta> ofertasOrdenadas() {
        List<Oferta> copia = new ArrayList<>(ofertas);
        copia.sort(Comparator.comparing(Oferta::precoBRL));
        return copia;
    }

    /** A oferta mais barata, ou vazio se nao houver ofertas. */
    public Oferta ofertaMaisBarata() {
        return ofertas.stream().min(Comparator.comparing(Oferta::precoBRL)).orElse(null);
    }

    /** Quantas lojas distintas oferecem este jogo (>1 significa que da pra comparar). */
    public long quantidadeLojas() {
        return ofertas.stream().map(Oferta::loja).distinct().count();
    }

    public boolean comparavel() {
        return quantidadeLojas() > 1;
    }

    /** Maior desconto entre as ofertas deste jogo (0 se nenhuma tiver desconto). */
    public int maiorDesconto() {
        return ofertas.stream().mapToInt(Oferta::descontoPerc).max().orElse(0);
    }
}
