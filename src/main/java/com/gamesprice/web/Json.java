package com.gamesprice.web;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Serializador JSON minimo, suficiente para as respostas da API web sem trazer uma
 * dependencia nova (o projeto mantem o Jsoup como unica dependencia de producao).
 *
 * <p>Serializa {@link Map} (objeto), {@link Iterable} (array), {@link CharSequence}
 * (string com escape), {@link Number}, {@link Boolean} e {@code null}.
 */
public final class Json {

    private Json() {
    }

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escreverValor(sb, valor);
        return sb.toString();
    }

    private static void escreverValor(StringBuilder sb, Object valor) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof Map<?, ?> mapa) {
            escreverObjeto(sb, mapa);
        } else if (valor instanceof Iterable<?> lista) {
            escreverArray(sb, lista);
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof BigDecimal bd) {
            sb.append(bd.toPlainString());
        } else {
            escreverString(sb, valor.toString());
        }
    }

    private static void escreverObjeto(StringBuilder sb, Map<?, ?> mapa) {
        sb.append('{');
        boolean primeiro = true;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            escreverString(sb, String.valueOf(entrada.getKey()));
            sb.append(':');
            escreverValor(sb, entrada.getValue());
        }
        sb.append('}');
    }

    private static void escreverArray(StringBuilder sb, Iterable<?> lista) {
        sb.append('[');
        boolean primeiro = true;
        for (Object item : lista) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            escreverValor(sb, item);
        }
        sb.append(']');
    }

    private static void escreverString(StringBuilder sb, String texto) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
