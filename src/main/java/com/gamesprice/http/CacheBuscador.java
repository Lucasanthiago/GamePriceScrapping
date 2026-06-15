package com.gamesprice.http;

import com.gamesprice.config.Config;
import com.gamesprice.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Decorator de {@link Buscador} que guarda o HTML em disco com TTL.
 *
 * <p>Evita bater no servidor a cada teste durante o desenvolvimento (boa pratica de
 * scraping). Em cache hit dentro do TTL, le do disco; senao, delega ao buscador
 * interno e grava o resultado.
 */
public final class CacheBuscador implements Buscador {

    private final Buscador interno;
    private final Path diretorio;
    private final Duration ttl;

    public CacheBuscador(Buscador interno, Path diretorio, Duration ttl) {
        this.interno = interno;
        this.diretorio = diretorio;
        this.ttl = ttl;
    }

    /** Cache padrao em {@code <tmp>/comparador-precos-cache} com o TTL da Config. */
    public static CacheBuscador padrao(Buscador interno) {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "comparador-precos-cache");
        return new CacheBuscador(interno, dir, Config.CACHE_TTL);
    }

    @Override
    public Document obter(String url, Map<String, String> cookies) throws IOException {
        Path arquivo = diretorio.resolve(chave(url, cookies) + ".html");

        if (cacheValido(arquivo)) {
            Log.info("cache hit " + url);
            String html = Files.readString(arquivo, StandardCharsets.UTF_8);
            return Jsoup.parse(html, url);
        }

        Document doc = interno.obter(url, cookies);
        gravar(arquivo, doc.outerHtml());
        return doc;
    }

    private boolean cacheValido(Path arquivo) {
        try {
            if (!Files.exists(arquivo)) {
                return false;
            }
            Instant modificado = Files.getLastModifiedTime(arquivo).toInstant();
            return modificado.plus(ttl).isAfter(Instant.now());
        } catch (IOException e) {
            return false;
        }
    }

    private void gravar(Path arquivo, String html) {
        try {
            Files.createDirectories(diretorio);
            Files.writeString(arquivo, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.aviso("nao foi possivel gravar no cache: " + e.getMessage());
        }
    }

    private static String chave(String url, Map<String, String> cookies) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(url.getBytes(StandardCharsets.UTF_8));
            md.update(cookies.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString((url + cookies).hashCode());
        }
    }
}
