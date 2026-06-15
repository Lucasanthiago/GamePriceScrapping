package com.gamesprice.cli;

import com.gamesprice.model.Jogo;
import com.gamesprice.model.Oferta;
import com.gamesprice.ranking.ValeAPena;
import com.gamesprice.util.Precos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exporta o resultado da comparacao para formatos prontos para abrir/entregar: <b>CSV</b>
 * (dados, abre em planilha) e <b>HTML</b> (documento autossuficiente e estilizado).
 *
 * <p>Fica ao lado de {@link TabelaPrecos}: a comparacao nao muda, apenas ganha novas formas
 * de apresentacao. Ambos os formatos incluem o score "Vale a pena" ({@link ValeAPena}).
 */
public final class ExportadorComparacao {

    private ExportadorComparacao() {
    }

    // ------------------------------------------------------------------ CSV

    /** Uma linha por oferta; colunas em nivel de jogo (score, comparavel) se repetem. */
    public static String paraCsv(String termo, List<Jogo> jogos) {
        Map<Jogo, ValeAPena.Pontuacao> ranking = indexarRanking(jogos);

        StringBuilder sb = new StringBuilder();
        sb.append("termo,jogo,loja,preco_brl,preco_original_brl,desconto_perc,")
                .append("nota_reviews_perc,vale_a_pena,comparavel,mais_barata,url\n");

        for (Jogo jogo : jogos) {
            ValeAPena.Pontuacao p = ranking.get(jogo);
            Oferta maisBarata = jogo.ofertaMaisBarata();
            for (Oferta oferta : jogo.ofertasOrdenadas()) {
                sb.append(csv(termo)).append(',')
                        .append(csv(jogo.tituloExibicao())).append(',')
                        .append(csv(oferta.loja().rotulo())).append(',')
                        .append(num(oferta.precoBRL())).append(',')
                        .append(num(oferta.precoOriginalBRL())).append(',')
                        .append(oferta.descontoPerc()).append(',')
                        .append(oferta.notaReviewsPerc() == null ? "" : oferta.notaReviewsPerc()).append(',')
                        .append(p == null ? "" : p.valor()).append(',')
                        .append(jogo.comparavel()).append(',')
                        .append(oferta == maisBarata).append(',')
                        .append(csv(oferta.url())).append('\n');
            }
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------- HTML

    /** Documento HTML completo e estilizado, pronto para abrir no navegador. */
    public static String paraHtml(String termo, List<Jogo> jogos) {
        Map<Jogo, ValeAPena.Pontuacao> ranking = indexarRanking(jogos);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Comparacao de precos: ").append(html(termo)).append("</title>\n")
                .append("<style>").append(CSS).append("</style>\n")
                .append("</head>\n<body>\n");

        sb.append("<header><h1>Comparador de Precos de Jogos</h1>")
                .append("<p class=\"sub\">Resultados para <strong>").append(html(termo))
                .append("</strong> &middot; ").append(jogos.size()).append(" jogo(s)</p></header>\n");

        if (jogos.isEmpty()) {
            sb.append("<p class=\"vazio\">Nenhuma oferta encontrada nas lojas suportadas.</p>\n");
        } else {
            sb.append("<main>\n");
            for (Jogo jogo : jogos) {
                sb.append(cartaoJogo(jogo, ranking.get(jogo)));
            }
            sb.append("</main>\n");
        }

        sb.append("<footer>Gerado pelo Comparador de Precos de Jogos &middot; ")
                .append(">> marca a loja mais barata de cada jogo</footer>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String cartaoJogo(Jogo jogo, ValeAPena.Pontuacao p) {
        Oferta maisBarata = jogo.ofertaMaisBarata();
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"jogo\">\n<div class=\"jhead\">")
                .append("<h2>").append(html(jogo.tituloExibicao())).append("</h2>");
        if (jogo.comparavel()) {
            sb.append("<span class=\"tag comp\">comparavel em ")
                    .append(jogo.quantidadeLojas()).append(" lojas</span>");
        }
        if (p != null) {
            sb.append("<span class=\"tag score\">vale a pena: ").append(p.valor()).append("</span>");
        }
        sb.append("</div>\n");

        sb.append("<table>\n<thead><tr><th></th><th>Loja</th><th>Preco</th>")
                .append("<th>Desconto</th><th>Nota</th><th>Link</th></tr></thead>\n<tbody>\n");
        for (Oferta oferta : jogo.ofertasOrdenadas()) {
            boolean melhor = oferta == maisBarata;
            sb.append("<tr class=\"").append(melhor ? "melhor" : "").append("\">")
                    .append("<td class=\"mark\">").append(melhor ? "&raquo;" : "").append("</td>")
                    .append("<td>").append(html(oferta.loja().rotulo())).append("</td>")
                    .append("<td class=\"preco\">").append(html(Precos.formatarBRL(oferta.precoBRL())));
            if (oferta.temDesconto()) {
                sb.append(" <s>").append(html(Precos.formatarBRL(oferta.precoOriginalBRL()))).append("</s>");
            }
            sb.append("</td>")
                    .append("<td>").append(oferta.temDesconto() ? "-" + oferta.descontoPerc() + "%" : "&mdash;").append("</td>")
                    .append("<td>").append(oferta.notaReviewsPerc() == null ? "&mdash;" : oferta.notaReviewsPerc() + "%").append("</td>")
                    .append("<td><a href=\"").append(html(hrefSeguro(oferta.url()))).append("\" target=\"_blank\" rel=\"noopener\">abrir</a></td>")
                    .append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n</section>\n");
        return sb.toString();
    }

    private static final String CSS =
            "*{box-sizing:border-box}"
            + "body{margin:0;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;"
            + "background:#0f1320;color:#e8ecf6;padding:0 0 48px}"
            + "header{padding:28px 24px 8px}h1{margin:0;font-size:22px}"
            + ".sub{color:#9aa4bf;margin:6px 0 0}main{padding:8px 24px;display:grid;gap:18px;"
            + "grid-template-columns:repeat(auto-fill,minmax(360px,1fr))}"
            + ".jogo{background:#171c2e;border:1px solid #232a42;border-radius:14px;padding:16px;"
            + "box-shadow:0 8px 24px rgba(0,0,0,.25)}"
            + ".jhead{display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin-bottom:10px}"
            + "h2{font-size:16px;margin:0;flex:1 1 auto}"
            + ".tag{font-size:11px;padding:3px 8px;border-radius:999px;white-space:nowrap}"
            + ".tag.comp{background:#1d3a2e;color:#76e0a8}.tag.score{background:#2a2350;color:#bfb1ff}"
            + "table{width:100%;border-collapse:collapse;font-size:13px}"
            + "th{text-align:left;color:#8a93b0;font-weight:600;padding:6px 8px;border-bottom:1px solid #232a42}"
            + "td{padding:7px 8px;border-bottom:1px solid #1c2236}"
            + "tr.melhor{background:#14241c}tr.melhor .preco{color:#76e0a8;font-weight:700}"
            + ".mark{color:#76e0a8;font-weight:700;width:18px}.preco s{color:#6b7390;font-size:11px;margin-left:4px}"
            + "a{color:#7aa2ff;text-decoration:none}a:hover{text-decoration:underline}"
            + ".vazio{padding:24px}footer{color:#6b7390;font-size:12px;padding:24px}";

    // -------------------------------------------------------------- helpers

    private static Map<Jogo, ValeAPena.Pontuacao> indexarRanking(List<Jogo> jogos) {
        return ValeAPena.ranquear(jogos).stream()
                .collect(Collectors.toMap(ValeAPena.Pontuacao::jogo, p -> p, (a, b) -> a));
    }

    private static String num(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    /**
     * Escapa um campo CSV (RFC 4180): aspas duplicadas e campo entre aspas se preciso.
     * Tambem neutraliza injecao de formula em planilhas: campos vindos de dados raspados
     * que comecem com = + - @ (ou tab/CR) recebem um apostrofo, para o Excel/Sheets tratar
     * como texto e nao executar uma "formula" maliciosa ao abrir o arquivo.
     */
    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        boolean precisaAspas = v.contains(",") || v.contains("\"")
                || v.contains("\n") || v.contains("\r");
        String escapado = v.replace("\"", "\"\"");
        return precisaAspas ? "\"" + escapado + "\"" : escapado;
    }

    /** So deixa passar URLs http/https; o resto (javascript:, data:...) vira "#". */
    private static String hrefSeguro(String url) {
        if (url == null) {
            return "#";
        }
        try {
            String esquema = java.net.URI.create(url.trim()).getScheme();
            if (esquema != null) {
                esquema = esquema.toLowerCase();
                if (esquema.equals("http") || esquema.equals("https")) {
                    return url.trim();
                }
            }
        } catch (RuntimeException ignored) {
            // URL malformada -> trata como insegura
        }
        return "#";
    }

    /** Escapa texto para inserir com seguranca em HTML. */
    private static String html(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
