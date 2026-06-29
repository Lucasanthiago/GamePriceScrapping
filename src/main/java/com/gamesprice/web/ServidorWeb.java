package com.gamesprice.web;

import com.gamesprice.cli.ExportadorComparacao;
import com.gamesprice.comparador.ComparadorPrecos;
import com.gamesprice.model.Jogo;
import com.gamesprice.util.Log;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Servidor web embutido (UI) usando apenas o {@code com.sun.net.httpserver} do JDK, sem
 * dependencia nova. Serve a pagina estatica (HTML/CSS/JS) e uma pequena API JSON que roda
 * o {@link ComparadorPrecos} e devolve o resultado ja com o ranking "Vale a pena".
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /} e estaticos ({@code /styles.css}, {@code /app.js}) — a UI.</li>
 *   <li>{@code GET /api/buscar?termo=...} — JSON com os jogos comparados.</li>
 *   <li>{@code GET /api/destaques} — JSON com os maiores descontos do momento (calculado
 *       uma unica vez na subida do servidor, ver {@code App.iniciarWeb}).</li>
 *   <li>{@code GET /api/exportar?termo=...&formato=csv|html} — download da comparacao.</li>
 * </ul>
 */
public final class ServidorWeb {

    private final ComparadorPrecos comparador;
    private final Future<List<Jogo>> destaques;
    private final Supplier<Optional<BigDecimal>> cotacaoUsdBrl;
    private final int porta;
    private HttpServer servidor;

    public ServidorWeb(
            ComparadorPrecos comparador,
            Future<List<Jogo>> destaques,
            Supplier<Optional<BigDecimal>> cotacaoUsdBrl,
            int porta) {
        this.comparador = comparador;
        this.destaques = destaques;
        this.cotacaoUsdBrl = cotacaoUsdBrl;
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/", this::tratar);
        servidor.setExecutor(Executors.newFixedThreadPool(4));
        servidor.start();
    }

    public void parar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    public String url() {
        return "http://localhost:" + porta + "/";
    }

    private void tratar(HttpExchange troca) {
        try (troca) {
            String caminho = troca.getRequestURI().getPath();
            if (caminho.equals("/api/buscar")) {
                apiBuscar(troca);
            } else if (caminho.equals("/api/destaques")) {
                apiDestaques(troca);
            } else if (caminho.equals("/api/exportar")) {
                apiExportar(troca);
            } else {
                estatico(troca, caminho);
            }
        } catch (Exception e) {
            Log.erro("erro ao tratar requisicao", e);
            tentarErro(troca, e);
        }
    }

    // ------------------------------------------------------------------ API

    private void apiBuscar(HttpExchange troca) throws IOException {
        String termo = parametro(troca, "termo");
        if (termo == null || termo.isBlank()) {
            responder(troca, 400, "application/json; charset=utf-8",
                    Json.escrever(Map.of("erro", "informe o parametro 'termo'")).getBytes(StandardCharsets.UTF_8));
            return;
        }
        List<Jogo> jogos = comparador.comparar(termo.trim());
        Map<String, Object> corpo = RespostaBusca.montar(termo.trim(), jogos, cotacaoUsdBrl.get());
        responder(troca, 200, "application/json; charset=utf-8",
                Json.escrever(corpo).getBytes(StandardCharsets.UTF_8));
    }

    private void apiDestaques(HttpExchange troca) throws IOException {
        List<Jogo> jogos;
        try {
            jogos = destaques.get();
        } catch (Exception e) {
            Log.erro("falha ao obter maiores descontos", e);
            jogos = List.of();
        }
        Map<String, Object> corpo = RespostaBusca.montarDestaques(jogos, cotacaoUsdBrl.get());
        responder(troca, 200, "application/json; charset=utf-8",
                Json.escrever(corpo).getBytes(StandardCharsets.UTF_8));
    }

    private void apiExportar(HttpExchange troca) throws IOException {
        String termo = parametro(troca, "termo");
        String formato = parametro(troca, "formato");
        if (termo == null || termo.isBlank()) {
            responder(troca, 400, "text/plain; charset=utf-8",
                    "informe o parametro 'termo'".getBytes(StandardCharsets.UTF_8));
            return;
        }
        formato = formato == null ? "csv" : formato.toLowerCase();
        List<Jogo> jogos = comparador.comparar(termo.trim());

        String conteudo;
        String tipo;
        String nomeArquivo = "comparacao-" + slug(termo) + "." + (formato.equals("html") ? "html" : "csv");
        if (formato.equals("html")) {
            conteudo = ExportadorComparacao.paraHtml(termo.trim(), jogos);
            tipo = "text/html; charset=utf-8";
        } else {
            conteudo = ExportadorComparacao.paraCsv(termo.trim(), jogos);
            tipo = "text/csv; charset=utf-8";
        }
        troca.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
        responder(troca, 200, tipo, conteudo.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------- estatico

    private void estatico(HttpExchange troca, String caminho) throws IOException {
        if (caminho.equals("/") || caminho.isEmpty()) {
            caminho = "/index.html";
        }
        if (caminho.contains("..")) {
            responder(troca, 403, "text/plain", "acesso negado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String recurso = "/web" + caminho;
        try (InputStream in = getClass().getResourceAsStream(recurso)) {
            if (in == null) {
                responder(troca, 404, "text/plain; charset=utf-8",
                        "nao encontrado".getBytes(StandardCharsets.UTF_8));
                return;
            }
            // sempre revalida: evita servir HTML/CSS/JS antigos do cache do navegador
            troca.getResponseHeaders().set("Cache-Control", "no-cache");
            responder(troca, 200, tipoConteudo(caminho), in.readAllBytes());
        }
    }

    private static String tipoConteudo(String caminho) {
        if (caminho.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (caminho.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (caminho.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (caminho.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    // -------------------------------------------------------------- helpers

    private void responder(HttpExchange troca, int status, String tipo, byte[] corpo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo);
        // API publica e somente leitura: libera para qualquer origem (front pode rodar em outro dominio).
        troca.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(corpo);
        }
    }

    private void tentarErro(HttpExchange troca, Exception e) {
        try {
            byte[] corpo = Json.escrever(Map.of("erro", String.valueOf(e.getMessage())))
                    .getBytes(StandardCharsets.UTF_8);
            responder(troca, 500, "application/json; charset=utf-8", corpo);
        } catch (IOException ignored) {
            // conexao ja perdida; nada a fazer
        }
    }

    private static String parametro(HttpExchange troca, String chave) {
        String query = troca.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        for (String par : query.split("&")) {
            int eq = par.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String k = URLDecoder.decode(par.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(par.substring(eq + 1), StandardCharsets.UTF_8);
            params.putIfAbsent(k, v);
        }
        return params.get(chave);
    }

    private static String slug(String termo) {
        String s = termo.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "jogos" : s;
    }
}
