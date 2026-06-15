package com.gamesprice.normalizacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decide se o titulo de um jogo e relevante para o termo buscado.
 *
 * <p>As buscas das lojas sao abrangentes: pesquisar "elden ring" devolve, alem do jogo,
 * uma penca de titulos tangenciais que a propria loja resolveu sugerir. O cruzamento por si
 * so nao filtra isso. Aqui aplicamos uma regra simples de precisao: o titulo so e relevante
 * quando contem <b>todos os tokens significativos</b> do termo (como palavras inteiras).
 *
 * <p>Tudo trabalha sobre a forma canonica do {@link NormalizadorTitulo}, entao acentos,
 * maiusculas, pontuacao e sufixos de edicao ja foram neutralizados antes da comparacao.
 */
public final class RelevanciaTitulo {

    /**
     * Palavras vazias: nao sao exigidas no match, porque uma loja pode escrever o titulo
     * sem elas ("The Witcher" x "Witcher 3"). Inclui artigos/preposicoes comuns em PT e EN.
     */
    private static final Set<String> VAZIAS = Set.of(
            "the", "of", "a", "an", "and", "or", "to", "in", "on", "for",
            "de", "do", "da", "e", "o", "os", "as");

    /**
     * Tokens que denunciam conteudo que nao e o jogo em si (trilha sonora, artbook...).
     * Num comparador de <i>jogos</i>, esses itens sao ruido — somem dos resultados.
     */
    private static final Set<String> CONTEUDO_EXTRA = Set.of(
            "soundtrack", "soundtracks", "ost", "artbook", "artbooks", "wallpaper", "wallpapers");

    private RelevanciaTitulo() {
    }

    /**
     * Tokens significativos do termo buscado (normalizado, sem palavras vazias).
     *
     * <p>Se o termo so tiver palavras vazias, devolve todos os tokens — assim nao desligamos
     * o filtro a toa. Termo vazio devolve lista vazia (o chamador entende como "nao filtrar").
     */
    public static List<String> termosSignificativos(String termo) {
        String norm = NormalizadorTitulo.normalizar(termo);
        if (norm.isEmpty()) {
            return List.of();
        }
        List<String> todos = Arrays.asList(norm.split(" "));
        List<String> significativos = new ArrayList<>();
        for (String token : todos) {
            if (!token.isEmpty() && !VAZIAS.contains(token)) {
                significativos.add(token);
            }
        }
        return significativos.isEmpty() ? todos : significativos;
    }

    /**
     * @param tituloNormalizado titulo do jogo ja normalizado
     * @param termosSignificativos saida de {@link #termosSignificativos(String)}
     * @return true se o titulo contem todos os termos (ou se a lista estiver vazia)
     */
    public static boolean relevante(String tituloNormalizado, List<String> termosSignificativos) {
        if (termosSignificativos.isEmpty()) {
            return true;
        }
        Set<String> tokens = new HashSet<>(Arrays.asList(tituloNormalizado.split(" ")));
        for (String termo : termosSignificativos) {
            if (!tokens.contains(termo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * O titulo parece ser conteudo extra (trilha sonora, artbook, wallpaper) e nao o jogo?
     *
     * @param tituloNormalizado titulo do jogo ja normalizado
     */
    public static boolean pareceConteudoExtra(String tituloNormalizado) {
        for (String token : tituloNormalizado.split(" ")) {
            if (CONTEUDO_EXTRA.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
