package com.gamesprice;

import com.gamesprice.cambio.ConversorMoeda;
import com.gamesprice.cambio.CotacaoAoVivoConversor;
import com.gamesprice.cambio.TaxaFixaConversor;
import com.gamesprice.cli.ExportadorComparacao;
import com.gamesprice.cli.TabelaPrecos;
import com.gamesprice.comparador.ComparadorPrecos;
import com.gamesprice.comparador.MaioresDescontos;
import com.gamesprice.config.Config;
import com.gamesprice.fonte.FonteGamersGate;
import com.gamesprice.fonte.FonteGamesPlanet;
import com.gamesprice.fonte.FonteLoja;
import com.gamesprice.fonte.FonteSteam;
import com.gamesprice.http.Buscador;
import com.gamesprice.http.CacheBuscador;
import com.gamesprice.http.JsoupBuscador;
import com.gamesprice.model.Jogo;
import com.gamesprice.util.Log;
import com.gamesprice.web.ServidorWeb;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Ponto de entrada (CLI/Web) e composition root: monta as dependencias e roda a comparacao.
 *
 * <p>Uso:
 * <pre>
 *   java -jar comparador-precos-jogos.jar               # modo interativo (CLI)
 *   java -jar comparador-precos-jogos.jar elden ring    # busca direta (CLI)
 *   java -jar comparador-precos-jogos.jar --web         # abre a UI web em http://localhost:8080
 *   java -jar comparador-precos-jogos.jar --html witcher  # exporta comparacao para arquivo
 * </pre>
 *
 * <p>Flags: {@code --verbose/-v} (logs), {@code --no-cache} (ignora cache em disco),
 * {@code --web} (servidor web), {@code --port N} (porta web), {@code --exato}
 * (desliga match aproximado), {@code --cambio-fixo} (usa taxa fixa em vez da cotacao ao
 * vivo), {@code --csv}/{@code --html} (exporta o resultado da busca para arquivo).
 */
public final class App {

    public static void main(String[] args) throws IOException {
        List<String> termoArgs = new ArrayList<>();
        boolean semCache = false;
        boolean web = false;
        boolean exato = false;
        boolean cambioFixo = false;
        boolean exportarCsv = false;
        boolean exportarHtml = false;
        int porta = Config.PORTA_WEB;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--verbose", "-v" -> Log.setVerboso(true);
                case "--no-cache" -> semCache = true;
                case "--web" -> web = true;
                case "--exato" -> exato = true;
                case "--cambio-fixo" -> cambioFixo = true;
                case "--csv" -> exportarCsv = true;
                case "--html" -> exportarHtml = true;
                case "--port", "--porta" -> {
                    if (i + 1 < args.length) {
                        porta = parsePorta(args[++i], porta);
                    }
                }
                default -> termoArgs.add(arg);
            }
        }

        Montagem m = montar(semCache, exato, cambioFixo, porta);

        if (web) {
            if (!termoArgs.isEmpty() || exportarCsv || exportarHtml) {
                Log.aviso("--web ignora termo de busca e --csv/--html; use o modo CLI para exportar.");
            }
            iniciarWeb(m);
            return;
        }

        if (!termoArgs.isEmpty()) {
            String termo = String.join(" ", termoArgs);
            List<Jogo> jogos = buscarEImprimir(m.comparador(), termo);
            if (exportarCsv) {
                exportar(termo, jogos, "csv");
            }
            if (exportarHtml) {
                exportar(termo, jogos, "html");
            }
        } else {
            loopInterativo(m.comparador());
        }
    }

    /** Resultado da injecao de dependencia: o comparador e como obter a cotacao para a UI. */
    private record Montagem(
            ComparadorPrecos comparador,
            MaioresDescontos maioresDescontos,
            Supplier<Optional<BigDecimal>> cotacaoUsdBrl,
            int porta) {
    }

    /** Monta o grafo de objetos (injecao de dependencia manual). */
    private static Montagem montar(boolean semCache, boolean exato, boolean cambioFixo, int porta) {
        Buscador buscador = new JsoupBuscador();
        if (Config.CACHE_HABILITADO && !semCache) {
            buscador = CacheBuscador.padrao(buscador);
        }

        ConversorMoeda fixo = new TaxaFixaConversor();
        Supplier<Optional<BigDecimal>> cotacao = Optional::empty;
        ConversorMoeda conversor = fixo;
        if (!cambioFixo) {
            CotacaoAoVivoConversor aoVivo = new CotacaoAoVivoConversor(fixo);
            conversor = aoVivo;
            cotacao = aoVivo::cotacaoUsdBrl;
        }

        List<FonteLoja> fontes = List.of(
                new FonteSteam(buscador),
                new FonteGamersGate(buscador, conversor),
                new FonteGamesPlanet(buscador, conversor));

        double limiar = exato ? 0.0 : Config.LIMIAR_SIMILARIDADE;
        ComparadorPrecos comparador = new ComparadorPrecos(fontes, limiar);
        MaioresDescontos maioresDescontos = new MaioresDescontos(fontes, Config.QTD_MAIORES_DESCONTOS);
        return new Montagem(comparador, maioresDescontos, cotacao, porta);
    }

    private static void iniciarWeb(Montagem m) throws IOException {
        // Roda uma unica vez, em paralelo a subida do servidor: o resultado fica
        // guardado neste future e nunca e recalculado (sem agendamento/repeticao).
        CompletableFuture<List<Jogo>> destaques = CompletableFuture.supplyAsync(() -> {
            Log.info("Buscando maiores descontos do momento...");
            return m.maioresDescontos().destaques();
        });

        ServidorWeb servidor = new ServidorWeb(m.comparador(), destaques, m.cotacaoUsdBrl(), m.porta());
        servidor.iniciar();
        System.out.println("Comparador de Precos de Jogos - UI web em " + servidor.url());
        System.out.println("(Ctrl+C para encerrar)");

        CountDownLatch travar = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.parar();
            travar.countDown();
        }));
        try {
            travar.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private static List<Jogo> buscarEImprimir(ComparadorPrecos comparador, String termo) {
        System.out.println("Buscando \"" + termo + "\" nas lojas...");
        List<Jogo> jogos = comparador.comparar(termo);
        System.out.println(TabelaPrecos.render(termo, jogos));
        return jogos;
    }

    private static void exportar(String termo, List<Jogo> jogos, String formato) {
        String conteudo = formato.equals("html")
                ? ExportadorComparacao.paraHtml(termo, jogos)
                : ExportadorComparacao.paraCsv(termo, jogos);
        String slug = termo.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            slug = "jogos";
        }
        Path arquivo = Path.of("comparacao-" + slug + "." + formato);
        try {
            Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
            System.out.println("Exportado: " + arquivo.toAbsolutePath());
        } catch (IOException e) {
            Log.aviso("nao foi possivel exportar " + formato + ": " + e.getMessage());
        }
    }

    private static int parsePorta(String valor, int padrao) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}
