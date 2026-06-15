package com.gamesprice.comparador;

import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.normalizacao.NormalizadorTitulo;
import com.gamesprice.normalizacao.SimilaridadeTitulo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a comparacao: roda todas as {@link FonteLoja} para um termo, cruza as
 * ofertas pelo titulo normalizado e devolve os jogos com suas ofertas agrupadas.
 *
 * <p>O cruzamento e feito aqui, no codigo: nao vem pronto de nenhuma loja. A chave de
 * agrupamento e o titulo normalizado por {@link NormalizadorTitulo}. Opcionalmente, um
 * segundo passo de <b>match aproximado</b> ({@link SimilaridadeTitulo}) funde titulos que
 * ficaram quase iguais ("The Witcher 3" x "Witcher 3"), respeitando os numeros do titulo
 * para nao confundir sequencias (Portal x Portal 2).
 */
public final class ComparadorPrecos {

    private final List<FonteLoja> fontes;
    private final double limiarSimilaridade;

    /** Comparador com cruzamento apenas exato (sem match aproximado). */
    public ComparadorPrecos(List<FonteLoja> fontes) {
        this(fontes, 0.0);
    }

    /**
     * @param fontes             lojas a consultar
     * @param limiarSimilaridade limiar 0..1 para fundir titulos quase iguais; 0 desliga
     */
    public ComparadorPrecos(List<FonteLoja> fontes, double limiarSimilaridade) {
        this.fontes = List.copyOf(fontes);
        this.limiarSimilaridade = limiarSimilaridade;
    }

    /**
     * Busca o termo em todas as fontes e cruza os resultados.
     *
     * @return jogos encontrados, ordenados: primeiro os comparaveis (em mais de uma
     *         loja), depois pelo menor preco.
     */
    public List<Jogo> comparar(String termo) {
        List<Oferta> todas = coletarOfertas(termo);
        Map<String, Jogo> porTitulo = agruparPorTituloNormalizado(todas);
        Collection<Jogo> jogos = limiarSimilaridade > 0
                ? fundirAproximados(porTitulo)
                : porTitulo.values();
        return ordenar(jogos);
    }

    private List<Oferta> coletarOfertas(String termo) {
        List<Oferta> todas = new ArrayList<>();
        for (FonteLoja fonte : fontes) {
            todas.addAll(fonte.buscar(termo));
        }
        return todas;
    }

    private Map<String, Jogo> agruparPorTituloNormalizado(List<Oferta> ofertas) {
        // LinkedHashMap preserva a ordem de descoberta antes da ordenacao final.
        Map<String, Jogo> mapa = new LinkedHashMap<>();
        for (Oferta oferta : ofertas) {
            String chave = NormalizadorTitulo.normalizar(oferta.tituloOriginal());
            if (chave.isEmpty()) {
                continue;
            }
            Jogo jogo = mapa.computeIfAbsent(chave, k -> new Jogo(k, oferta.tituloOriginal()));
            jogo.adicionarOferta(oferta);
        }
        return mapa;
    }

    /**
     * Funde grupos cujas chaves normalizadas sao quase iguais. Guarda de seguranca: so
     * funde quando os numeros do titulo coincidem, evitando juntar sequencias diferentes
     * (ex.: "fifa 22" x "fifa 23", "portal" x "portal 2").
     */
    private Collection<Jogo> fundirAproximados(Map<String, Jogo> grupos) {
        List<Jogo> canonicos = new ArrayList<>();
        // todas as chaves ja absorvidas por cada grupo: comparamos contra TODAS elas (nao so
        // a primeira), tornando a fusao menos sensivel a ordem de descoberta dos titulos.
        List<List<String>> membrosPorGrupo = new ArrayList<>();

        for (Map.Entry<String, Jogo> entrada : grupos.entrySet()) {
            String chave = entrada.getKey();
            Jogo jogo = entrada.getValue();

            int alvo = encontrarGrupoSimilar(chave, membrosPorGrupo);
            if (alvo < 0) {
                canonicos.add(jogo);
                List<String> membros = new ArrayList<>();
                membros.add(chave);
                membrosPorGrupo.add(membros);
            } else {
                Jogo destino = canonicos.get(alvo);
                for (Oferta oferta : jogo.ofertasOrdenadas()) {
                    destino.adicionarOferta(oferta);
                }
                membrosPorGrupo.get(alvo).add(chave);
            }
        }
        return canonicos;
    }

    /** Indice do primeiro grupo cujo algum membro casa com a chave; -1 se nenhum. */
    private int encontrarGrupoSimilar(String chave, List<List<String>> membrosPorGrupo) {
        for (int i = 0; i < membrosPorGrupo.size(); i++) {
            for (String membro : membrosPorGrupo.get(i)) {
                if (numerosIguais(chave, membro)
                        && SimilaridadeTitulo.similaridade(chave, membro) >= limiarSimilaridade) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Os tokens numericos das duas chaves (em ordem) sao identicos? */
    private static boolean numerosIguais(String a, String b) {
        return numeros(a).equals(numeros(b));
    }

    private static List<String> numeros(String chave) {
        List<String> nums = new ArrayList<>();
        for (String token : chave.split(" ")) {
            if (!token.isEmpty() && token.chars().allMatch(Character::isDigit)) {
                nums.add(token);
            }
        }
        nums.sort(Comparator.naturalOrder());
        return nums;
    }

    private List<Jogo> ordenar(Collection<Jogo> jogos) {
        List<Jogo> lista = new ArrayList<>(jogos);
        lista.sort(
                Comparator.comparing(Jogo::comparavel).reversed()
                        .thenComparing(j -> j.ofertaMaisBarata().precoBRL()));
        return lista;
    }
}
