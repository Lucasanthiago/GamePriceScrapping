package com.gamesprice;

import com.gamesprice.cambio.ConversorMoeda;
import com.gamesprice.cambio.TaxaFixaConversor;
import com.gamesprice.cli.TabelaPrecos;
import com.gamesprice.comparador.ComparadorPrecos;
import com.gamesprice.config.Config;
import com.gamesprice.fonte.FonteGamersGate;
import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.fonte.FonteSteam;
import com.gamesprice.http.Buscador;
import com.gamesprice.http.CacheBuscador;
import com.gamesprice.http.JsoupBuscador;
import com.gamesprice.model.Jogo;
import com.gamesprice.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Ponto de entrada (CLI) e composition root: monta as dependencias e roda a comparacao.
 *
 * <p>Uso:
 * <pre>
 *   java -jar comparador-precos-jogos.jar               # modo interativo
 *   java -jar comparador-precos-jogos.jar elden ring    # busca direta
 *   java -jar comparador-precos-jogos.jar --verbose witcher
 * </pre>
 */
public final class App {

    public static void main(String[] args) {
        List<String> termoArgs = new ArrayList<>();
        boolean semCache = false;

        for (String arg : args) {
            switch (arg) {
                case "--verbose", "-v" -> Log.setVerboso(true);
                case "--no-cache" -> semCache = true;
                default -> termoArgs.add(arg);
            }
        }

        ComparadorPrecos comparador = montarComparador(semCache);

        if (!termoArgs.isEmpty()) {
            buscarEImprimir(comparador, String.join(" ", termoArgs));
        } else {
            loopInterativo(comparador);
        }
    }

    /** Monta o grafo de objetos (injecao de dependencia manual). */
    private static ComparadorPrecos montarComparador(boolean semCache) {
        Buscador buscador = new JsoupBuscador();
        if (Config.CACHE_HABILITADO && !semCache) {
            buscador = CacheBuscador.padrao(buscador);
        }
        ConversorMoeda conversor = new TaxaFixaConversor();

        List<FonteLoja> fontes = List.of(
                new FonteSteam(buscador),
                new FonteGamersGate(buscador, conversor));

        return new ComparadorPrecos(fontes);
    }

    private static void loopInterativo(ComparadorPrecos comparador) {
        System.out.println("Comparador de Precos de Jogos");
        System.out.println("Digite o nome de um jogo (ou ENTER vazio para sair).");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nJogo> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String termo = scanner.nextLine().trim();
                if (termo.isEmpty()) {
                    break;
                }
                buscarEImprimir(comparador, termo);
            }
        }
        System.out.println("Ate mais!");
    }

    private static void buscarEImprimir(ComparadorPrecos comparador, String termo) {
        System.out.println("Buscando \"" + termo + "\" nas lojas...");
        List<Jogo> jogos = comparador.comparar(termo);
        System.out.println(TabelaPrecos.render(termo, jogos));
    }
}
