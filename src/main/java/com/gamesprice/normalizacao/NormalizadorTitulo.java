package com.gamesprice.normalizacao;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normaliza titulos de jogos para servir de chave de cruzamento entre lojas.
 *
 * <p>O mesmo jogo aparece escrito de formas diferentes ("Elden Ring", "ELDEN RING",
 * "Elden Ring(R)"). A normalizacao reduz todas essas variantes a uma forma canonica:
 * minusculas, sem acentos, sem simbolos, sem sufixos de edicao e com espacos colapsados.
 *
 * <p>Classe utilitaria sem estado (apenas metodos estaticos).
 */
public final class NormalizadorTitulo {

    /** Sufixos de edicao removidos do final do titulo (nao alteram qual jogo e). */
    private static final List<String> SUFIXOS_EDICAO = List.of(
            "game of the year edition",
            "game of the year",
            "goty edition",
            "goty",
            "deluxe edition",
            "premium deluxe edition",
            "premium edition",
            "ultimate edition",
            "definitive edition",
            "complete edition",
            "standard edition",
            "gold edition",
            "anniversary edition",
            "enhanced edition",
            "remastered",
            "deluxe",
            "edition");

    private static final Pattern ACENTOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    /** Apostrofos juntam as letras ("assassin's" -> "assassins"), nao separam. */
    private static final Pattern APOSTROFOS = Pattern.compile("['’`]");
    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private NormalizadorTitulo() {
    }

    /**
     * Devolve a forma canonica do titulo. Nunca retorna null; entradas nulas viram "".
     */
    public static String normalizar(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "";
        }
        String t = titulo.toLowerCase();

        // remove acentos: decompoe e tira as marcas diacriticas
        t = Normalizer.normalize(t, Normalizer.Form.NFD);
        t = ACENTOS.matcher(t).replaceAll("");

        // apostrofos somem (juntam as letras); demais simbolos viram separador
        t = APOSTROFOS.matcher(t).replaceAll("");
        t = NAO_ALFANUMERICO.matcher(t).replaceAll(" ");
        t = ESPACOS.matcher(t).replaceAll(" ").trim();

        // remove sufixos de edicao do final, repetidamente (ex.: "... ultimate edition")
        t = removerSufixosEdicao(t);

        return ESPACOS.matcher(t).replaceAll(" ").trim();
    }

    private static String removerSufixosEdicao(String texto) {
        boolean removeu = true;
        String t = texto;
        while (removeu) {
            removeu = false;
            for (String sufixo : SUFIXOS_EDICAO) {
                if (t.endsWith(" " + sufixo) || t.equals(sufixo)) {
                    t = t.substring(0, t.length() - sufixo.length()).trim();
                    removeu = true;
                    break;
                }
            }
        }
        return t;
    }
}
