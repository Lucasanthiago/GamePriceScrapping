package com.gamesprice.model;

/**
 * Lojas suportadas pelo comparador.
 *
 * <p>O {@code rotulo} e usado na apresentacao (tabela/CLI). Para adicionar uma
 * loja nova, inclua um valor aqui e crie a {@code FonteLoja} correspondente.
 */
public enum Loja {
    STEAM("Steam"),
    GAMERSGATE("GamersGate"),
    GAMESPLANET("GamesPlanet");

    private final String rotulo;

    Loja(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
