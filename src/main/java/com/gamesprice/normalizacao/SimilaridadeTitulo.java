package com.gamesprice.normalizacao;

/**
 * Similaridade entre titulos por distancia de Levenshtein.
 *
 * <p>Usada como reforco opcional ao cruzamento exato: quando dois titulos
 * normalizados nao batem exatamente mas sao quase iguais (diferenca de pontuacao,
 * abreviacao etc.), a similaridade ajuda a aceitar o match aproximado.
 */
public final class SimilaridadeTitulo {

    private SimilaridadeTitulo() {
    }

    /** Distancia de edicao (numero minimo de insercoes/remocoes/substituicoes). */
    public static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        int[] anterior = new int[m + 1];
        int[] atual = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            atual[0] = i;
            for (int j = 1; j <= m; j++) {
                int custoSub = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                atual[j] = Math.min(
                        Math.min(atual[j - 1] + 1, anterior[j] + 1),
                        anterior[j - 1] + custoSub);
            }
            int[] tmp = anterior;
            anterior = atual;
            atual = tmp;
        }
        return anterior[m];
    }

    /** Similaridade normalizada em [0,1]; 1 = identicos. */
    public static double similaridade(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        int maior = Math.max(a.length(), b.length());
        return 1.0 - ((double) levenshtein(a, b) / maior);
    }
}
