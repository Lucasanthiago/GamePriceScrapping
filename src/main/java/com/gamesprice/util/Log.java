package com.gamesprice.util;

/**
 * Log minimalista para stderr, para nao poluir a saida da CLI (stdout) e nao
 * trazer dependencia de framework de logging num projeto pequeno.
 */
public final class Log {

    private static boolean verboso = false;

    private Log() {
    }

    public static void setVerboso(boolean v) {
        verboso = v;
    }

    public static void info(String msg) {
        if (verboso) {
            System.err.println("[info] " + msg);
        }
    }

    public static void aviso(String msg) {
        System.err.println("[aviso] " + msg);
    }

    public static void erro(String msg, Throwable t) {
        System.err.println("[erro] " + msg + ": " + t.getMessage());
        if (verboso) {
            t.printStackTrace();
        }
    }
}
